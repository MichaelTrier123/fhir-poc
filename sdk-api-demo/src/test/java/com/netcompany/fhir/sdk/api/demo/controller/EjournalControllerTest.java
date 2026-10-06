package com.netcompany.fhir.sdk.api.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EjournalControllerTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ejournal.data-root", tempDir::toString);
    }

    @Autowired
    MockMvc mockMvc;

    @BeforeAll
    static void setup() throws Exception {
        Path cprDir = tempDir.resolve("ejournal-0506889996");
        Files.createDirectories(cprDir);
        Files.writeString(cprDir.resolve("forloebsoversigt-0506889996.json"), "{\"forloeb\": \"data\"}");
        Files.writeString(cprDir.resolve("diagnoser-605bf396-4276-4497-ba8e-0f397d1b3eb3.json"), "{\"diag\": \"ok\"}");
        Files.writeString(cprDir.resolve("notater-605bf396-4276-4497-ba8e-0f397d1b3eb3.json"), "{\"Notater\": []}");
        Files.writeString(cprDir.resolve("epikriser-605bf396-4276-4497-ba8e-0f397d1b3eb3.json"), "{\"Epikriser\": []}");
    }

    @Test
    void forloebsoversigtReturnsJson() throws Exception {
        mockMvc.perform(get("/api/ejournal/0506889996/forloebsoversigt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.forloeb").value("data"));
    }

    @Test
    void forloebsoversigtWithFormattedCprReturnsJson() throws Exception {
        mockMvc.perform(get("/api/ejournal/050688-9996/forloebsoversigt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forloeb").value("data"));
    }

    @Test
    void diagnoserWithUuidReturnsJson() throws Exception {
        mockMvc.perform(get("/api/ejournal/0506889996/diagnoser")
                        .param("key", "605bf396-4276-4497-ba8e-0f397d1b3eb3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.diag").value("ok"));
    }

    @Test
    void missingResourceReturns404() throws Exception {
        mockMvc.perform(get("/api/ejournal/1111111111/forloebsoversigt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedCprReturns400() throws Exception {
        mockMvc.perform(get("/api/ejournal/abc/forloebsoversigt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedKeyReturns400() throws Exception {
        mockMvc.perform(get("/api/ejournal/0506889996/diagnoser").param("key", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void documentsAreAvailableWithValidatedKeys() throws Exception {
        for (String type : new String[]{"notater", "epikriser"}) {
            mockMvc.perform(get("/api/ejournal/050688-9996/" + type)
                    .param("key", "605bf396-4276-4497-ba8e-0f397d1b3eb3"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
            mockMvc.perform(get("/api/ejournal/0506889996/" + type).param("key", "invalid"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get("/api/ejournal/0506889996/" + type)
                    .param("key", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound());
        }
    }
}
