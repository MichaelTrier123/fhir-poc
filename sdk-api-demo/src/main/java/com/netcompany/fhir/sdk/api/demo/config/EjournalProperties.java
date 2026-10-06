package com.netcompany.fhir.sdk.api.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ejournal")
public record EjournalProperties(String dataRoot) {
}
