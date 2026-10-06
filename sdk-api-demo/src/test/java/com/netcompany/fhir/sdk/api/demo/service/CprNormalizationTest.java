package com.netcompany.fhir.sdk.api.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.netcompany.fhir.sdk.api.demo.exception.InvalidInputException;

class CprNormalizationTest {

    @Test
    void normalizesCprWithoutHyphen() {
        assertEquals("0506889996", EjournalFileService.normalizeCpr("0506889996"));
    }

    @Test
    void normalizesCprWithHyphen() {
        assertEquals("0506889996", EjournalFileService.normalizeCpr("050688-9996"));
    }

    @Test
    void rejectsNonDigitValue() {
        assertThrows(InvalidInputException.class, () -> EjournalFileService.normalizeCpr("abc"));
    }

    @Test
    void rejectsPathLikeValue() {
        assertThrows(InvalidInputException.class, () -> EjournalFileService.normalizeCpr("../../foo"));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(InvalidInputException.class, () -> EjournalFileService.normalizeCpr("050688999"));
    }
}
