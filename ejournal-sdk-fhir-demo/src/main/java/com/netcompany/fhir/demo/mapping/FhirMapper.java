package com.netcompany.fhir.demo.mapping;

import com.netcompany.fhir.demo.source.JournalSource;
import com.netcompany.fhir.demo.source.JournalSource.Warning;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;

@Component
public class FhirMapper {
    public static final String LOCAL = "https://example.org/ejournal-sdk-fhir-demo/";
    private static final String DAR = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";

    public record Result(Map<String, Object> bundle, List<Warning> warnings, int omittedEpisodes, String fhirVersion) {}

    public Result transform(JournalSource source) {
        return new Mapping(source).run();
    }

    private static final class Mapping {
        private final JournalSource source;
        private final Map<String, Map<String, Object>> resources = new LinkedHashMap<>();
        private final List<Warning> warnings;
        private final String patientId;

        Mapping(JournalSource source) {
            this.source = source;
            warnings = new ArrayList<>(source.warnings());
            patientId = id("Patient", source.cpr());
        }

        Result run() {
            var patient = resource("Patient", patientId);
            patient.put("identifier", List.of(m("system", "urn:oid:1.2.208.176.1.2", "value", source.cpr())));
            String name = text(source.overview(), "Navn");
            if (!name.isBlank()) patient.put("name", List.of(m("text", name)));
            trace(patient, "forloebsoversigt", "patient", null);
            add(patient);
            if (!source.episodes().isEmpty()) warnings.add(new Warning("unknown-status",
                "Kilden angiver ikke livscyklusstatus: forløbsstatus er data-absent-reason unknown; kontakt- og procedurestatus er unknown.", null));

            for (var episode : source.episodes()) {
                JsonNode row = episode.overview();
                var eoc = resource("EpisodeOfCare", id("EpisodeOfCare", episode.key()));
                eoc.put("identifier", List.of(identifier("forloeb", episode.key())));
                eoc.put("_status", m("extension", List.of(m("url", DAR, "valueCode", "unknown"))));
                eoc.put("patient", ref(patientId));
                putPeriod(eoc, "period", row, episode.key());
                String org = organization(text(row, "SygehusKode"), text(row, "SygehusNavn"),
                    text(row, "AfdelingKode"), text(row, "AfdelingNavn"));
                if (org != null) eoc.put("managingOrganization", ref(org));
                trace(eoc, "forloebsoversigt", episode.key(), episode.key());
                String overviewCode = text(row, "DiagnoseKode"), overviewText = text(row, "DiagnoseNavn");
                if (!overviewCode.equals("diag_udef") && (!overviewCode.isBlank() || !overviewText.isBlank())) {
                    // Oversigtsmetadata angiver ikke diagnoseart eller diagnosens registreringstid.
                    extension(eoc, "source-overview-diagnosis", "valueCodeableConcept", concept(overviewCode, overviewText));
                }
                String updated = date(row, "DatoOpdateret", episode.key());
                if (!updated.isBlank()) extension(eoc, "source-updated", "valueDateTime", updated);
                add(eoc);
                for (var detail : episode.details().entrySet()) {
                    for (JsonNode item : detail.getValue().path(detail.getKey())) {
                        switch (detail.getKey()) {
                            case "Kontaktperioder" -> encounter(item, episode.key(), eoc);
                            case "Diagnoser" -> condition(item, episode.key());
                            case "Procedurer" -> procedure(item, episode.key());
                            case "Notater", "Epikriser" -> document(item, detail.getKey(), episode.key());
                            default -> throw new IllegalArgumentException("Unexpected source type");
                        }
                    }
                }
            }
            if (source.cave() != null) {
                for (JsonNode cave : source.cave().path("CaveOplysninger")) document(cave, "CaveOplysninger", null);
                if (!source.cave().path("CaveOplysninger").isEmpty()) warnings.add(new Warning("unstructured-cave",
                    "CAVE er bevaret som dokumenttekst. Allergen, reaktion og klinisk status er ikke udledt af friteksten.", null));
            }
            if (!warnings.isEmpty()) {
                var outcome = resource("OperationOutcome", id("OperationOutcome", "mapping-warnings"));
                outcome.put("issue", warnings.stream().map(w -> {
                    var issue = m("severity", "warning", "code", "processing", "details", m("text", w.message()));
                    if (w.episodeKey() != null) extension(issue, "source-forloeb-key", "valueString", w.episodeKey());
                    return issue;
                }).toList());
                add(outcome);
            }
            var entries = resources.values().stream().map(r -> m("fullUrl", "urn:uuid:" + r.get("id"), "resource", r)).toList();
            var bundle = m("resourceType", "Bundle", "id", id("Bundle", "journal"), "type", "collection");
            if (!entries.isEmpty()) bundle.put("entry", entries);
            return new Result(bundle, List.copyOf(warnings), source.omittedEpisodes(), "4.0.1");
        }

