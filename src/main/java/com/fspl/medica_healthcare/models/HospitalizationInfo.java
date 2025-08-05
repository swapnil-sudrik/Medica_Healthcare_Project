package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HospitalizationInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;
    private boolean isHospitalized;
    private LocalDate dateOfAdmission;
    private LocalDate dateOfDischarge;

//    @OneToOne
//    @ManyToOne
//    private RoomCharge room;

    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private Catalog catalog;

    private double nursingCharges;
    private double canteenCharges;
    private double additionalCharges;
    private int totalDaysAdmitted;


    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;


    private LocalDate createdDate;

    private LocalDate modifiedDate;
}
