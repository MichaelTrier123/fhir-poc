package com.netcompany.fhir.demo.source;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.netcompany.fhir.demo.source.JournalSource.Episode;
import com.netcompany.fhir.demo.source.JournalSource.Warning;

@Component
public class SdkClient {
    private static final Map<String, String> TYPES = Map.of(
        "Kontaktperioder", "kontaktperioder", "Diagnoser", "diagnoser", "Procedurer", "procedurer",
        "Notater", "notater", "Epikriser", "epikriser");
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient http;

    public SdkClient(ObjectMapper mapper, @Value("${sdk.base-url}") String baseUrl,
                     @Value("${sdk.timeout-seconds}") int timeoutSeconds) {
        URI uri = URI.create(baseUrl);
        if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null
                || timeoutSeconds < 1) {
            throw new IllegalArgumentException("Invalid SDK configuration");
        }
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public static String normalizeCpr(String cpr) {
        if (cpr == null || !cpr.matches("(?:[0-9]{10}|[0-9]{6}-[0-9]{4})")) {
            throw new SourceException(400, "Angiv CPR som 10 cifre eller 6 cifre, bindestreg og 4 cifre.");
        }
        return cpr.replace("-", "");
    }

    public JournalSource fetch(String requestedCpr) {
        String cpr = normalizeCpr(requestedCpr);
        List<Warning> warnings = new ArrayList<>();
        JsonNode overview = get(cpr, "forloebsoversigt", null, false);
        if (!cpr.equals(normalizeSourceCpr(overview.path("PersonNummer").asString()))) {
            throw new SourceException(502, "Kildens patientidentitet stemmer ikke med forespørgslen.");
        }
        requireArray(overview, "Forloeb");
        JsonNode cave = get(cpr, "caveoplysninger", null, true);
        checkCount(cave, "CaveOplysninger", overview.path("AntalCave").asInt(-1), null, warnings);
        checkCount(overview, "Forloeb", overview.path("NumberOfForloeb").asInt(-1), null, warnings);
        if (overview.path("HarSpaerretForloeb").asBoolean() || overview.path("UkendtExist").asBoolean()) {
            warnings.add(new Warning("source-restriction", "Kilden angiver spærrede eller ukendte forløb. Udtrækket kan være ufuldstændigt.", null));
        }
        List<Episode> episodes = new ArrayList<>();
        int omitted = 0;
        var seen = new java.util.HashSet<String>();
        for (JsonNode episode : overview.path("Forloeb")) {
            if (restricted(episode)) { omitted++; continue; }
            String key = episode.path("IdNoegle").path("Noegle").asString("");
            try {
                if (!UUID.fromString(key).toString().equalsIgnoreCase(key)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw new SourceException(502, "Kilden indeholder en ugyldig forløbsnøgle.");
            }
            if (!seen.add(key)) throw new SourceException(502, "Kilden indeholder gentagne forløbsnøgler.");
            Map<String, JsonNode> details = new LinkedHashMap<>();
            for (String type : List.of("Kontaktperioder", "Diagnoser", "Procedurer", "Notater", "Epikriser")) {
                int count = episode.path("Antal" + type).asInt(-1);
                if (count == 0) continue;
                JsonNode detail = get(cpr, TYPES.get(type), key, true);
                checkCount(detail, type, count, key, warnings);
                if (detail != null) details.put(type, detail);
            }
            episodes.add(new Episode(key, episode, details));
        }
        if (omitted > 0) warnings.add(new Warning("restricted-episodes", omitted + " skjulte eller privatmarkerede forløb er udeladt.", null));
        return new JournalSource(cpr, overview, cave, episodes, warnings, omitted);
    }

    private static String normalizeSourceCpr(String value) {
        return value == null ? "" : value.replace("-", "");
    }

    static boolean restricted(JsonNode episode) {
        String privacy = episode.path("Privatmarkering").asString("");
        return episode.path("Skjult").asBoolean() || (!privacy.isBlank() && !privacy.equals("None"));
    }

    private static void requireArray(JsonNode node, String name) {
        if (!node.path(name).isArray()) throw new SourceException(502, "Kilden har en uventet datastruktur.");
    }

    private static void checkCount(JsonNode node, String type, int expected, String key, List<Warning> warnings) {
        if (node == null) {
            warnings.add(new Warning("missing-source", type + ": kildefilen findes ikke; data er ikke hentet.", key));
            return;
        }
        requireArray(node, type);
        if (expected >= 0 && node.path(type).size() != expected) {
            warnings.add(new Warning("count-mismatch", type + ": forventet " + expected + ", hentet " + node.path(type).size() + ".", key));
        }
    }

    private JsonNode get(String cpr, String type, String key, boolean optional) {
        String path = baseUrl + "/api/ejournal/" + cpr + "/" + type + (key == null ? "" : "?key=" + key);
        HttpRequest request = HttpRequest.newBuilder(URI.create(path)).timeout(timeout).header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                if (optional) return null;
                throw new SourceException(404, "Patientens forløbsoversigt findes ikke i SDK-demoen.");
            }
            if (response.statusCode() != 200) throw new SourceException(502, "SDK-demoen kunne ikke levere de ønskede data.");
            JsonNode body;
            try { body = mapper.readTree(response.body()); }
            catch (RuntimeException ex) { throw new SourceException(502, "SDK-demoen returnerede ugyldig JSON."); }
            if (body == null || !body.isObject()) throw new SourceException(502, "SDK-demoen returnerede en uventet datastruktur.");
            if (!body.path("ErrorMessage").isNull() && !body.path("ErrorMessage").isMissingNode()
                    && !body.path("ErrorMessage").asString("").isBlank()) {
                throw new SourceException(502, "SDK-demoen angiver en fejl i udtrækket.");
            }
            return body;
        } catch (HttpTimeoutException ex) {
            throw new SourceException(504, "SDK-demoen svarede ikke inden for tidsgrænsen.");
        } catch (IOException ex) {
            throw new SourceException(502, "SDK-demoen kan ikke kontaktes. Kontrollér, at den kører.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SourceException(503, "Datahentningen blev afbrudt.");
        }
    }
}
