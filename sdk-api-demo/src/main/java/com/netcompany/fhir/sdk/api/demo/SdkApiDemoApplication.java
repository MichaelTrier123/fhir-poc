package com.netcompany.fhir.sdk.api.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SdkApiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdkApiDemoApplication.class, args);
    }

}
