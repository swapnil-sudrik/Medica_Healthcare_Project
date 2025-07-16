package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
public class Hospital {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;

    @Lob
    private byte[] address;
    private String contactNumber;
    private String emailId;
    private LocalTime openingTime;
    private LocalTime closingTime;

    @Lob
    @JsonIgnore
    private byte[] letterHead;

    @Lob
    private byte[] branch;

    private String workingDays;
    private String offDays;
    private String numberOfUsers;

    @Lob
    private byte[] departments;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;

    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private int status;


}
