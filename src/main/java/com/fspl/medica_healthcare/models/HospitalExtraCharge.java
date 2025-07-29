package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "hospital_extra_charges")
@Data
public class HospitalExtraCharge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 100)
    @NotNull(message = "Charge type cannot be blank")
    private String chargeType;

    private BigDecimal amount;

    @JsonIgnore
    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name="hospital_id",nullable = false)
    private Hospital hospital;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;

    private LocalDate paymentDate;
    private LocalDate modifyPaymentDate;

    public HospitalExtraCharge() {
    }

    public HospitalExtraCharge(long id, String chargeType, BigDecimal amount, Hospital hospital, User createdUser, User modifiedUser, LocalDate paymentDate, LocalDate modifyPaymentDate) {
        this.id = id;
        this.chargeType = chargeType;
        this.amount = amount;
        this.hospital = hospital;
        this.createdUser = createdUser;
        this.modifiedUser = modifiedUser;
        this.paymentDate = paymentDate;
        this.modifyPaymentDate = modifyPaymentDate;
    }
}