        private void encounter(JsonNode row, String key, Map<String, Object> episode) {
            var resource = item("Encounter", row, "Kontaktperioder", key);
            resource.put("status", "unknown");
            String code = switch (text(row, "Status")) { case "Ambulant" -> "AMB"; case "Indlagt" -> "IMP"; default -> "UNK"; };
            resource.put("class", m("system", code.equals("UNK") ? "http://terminology.hl7.org/CodeSystem/v3-NullFlavor"
                : "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code", code));
            if (code.equals("UNK")) warnings.add(new Warning("unknown-class", "En kontakt har ukendt klasse; kildens Status er bevaret som sporbarhed.", key));
            if (!text(row, "Status").isBlank()) extension(resource, "source-contact-class", "valueString", text(row, "Status"));
            if (!text(row, "Prioritet").isBlank()) resource.put("priority", m("text", text(row, "Prioritet")));
            resource.put("subject", ref(patientId));
            resource.put("episodeOfCare", List.of(ref((String) episode.get("id"))));
            putPeriod(resource, "period", row, key);
            String org = organization(row.path("EnhedsInformation"));
            if (org != null) resource.put("serviceProvider", ref(org));
            sourceText(resource, row, "Fritekst");
            add(resource);
        }

        private void condition(JsonNode row, String key) {
            String code = text(row, "DiagnoseKode");
            String description = text(row, "DiagnoseBeskrivelse");
            if (code.equals("diag_udef") || (code.isBlank() && description.isBlank())) {
                warnings.add(new Warning("undefined-diagnosis", "En diagnose uden brugbar kode/tekst er udeladt.", key));
                return;
            }
            var resource = item("Condition", row, "Diagnoser", key);
            resource.put("subject", ref(patientId));
            resource.put("code", concept(code, description));
            if (!text(row, "DiagnoseArt").isBlank()) resource.put("category", List.of(m("text", text(row, "DiagnoseArt"))));
            // Kilden beskriver ikke sikkert onset, abatement eller clinicalStatus.
            sourcePeriod(resource, row, key);
            sourceText(resource, row, "Fritekst");
            supplements(resource, row);
            add(resource);
        }

        private void procedure(JsonNode row, String key) {
            var resource = item("Procedure", row, "Procedurer", key);
            resource.put("subject", ref(patientId));
            resource.put("status", "unknown");
            var code = concept(text(row, "ProcedureKode"), text(row, "ProcedureBeskrivelse"));
            if (!code.isEmpty()) resource.put("code", code);
            if (!text(row, "ProcedureArt").isBlank()) resource.put("category", m("text", text(row, "ProcedureArt")));
            // Bevar kildedatoen uden at påstå faktisk udførelse af proceduren.
            sourcePeriod(resource, row, key);
            sourceText(resource, row, "Fritekst");
            supplements(resource, row);
            add(resource);
        }

