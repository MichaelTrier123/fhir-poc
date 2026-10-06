package com.netcompany.fhir.demo.api;

import com.netcompany.fhir.demo.source.SourceException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    public record Error(int status, String message) {}

    @ExceptionHandler(SourceException.class)
    ResponseEntity<Error> source(SourceException ex) {
        return ResponseEntity.status(ex.status()).cacheControl(CacheControl.noStore()).body(new Error(ex.status(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Error> unexpected(Exception ex) {
        return ResponseEntity.internalServerError().cacheControl(CacheControl.noStore())
            .body(new Error(500, "Der opstod en fejl under transformationen."));
    }
}
