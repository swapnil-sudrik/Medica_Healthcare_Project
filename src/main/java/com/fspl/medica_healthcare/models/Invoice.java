package com.fspl.medica_healthcare.models;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fspl.medica_healthcare.enums.InvoiceStatus;
import com.fspl.medica_healthcare.enums.PaymentMode;
import com.fspl.medica_healthcare.enums.PolicyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    //    @OneToOne
//    @JoinColumn(name = "appointment_id")
//    @JsonBackReference
//    @JsonIgnore
    @ManyToOne
    private Appointment appointment;


    private double totalAmount;

    //    @NotNull(message = "Paid amount cannot be null")
//    @DecimalMin(value = "0.0", message = "Paid amount must be greater than or equal to 0")
    private double paidAmount;

    private double balanceAmount;

    //    @ManyToOne
    private double doctorFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
//    @NotBlank(message = "Payment mode is mandatory"
    @NotNull(message = "payment mode is mandatory")
//    @Pattern(
//            regexp = "UPI|DEBIT_CARD|CREDIT_CARD|NET_BANKING",
//            message = "Payment mode is not correct. Allowed values are: UPI,\n" +
//                    "    DEBIT_CARD,\n" +
//                    "    CREDIT_CARD,\n" +
//                    "    NET_BANKING"
//    )
    private PaymentMode paymentMode;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;

    private LocalDate createdDate;

    private LocalDate modifiedDate;

    @Enumerated(EnumType.STRING)
//    @NotBlank(message = "Billing Status is mandatory")
    @NotNull(message = "payment mode is mandatory")

//    @Pattern(
//            regexp = "COMPLETE|PARTIALLY_PAID|UNPAID",
//            message = "Role is not correct. Allowed values are: COMPLETE,\n" +
//                    "    PARTIALLY_PAID,\n" +
//                    "    UNPAID,"
//    )
    private InvoiceStatus status;


    @FutureOrPresent
    private LocalDate dueDate;

    @ManyToOne
    private HospitalizationInfo hospitalizationInfo;

    private long policyNumber;

    private String policyCompanyName;

    private PolicyStatus policyStatus;

    private double policyAmount;

    private double quickCareCharges;

}
