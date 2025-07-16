package com.fspl.medica_healthcare.exceptions;

public class RecordAlreadyExitException extends RuntimeException {
    public RecordAlreadyExitException(String message) {
        super(message);
    }
}
