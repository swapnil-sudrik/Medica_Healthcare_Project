package com.fspl.medica_healthcare.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fspl.medica_healthcare.enums.Roles;
import com.fspl.medica_healthcare.models.Hospital;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String name;
    private String roles;
    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private int status;
    private String branch;
    private String address;
    private String contactNumber;
    private LocalDate dateOfBirth;

    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private UserResponseDTO createdUser;

    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private UserResponseDTO modifiedUser;


    private HospitalResponseDTO hospital;
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private StaffDTO staff;
}
