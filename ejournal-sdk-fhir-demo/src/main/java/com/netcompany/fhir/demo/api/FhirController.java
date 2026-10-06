package com.netcompany.fhir.demo.api;

import com.netcompany.fhir.demo.mapping.FhirMapper;
import com.netcompany.fhir.demo.source.SdkClient;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir")
public class FhirController {
    private final SdkClient client;
    private final FhirMapper mapper;

    public FhirController(SdkClient client, FhirMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @GetMapping("/{cpr}")
    public ResponseEntity<FhirMapper.Result> journal(@PathVariable String cpr) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mapper.transform(client.fetch(cpr)));
    }

    @GetMapping(value = "/{cpr}/bundle", produces = "application/fhir+json")
    public ResponseEntity<Map<String, Object>> bundle(@PathVariable String cpr) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType("application/fhir+json"))
            .body(mapper.transform(client.fetch(cpr)).bundle());
    }
}
