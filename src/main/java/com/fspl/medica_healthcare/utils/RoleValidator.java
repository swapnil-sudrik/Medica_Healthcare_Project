package com.fspl.medica_healthcare.utils;

import com.fspl.medica_healthcare.enums.Roles;
import com.fspl.medica_healthcare.exceptions.InvalidRoleException;

public class RoleValidator {
    public static Roles validateAndGetRole(String role) {
        try {
            return Roles.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Your role is not correct: " + role);
        }
    }
}
