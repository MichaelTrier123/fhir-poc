package com.netcompany.fhir.sdk.api.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.netcompany.fhir.sdk.api.demo.config.EjournalProperties;
import com.netcompany.fhir.sdk.api.demo.exception.ResourceNotFoundException;

class EjournalFileServiceTest {

    @TempDir
    static Path tempDir;

    private static EjournalFileService service;

    @BeforeAll
    static void setup() throws Exception {
        Path cprDir = tempDir.resolve("ejournal-0506889996");
        Files.createDirectories(cprDir);
        Files.writeString(cprDir.resolve("forloebsoversigt-0506889996.json"), "{\"forloeb\": \"data\"}");
        service = new EjournalFileService(JsonMapper.builder().build(), new EjournalProperties(tempDir.toString()));
    }

    @Test
    void loadsForloebsoversigtFile() throws Exception {
        JsonNode node = service.getForloebsoversigt("0506889996");
        assertEquals("data", node.get("forloeb").asString());
    }

    @Test
    void missingResourceThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.getCaveoplysninger("0506889996"));
    }

    @Test
    void missingCprDirectoryThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.getForloebsoversigt("1111111111"));
    }

    @Test
    void resolvesFormattedCprToSameFile() throws Exception {
        JsonNode node = service.getForloebsoversigt("050688-9996");
        assertTrue(node.has("forloeb"));
    }
}
