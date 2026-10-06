package com.netcompany.fhir.sdk.api.demo.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.netcompany.fhir.sdk.api.demo.config.EjournalProperties;
import com.netcompany.fhir.sdk.api.demo.exception.InvalidInputException;
import com.netcompany.fhir.sdk.api.demo.exception.ResourceNotFoundException;

@Service
public class EjournalFileService {

    private static final Logger log = LoggerFactory.getLogger(EjournalFileService.class);

    private final ObjectMapper objectMapper;
    private final EjournalProperties properties;

    public EjournalFileService(ObjectMapper objectMapper, EjournalProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public JsonNode getForloebsoversigt(String cpr) {
        String normalizedCpr = normalizeCpr(cpr);
        return readFile(normalizedCpr, "forloebsoversigt-" + normalizedCpr + ".json");
    }

    public JsonNode getCaveoplysninger(String cpr) {
        String normalizedCpr = normalizeCpr(cpr);
        return readFile(normalizedCpr, "caveoplysninger-" + normalizedCpr + ".json");
    }

    public JsonNode getDiagnoser(String cpr, UUID key) {
        String normalizedCpr = normalizeCpr(cpr);
        return readFile(normalizedCpr, "diagnoser-" + key + ".json");
    }

    public JsonNode getProcedurer(String cpr, UUID key) {
        String normalizedCpr = normalizeCpr(cpr);
        return readFile(normalizedCpr, "procedurer-" + key + ".json");
    }

    public JsonNode getKontaktperioder(String cpr, UUID key) {
        String normalizedCpr = normalizeCpr(cpr);
        return readFile(normalizedCpr, "kontaktperioder-" + key + ".json");
    }

    public JsonNode getNotater(String cpr, UUID key) {
        return readFile(normalizeCpr(cpr), "notater-" + key + ".json");
    }

    public JsonNode getEpikriser(String cpr, UUID key) {
        return readFile(normalizeCpr(cpr), "epikriser-" + key + ".json");
    }

    public static String normalizeCpr(String cpr) {
        if (cpr == null) {
            throw new InvalidInputException("Invalid CPR");
        }

        String normalizedCpr = cpr.replace("-", "");
        if (!normalizedCpr.matches("\\d{10}")) {
            throw new InvalidInputException("Invalid CPR");
        }

        return normalizedCpr;
    }

    private JsonNode readFile(String cpr, String filename) {
        Path dataRoot = Paths.get(properties.dataRoot()).toAbsolutePath().normalize();

        Path file = dataRoot.resolve("ejournal-" + cpr).resolve(filename).normalize();

        log.debug("Reading file: " + file);

        if (!file.startsWith(dataRoot)) {
            throw new InvalidInputException("Invalid path");
        }

        log.debug("Normalized CPR: {}, resource file: {}", cpr, file.getFileName());

        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Requested eJournal resource was not found");
        }

        return objectMapper.readTree(file.toFile());
    }
}
