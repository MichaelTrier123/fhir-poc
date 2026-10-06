package com.netcompany.fhir.sdk.api.demo.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import com.netcompany.fhir.sdk.api.demo.exception.InvalidInputException;
import com.netcompany.fhir.sdk.api.demo.service.EjournalFileService;

@RestController
@RequestMapping("/api/ejournal")
public class EjournalController {

    private final EjournalFileService fileService;

    public EjournalController(EjournalFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/{cpr}/forloebsoversigt")
    public JsonNode forloebsoversigt(@PathVariable String cpr) {
        return fileService.getForloebsoversigt(cpr);
    }

    @GetMapping("/{cpr}/caveoplysninger")
    public JsonNode caveoplysninger(@PathVariable String cpr) {
        return fileService.getCaveoplysninger(cpr);
    }

    @GetMapping("/{cpr}/diagnoser")
    public JsonNode diagnoser(@PathVariable String cpr, @RequestParam("key") String key) {
        return fileService.getDiagnoser(cpr, parseUuid(key));
    }

    @GetMapping("/{cpr}/procedurer")
    public JsonNode procedurer(@PathVariable String cpr, @RequestParam("key") String key) {
        return fileService.getProcedurer(cpr, parseUuid(key));
    }

    @GetMapping("/{cpr}/kontaktperioder")
    public JsonNode kontaktperioder(@PathVariable String cpr, @RequestParam("key") String key) {
        return fileService.getKontaktperioder(cpr, parseUuid(key));
    }

    private UUID parseUuid(String key) {
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException("Invalid key");
        }
    }
}
