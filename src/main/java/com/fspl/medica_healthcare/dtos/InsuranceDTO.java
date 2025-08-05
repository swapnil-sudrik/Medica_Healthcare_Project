package com.fspl.medica_healthcare.dtos;

import com.fspl.medica_healthcare.enums.PaymentMode;
import com.fspl.medica_healthcare.enums.PolicyStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class InsuranceDTO {
    private PaymentMode paymentMode;
    private long policyNumber;
    private String policyCompanyName;
    @Enumerated(EnumType.STRING)
    private PolicyStatus policyStatus;
    private double policyAmount;
    private double quickCareCharge;
    private double paidAmount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;
}
