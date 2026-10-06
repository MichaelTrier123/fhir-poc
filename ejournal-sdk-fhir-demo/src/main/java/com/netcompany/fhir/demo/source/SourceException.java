package com.netcompany.fhir.demo.source;

public class SourceException extends RuntimeException {
    private final int status;

    public SourceException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() { return status; }
}
