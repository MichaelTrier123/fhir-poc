package com.netcompany.fhir.demo;

import static org.junit.jupiter.api.Assertions.*;

import com.netcompany.fhir.demo.mapping.FhirMapper;
import com.netcompany.fhir.demo.source.JournalSource;
import com.netcompany.fhir.demo.source.SdkClient;
import com.netcompany.fhir.demo.source.SourceException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JournalIntegrationTest {
    private static final String KEY = "605bf396-4276-4497-ba8e-0f397d1b3eb3";
    private static final String RESTRICTED = "00000000-0000-0000-0000-000000000001";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<String> requests = ConcurrentHashMap.newKeySet();
    private static final HttpServer SOURCE = sourceServer();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("sdk.base-url", () -> "http://127.0.0.1:" + SOURCE.getAddress().getPort());
    }

    @AfterAll static void close() { SOURCE.stop(0); }

    private static HttpServer sourceServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/ejournal/", exchange -> {
                String path = exchange.getRequestURI().toString();
                requests.add(path);
                String cpr = exchange.getRequestURI().getPath().split("/")[3];
                String type = exchange.getRequestURI().getPath().split("/")[4];
                String body = "{}";
                int status = 200;
                if (cpr.equals("0000000000")) status = 404;
                else if (cpr.equals("1111111111")) body = "not json";
                else if (cpr.equals("2222222222")) body = "{\"PersonNummer\":\"9999999999\",\"Forloeb\":[]}";
                else if (cpr.equals("3333333333")) { status = 500; body = "sensitive upstream details"; }
                else if (cpr.equals("4444444444")) body = "{\"ErrorMessage\":\"sensitive upstream details\"}";
                else switch (type) {
                    case "forloebsoversigt" -> body = """
                        {"PersonNummer":"050688-9996","Navn":"Syntetisk Testpatient","NumberOfForloeb":2,"AntalCave":1,
                         "Forloeb":[{"IdNoegle":{"Noegle":"%s"},"DatoFra":"2024-01-01T12:00:00Z",
                           "DatoTil":null,"DatoOpdateret":"2024-01-02T12:00:00Z","AfdelingKode":"D1","AfdelingNavn":"Testafdeling",
                           "DiagnoseKode":"DI109","DiagnoseNavn":"Syntetisk oversigtsdiagnose",
                           "SygehusKode":"H1","SygehusNavn":"Testhospital","Privatmarkering":"None","Skjult":false,
                           "AntalKontaktperioder":2,"AntalDiagnoser":1,"AntalProcedurer":1,"AntalNotater":1,"AntalEpikriser":1},
                           {"IdNoegle":{"Noegle":"%s"},"Privatmarkering":"OrganisationspaerringSundhedsfaglig","AntalDiagnoser":1}]}
                        """.formatted(KEY, RESTRICTED);
                    case "kontaktperioder" -> body = """
                        {"Kontaktperioder":[{"Noegle":"c1","Status":"Ambulant","Prioritet":"Planlagt","DatoFra":"2024-01-01T12:00:00Z","DatoTil":null},
                          {"Noegle":"c2","Status":null,"DatoFra":"2024-01-01T12:00:00Z"}]}
                        """;
                    case "diagnoser" -> body = """
                        {"Diagnoser":[{"Noegle":"d1","DiagnoseKode":"DA123","DiagnoseBeskrivelse":"Syntetisk diagnose",
                        "DiagnoseArt":"Aktionsdiagnose","DatoFra":"2024-01-01T12:00:00Z","Tillaegskoder":[{"Kode":"ALCC01","Beskrivelse":"sygdom"}]}]}
                        """;
                    case "procedurer" -> body = """
                        {"Procedurer":[{"Noegle":"p1","ProcedureKode":"B123","ProcedureBeskrivelse":"Syntetisk procedure","DatoFra":"2024-01-01T12:00:00Z"}]}
                        """;
                    case "notater" -> body = """
                        {"Notater":[{"Noegle":"n1","Overskrift":"Notat","Broedtekst":"<p>Æble &amp; pære</p><script>alert(1)</script>","DatoFra":"2024-01-01T12:00:00Z"}]}
                        """;
                    case "caveoplysninger" -> body = """
                        {"CaveOplysninger":[{"Beskrivelse":"Status: Afsluttet<br/>Aktiv i metadata","Aktiv":true,"DatoFra":"2023-01-01T12:00:00Z"}]}
                        """;
                    case "epikriser" -> status = 404;
                    default -> status = 404;
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode journal() throws Exception {
        var response = get("/api/fhir/050688-9996");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""));
        return JSON.readTree(response.body());
    }

    private static JsonNode resource(JsonNode result, String type) {
        for (JsonNode entry : result.path("bundle").path("entry")) {
            if (entry.path("resource").path("resourceType").asString().equals(type)) return entry.path("resource");
        }
        fail("Missing resource " + type);
        return null;
    }

    @Test void mapsConservativelyWithResolvableDeterministicReferences() throws Exception {
        JsonNode result = journal();
        assertEquals("collection", result.path("bundle").path("type").asString());
        assertEquals(result.path("bundle"), journal().path("bundle"));
        JsonNode patient = resource(result, "Patient");
        assertEquals("0506889996", patient.path("identifier").get(0).path("value").asString());
        assertFalse(patient.has("birthDate"));
        assertFalse(patient.has("gender"));
        JsonNode episode = resource(result, "EpisodeOfCare");
        assertFalse(episode.has("status"));
        assertEquals("unknown", episode.path("_status").path("extension").get(0).path("valueCode").asString());
        assertFalse(episode.has("meta"));
        JsonNode encounter = resource(result, "Encounter");
        assertEquals("unknown", encounter.path("status").asString());
        assertEquals("AMB", encounter.path("class").path("code").asString());
        assertEquals("Planlagt", encounter.path("priority").path("text").asString());
        JsonNode diagnosis = resource(result, "Condition");
        assertEquals("DA123", diagnosis.path("code").path("coding").get(0).path("code").asString());
        assertFalse(diagnosis.has("clinicalStatus"));
        assertFalse(diagnosis.has("onsetDateTime"));
        assertFalse(diagnosis.has("encounter"));
        JsonNode procedure = resource(result, "Procedure");
        assertEquals("unknown", procedure.path("status").asString());
        assertFalse(procedure.has("performedDateTime"));
        assertFalse(procedure.has("performedPeriod"));
        Set<String> urls = new HashSet<>();
        for (JsonNode entry : result.path("bundle").path("entry")) assertTrue(urls.add(entry.path("fullUrl").asString()));
        assertReferences(result.path("bundle"), urls);
    }

    private static void assertReferences(JsonNode node, Set<String> urls) {
        if (node.isObject() && node.has("reference")) assertTrue(urls.contains(node.path("reference").asString()), "Dangling reference");
        if (node.isContainer()) for (JsonNode child : node) assertReferences(child, urls);
    }

    @Test void preservesEpisodeDiagnosisAsOverviewMetadataWithoutInventingActionDiagnosis() throws Exception {
        JsonNode result = journal();
        JsonNode episode = resource(result, "EpisodeOfCare");
        JsonNode overviewDiagnosis = null;
        for (JsonNode extension : episode.path("extension")) {
            if (extension.path("url").asString().equals(FhirMapper.LOCAL + "StructureDefinition/source-overview-diagnosis")) {
                overviewDiagnosis = extension.path("valueCodeableConcept");
            }
        }
        assertNotNull(overviewDiagnosis);
        assertEquals("DI109", overviewDiagnosis.path("coding").get(0).path("code").asString());
        assertEquals("Syntetisk oversigtsdiagnose", overviewDiagnosis.path("text").asString());
        assertFalse(episode.has("diagnosis"));
        int conditions = 0;
        for (JsonNode entry : result.path("bundle").path("entry")) {
            if (entry.path("resource").path("resourceType").asString().equals("Condition")) conditions++;
        }
        assertEquals(1, conditions, "Oversigtsdiagnosen skal ikke oprette en ekstra Condition");
        assertEquals("DA123", resource(result, "Condition").path("code").path("coding").get(0).path("code").asString());
    }

    @Test void omitsUndefinedOrEmptyEpisodeOverviewDiagnosis() throws Exception {
        for (String fields : List.of("\"DiagnoseKode\":\"diag_udef\",\"DiagnoseNavn\":\"Ukendt diagnose\"",
                "\"DiagnoseKode\":\"\",\"DiagnoseNavn\":\"\"")) {
            var source = new JournalSource("0506889996", JSON.readTree("{}"), null,
                List.of(new JournalSource.Episode(KEY, JSON.readTree("{" + fields + "}"), Map.of())), List.of(), 0);
            JsonNode result = JSON.readTree(JSON.writeValueAsString(new FhirMapper().transform(source)));
            for (JsonNode extension : resource(result, "EpisodeOfCare").path("extension")) {
                assertNotEquals(FhirMapper.LOCAL + "StructureDefinition/source-overview-diagnosis", extension.path("url").asString());
            }
        }
    }

    @Test void reportsPartialDataAndNeverFetchesRestrictedEpisodes() throws Exception {
        JsonNode result = journal();
        assertEquals(1, result.path("omittedEpisodes").asInt());
        assertTrue(result.path("warnings").toString().contains("missing-source"));
        assertTrue(result.path("warnings").toString().contains("unknown-class"));
        assertTrue(result.path("warnings").toString().contains("restricted-episodes"));
        assertFalse(requests.stream().anyMatch(r -> r.contains(RESTRICTED)));
        assertTrue(requests.stream().anyMatch(r -> r.contains("/notater?key=")));
        assertTrue(requests.stream().anyMatch(r -> r.contains("/epikriser?key=")));
        assertTrue(resource(result, "OperationOutcome").path("issue").size() > 0);
    }

    @Test void preservesUnicodeDocumentsWithoutHtmlOrInventedAllergies() throws Exception {
        JsonNode result = journal();
        boolean foundNote = false, foundCave = false;
        for (JsonNode entry : result.path("bundle").path("entry")) {
            JsonNode r = entry.path("resource");
            assertNotEquals("AllergyIntolerance", r.path("resourceType").asString());
            if (!r.path("resourceType").asString().equals("DocumentReference")) continue;
            String text = new String(Base64.getDecoder().decode(r.path("content").get(0).path("attachment").path("data").asString()), StandardCharsets.UTF_8);
            assertFalse(text.contains("<"));
            assertFalse(text.contains("alert"));
            if (r.path("type").path("text").asString().equals("Notater")) { assertEquals("Æble & pære", text); foundNote = true; }
            if (r.path("type").path("text").asString().equals("CaveOplysninger")) { assertTrue(text.contains("Afsluttet")); foundCave = true; }
        }
        assertTrue(foundNote && foundCave);
    }

    @Test void servesFhirMediaTypeAndSafeHttpErrors() throws Exception {
        var response = get("/api/fhir/0506889996/bundle");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/fhir+json"));
        assertEquals("Bundle", JSON.readTree(response.body()).path("resourceType").asString());
        assertEquals(400, get("/api/fhir/abc").statusCode());
        assertEquals(404, get("/api/fhir/0000000000").statusCode());
        for (String cpr : new String[]{"1111111111", "2222222222", "3333333333", "4444444444"}) {
            var failed = get("/api/fhir/" + cpr);
            assertEquals(502, failed.statusCode());
            assertFalse(failed.body().contains("sensitive"));
            assertFalse(failed.body().contains("stackTrace"));
            assertEquals("no-store", failed.headers().firstValue("Cache-Control").orElse(""));
        }
    }

    @Test void rejectsMalformedCprAndStripsUnsafeHtml() {
        assertEquals("0506889996", SdkClient.normalizeCpr("050688-9996"));
        assertThrows(SourceException.class, () -> SdkClient.normalizeCpr("05-06-889996"));
        assertThrows(SourceException.class, () -> SdkClient.normalizeCpr("../data"));
        assertEquals("Linje 1\nLinje 2 & æ", FhirMapper.plainText("<div>Linje 1<br/>Linje 2 &amp; æ</div><style>p{color:red}</style>"));
    }

    @Test void servesStaticResourcesWithoutCaching() throws Exception {
        for (String path : List.of("/", "/index.html", "/app.js", "/app.js?v=cache-header-check",
                "/diagnosis-overview.js", "/styles.css", "/diagnosis.css", "/journal.css")) {
            var response = get(path);
            assertEquals(200, response.statusCode(), path);
            assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""), path);
        }
    }
}
