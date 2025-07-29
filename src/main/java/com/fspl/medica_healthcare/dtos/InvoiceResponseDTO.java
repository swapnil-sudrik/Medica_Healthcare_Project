package com.fspl.medica_healthcare.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fspl.medica_healthcare.enums.InvoiceStatus;
import com.fspl.medica_healthcare.enums.PaymentMode;
import com.fspl.medica_healthcare.enums.PolicyStatus;
import com.fspl.medica_healthcare.models.Deposit;
import com.fspl.medica_healthcare.models.HospitalizationInfo;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceResponseDTO {

    private long id;
    private double totalAmount;
    private double paidAmount;
    private double balanceAmount;
    private double doctorFee;
    private PaymentMode paymentMode;
    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private LocalDate dueDate;

    @JsonIgnoreProperties(value = {"createdUser","modifiedUser","currentDoctor"})
    private AppointmentDTO appointmentDTO;

    @JsonIgnoreProperties(value = {"createdUser","modifiedUser"})
    private HospitalizationInfo hospitalizationInfo;

    @JsonIgnoreProperties(value = {"appointment", "createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private List<Deposit> deposit;

    private InvoiceStatus status;
    private long policyNumber;
    private String policyCompanyName;
    private PolicyStatus policyStatus;
    private double policyAmount;

    private double quickCareCharges;

    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private UserResponseDTO createdUser;

    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private UserResponseDTO modifiedUser;


}