        private void document(JsonNode row, String kind, String key) {
            var resource = item("DocumentReference", row, kind, key);
            String title = text(row, "Overskrift");
            if (title.isBlank()) title = kind.equals("CaveOplysninger") ? "CAVE-oplysning" : kind;
            String raw = kind.equals("CaveOplysninger") ? text(row, "Beskrivelse") : text(row, "Broedtekst");
            String body = plainText(raw);
            String extra = plainText(text(row, "Fritekst"));
            if (!extra.isBlank()) body += "\n" + extra;
            if (body.isBlank()) {
                body = "Ingen dokumenttekst i kilden.";
                warnings.add(new Warning("empty-document", kind + ": et dokument har ingen tekst.", key));
            }
            // Status beskriver det genererede dokument, ikke en allergis kliniske status.
            resource.put("status", "current");
            resource.put("subject", ref(patientId));
            resource.put("type", m("text", kind));
            resource.put("description", title);
            var attachment = m("contentType", "text/plain; charset=utf-8", "language", "da", "title", title,
                "data", Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)));
            resource.put("content", List.of(m("attachment", attachment)));
            sourcePeriod(resource, row, key);
            if (kind.equals("CaveOplysninger") && row.path("Aktiv").isBoolean()) extension(resource, "source-active", "valueBoolean", row.path("Aktiv").asBoolean());
            add(resource);
        }

        private Map<String, Object> item(String type, JsonNode row, String kind, String episode) {
            String key = text(row, "Noegle");
            if (key.isBlank()) key = UUID.nameUUIDFromBytes(row.toString().getBytes(StandardCharsets.UTF_8)).toString();
            var resource = resource(type, id(type, kind + ":" + episode + ":" + key));
            resource.put("identifier", List.of(identifier(kind, key)));
            trace(resource, kind, key, episode);
            String org = organization(row.path("EnhedsInformation"));
            if (org != null) extension(resource, "source-organization", "valueReference", ref(org));
            return resource;
        }

        private void supplements(Map<String, Object> resource, JsonNode row) {
            if (row.path("Tillaegskoder").isArray()) {
                for (JsonNode code : row.path("Tillaegskoder")) {
                    var concept = concept(text(code, "Kode"), text(code, "Beskrivelse"));
                    if (!concept.isEmpty()) extension(resource, "source-additional-code", "valueCodeableConcept", concept);
                }
            }
        }

        private void sourceText(Map<String, Object> resource, JsonNode row, String field) {
            String text = plainText(text(row, field));
            if (!text.isBlank()) extension(resource, "source-text", "valueString", text);
        }

        private void sourcePeriod(Map<String, Object> resource, JsonNode row, String key) {
            var period = period(row, key);
            if (!period.isEmpty()) extension(resource, "source-period", "valuePeriod", period);
        }

        private void putPeriod(Map<String, Object> resource, String field, JsonNode row, String key) {
            var period = period(row, key);
            if (!period.isEmpty()) resource.put(field, period);
        }

        private Map<String, Object> period(JsonNode row, String key) {
            String start = date(row, "DatoFra", key), end = date(row, "DatoTil", key);
            var result = m();
            if (!start.isBlank()) result.put("start", start);
            if (!end.isBlank()) result.put("end", end);
            if (!start.isBlank() && !end.isBlank() && Instant.parse(end).isBefore(Instant.parse(start))) {
                warnings.add(new Warning("invalid-period", "Kildens slutdato ligger før startdato. Perioden er udeladt.", key));
                return m();
            }
            return result;
        }

        private String date(JsonNode row, String field, String key) {
            String value = text(row, field);
            if (value.isBlank()) return "";
            try { Instant.parse(value); return value; }
            catch (DateTimeParseException ex) {
                warnings.add(new Warning("invalid-date", "Kilden indeholder en ugyldig " + field + "; feltet er udeladt.", key));
                return "";
            }
        }

        private String organization(JsonNode unit) {
            return organization(text(unit, "SygehusKode"), text(unit, "Institution"), text(unit, "AfdelingsKode"), text(unit, "Afdeling"));
        }

        private String organization(String hospitalCode, String hospitalName, String departmentCode, String departmentName) {
            String hospital = org("sygehus", hospitalCode, hospitalName, null);
            String department = org("afdeling", departmentCode, departmentName, hospital);
            return department == null ? hospital : department;
        }

        private String org(String kind, String code, String name, String parent) {
            if (code.isBlank() && name.isBlank()) return null;
            String identifierKey = code.isBlank() ? "name:" + name + ":" + parent : code;
            String id = id("Organization", kind + ":" + identifierKey);
            if (!resources.containsKey(id)) {
                var org = resource("Organization", id);
                if (!code.isBlank()) org.put("identifier", List.of(identifier(kind + "-kode", code)));
                if (!name.isBlank()) org.put("name", name);
                if (parent != null) org.put("partOf", ref(parent));
                add(org);
            }
            return id;
        }

        private void trace(Map<String, Object> resource, String kind, String key, String episode) {
            extension(resource, "source-type", "valueString", kind);
            extension(resource, "source-key", "valueString", key);
            if (episode != null) extension(resource, "source-forloeb-key", "valueString", episode);
        }

        private String id(String type, String key) {
            return UUID.nameUUIDFromBytes((type + ":" + source.cpr() + ":" + key).getBytes(StandardCharsets.UTF_8)).toString();
        }

        private void add(Map<String, Object> resource) {
            String id = (String) resource.get("id");
            var previous = resources.putIfAbsent(id, resource);
            if (previous != null && !previous.equals(resource)) {
                throw new com.netcompany.fhir.demo.source.SourceException(502, "Kilden indeholder modstridende ressourcer med samme nøgle.");
            }
        }
    }

    public static String plainText(String html) {
        String safe = html.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>", "");
        safe = safe.replaceAll("(?i)<br\\s*/?>|</(?:p|div|pre|li|tr|h[1-6])\\s*>", "\n").replaceAll("<[^>]*>", "");
        return HtmlUtils.htmlUnescape(safe).replace('\u00a0', ' ').replaceAll("[ \\t]+", " ").replaceAll("\\n[ \\t]*\\n(?:[ \\t]*\\n)+", "\n\n").strip();
    }

    private static String text(JsonNode node, String field) { return node.path(field).asString(""); }
    private static Map<String, Object> resource(String type, String id) { return m("resourceType", type, "id", id); }
    private static Map<String, Object> identifier(String kind, String value) { return m("system", LOCAL + "identifier/" + kind, "value", value); }
    private static Map<String, Object> ref(String id) { return m("reference", "urn:uuid:" + id); }
    private static Map<String, Object> concept(String code, String text) {
        var result = m();
        if (!code.isBlank()) result.put("coding", List.of(m("system", LOCAL + "CodeSystem/source-sks", "code", code)));
        if (!text.isBlank()) result.put("text", text);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void extension(Map<String, Object> resource, String name, String valueType, Object value) {
        var extensions = (List<Map<String, Object>>) resource.computeIfAbsent("extension", _ -> new ArrayList<Map<String, Object>>());
        extensions.add(m("url", LOCAL + "StructureDefinition/" + name, valueType, value));
    }

    private static Map<String, Object> m(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
