package com.fspl.medica_healthcare.models;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fspl.medica_healthcare.enums.BillingStatus;
import com.fspl.medica_healthcare.enums.PaymentMode;
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
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
//    @JoinColumn(name = "appointment_id")
//    @JsonBackReference
    @JsonIgnore
    private Appointment appointment;


    private BigDecimal totalAmount;

    @NotNull(message = "Paid amount cannot be null")
    @DecimalMin(value = "0.0", message = "Paid amount must be greater than or equal to 0")
    private BigDecimal paidAmount;

    private BigDecimal balanceAmount;

//    @ManyToOne
    private BigDecimal doctorFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
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
    private BillingStatus status;


    @FutureOrPresent
    private LocalDate dueDate;

    @ManyToOne
    private HospitalizationInfo hospitalizationInfo;

}
