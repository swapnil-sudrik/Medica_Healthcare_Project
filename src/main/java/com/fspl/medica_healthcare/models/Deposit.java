package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fspl.medica_healthcare.enums.PaymentMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private double depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser","hospitalizationInfo","currentDoctor","doctor","patient"}) // Ignore to prevent recursion
    private Appointment appointment;

    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private User createdUser;

    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private User modifiedUser;

    private LocalDate createdDate;

    private LocalDate modifiedDate;
}
