package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    private Boolean isHospitalized;
    private LocalDate dateOfAdmission;
    private LocalDate dateOfDischarge;

//    @OneToOne
//    @ManyToOne
//    private RoomCharge room;

    @ManyToOne
    private Catalog catalog;

    private BigDecimal nursingCharges;
    private BigDecimal additionalCharges;
    private Integer totalDaysAdmitted;


    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;

    private LocalDate createdDate;

    private LocalDate modifiedDate;
}
