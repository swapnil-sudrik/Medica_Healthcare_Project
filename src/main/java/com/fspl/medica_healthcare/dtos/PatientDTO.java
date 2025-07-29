package com.fspl.medica_healthcare.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

@Validated
@Data
public class PatientDTO {

    private Long id;
    private String name;
    private String contactNumber;
    private String whatsAppNumber;
    private String emailId;
    private String dateOfBirth;
    private String gender;
    private String bloodGroup;
    private int age;
    private Integer status;
    private Integer currentStatus;
    private String diet;
    private User currentDoctor;
    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private Hospital hospital;
    @JsonIgnoreProperties(value = {"createdUser","modifiedUser","hospital","staff"})
    private User createdUser;
    @JsonIgnoreProperties(value = {"createdUser","modifiedUser","hospital","staff"})
    private User modifiedUser;

}
