package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "staffs")
//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String email;

    private String name;

    @Lob
    private byte[] address;

    @Lob
    private byte[] branch;

    private String roles;

    private double doctorFee;

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private double salary;

//(10,2) means:
//            10 → Total digits allowed (both before and after the decimal point).
//            2 → Number of decimal places (digits after the decimal point).
//    Example: 12345678.99 (valid), 123456789.99 (invalid due to 11 digits total).
//    DEFAULT 0.00
//    If no value is provided for salary, the default value will be 0.00.
//    This ensures that NULL is not inserted if a value is missing.

    private int status;

    @ManyToOne
//    @JsonBackReference
    @JsonIgnoreProperties({"createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private Hospital hospital;


    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}, allowSetters = true) // Ignore to prevent recursion
    private User createdUser;

    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}, allowSetters = true) // Ignore to prevent recursion
    private User modifiedUser;

    private LocalDate createdDate;
    private LocalDate modifiedDate;

    private boolean isCareTaker;

    private String specalization;

    private int bookingStatus;

    private  Double bookingCharge;

    private String type;
}
