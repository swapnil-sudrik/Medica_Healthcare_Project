package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;


import java.time.LocalDate;

@Entity
@Data
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Lob
    private byte[] name;
    private String contactNumber;
    private String whatsAppNumber;

    @Lob
    private byte[] emailId;

    private String dateOfBirth;


    private String gender;
    private String bloodGroup;
    private String age;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;

    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private int status;
    private int currentStatus;

    @Pattern(regexp = "^(VEG|NONVEG|VEG\\+EGGS)$",
            message = "Diet must be only VEG, NONVEG, or VEG+EGGS")
    private String diet;

    @ManyToOne
    private User currentDoctor;

    @ManyToOne(fetch = FetchType.LAZY) //  Ensure proper lazy fetching
    @JsonBackReference
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;


}