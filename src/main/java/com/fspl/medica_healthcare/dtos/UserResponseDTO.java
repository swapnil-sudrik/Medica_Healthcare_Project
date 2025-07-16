package com.fspl.medica_healthcare.dtos;

import com.fspl.medica_healthcare.enums.Roles;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String username;
    private String name;
    private String roles;
    private HospitalResponseDTO hospital; // Add Hospital DTO to include hospital details

}
