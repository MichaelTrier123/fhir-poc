package com.netcompany.fhir.demo.source;

import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public record JournalSource(String cpr, JsonNode overview, JsonNode cave,
                            List<Episode> episodes, List<Warning> warnings, int omittedEpisodes) {
    public record Episode(String key, JsonNode overview, Map<String, JsonNode> details) {}
    public record Warning(String code, String message, String episodeKey) {}
}
