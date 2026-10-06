package com.netcompany.fhir.sdk.api.demo.exception;

public record ApiError(int status, String error, String message) {
}
