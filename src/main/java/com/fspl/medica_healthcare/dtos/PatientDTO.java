package com.fspl.medica_healthcare.dtos;

import com.fspl.medica_healthcare.models.User;
import lombok.Data;
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


    private LocalDate dateOfBirth;


    private String gender;


    private String bloodGroup;
    private int age;
    private int status;

    private int currentStatus;

    private String diet;

    private User currentDoctor;

}
