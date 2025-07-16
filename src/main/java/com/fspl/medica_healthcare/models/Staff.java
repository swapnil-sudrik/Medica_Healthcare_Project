package com.fspl.medica_healthcare.models;

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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "email is mandatory")
    @Email(message = "email must be a valid email address")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$",
            message = "email address and should not start with a number or contain invalid domains"
    )
    @Size(max = 100, message = "email cannot exceed 100 characters")
    @Column(nullable = false , unique = true)
    private String email;

    private String name;

    @Size(max = 100, message = "Address is mandatory")
    @Column(nullable = false)
    private byte[] address;

    private byte[] branch;

    @NotBlank(message = "Role is mandatory")
    @Column(nullable = false)
    private String roles;

    private BigDecimal doctorFee;


    //    @NotNull(message = "Salary is mandatory")
    @Column(nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal salary;

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
    @JsonIgnoreProperties("staff") // Prevent cyclic references
    private Hospital hospital;


    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;

    private LocalDate createdDate;
    private LocalDate modifiedDate;
}
