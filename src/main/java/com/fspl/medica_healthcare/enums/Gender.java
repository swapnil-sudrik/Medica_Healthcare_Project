package com.fspl.medica_healthcare.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fspl.medica_healthcare.exceptions.InvalidGenderException;


public enum Gender {
    MALE, FEMALE, OTHER;

    @JsonCreator
    public static Gender fromValue(String value) {
        try {
            return Gender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidGenderException("Invalid Gender value: " + value);
        }
    }
}
