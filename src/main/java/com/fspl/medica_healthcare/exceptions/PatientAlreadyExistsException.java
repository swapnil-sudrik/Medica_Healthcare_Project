package com.fspl.medica_healthcare.exceptions;

public class PatientAlreadyExistsException extends  RuntimeException{
    public PatientAlreadyExistsException(String message) {
        super(message);
    }
}
