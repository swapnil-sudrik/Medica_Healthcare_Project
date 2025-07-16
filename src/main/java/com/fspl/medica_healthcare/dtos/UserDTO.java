package com.fspl.medica_healthcare.dtos;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserDTO {

    @NotBlank(message = "Username is mandatory")
    @Email(message = "Username must be a valid email address")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$",
            message = "Username must be a valid email address and should not start with a number or contain invalid domains"
    )
    @Size(max = 100, message = "Username cannot exceed 100 characters")
    private String username;

    //    @NotBlank(message = "Password is mandatory")
    private String password;

    @NotBlank(message = "Name is mandatory")
    @Pattern(
            regexp = "^[a-zA-Z\\s]+$",
            message = "Name can only contain letters and spaces"
    )
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Address is mandatory")
    private String address;

    //    @NotBlank(message = "Branch is mandatory")
    private String branch;


    //    @NotBlank(message = "Role is mandatory")
//    @Pattern(
//            regexp = "ADMIN|DOCTOR|RECEPTIONIST|SUPER_ADMIN",
//            message = "Role is not correct. Allowed values are: ADMIN, DOCTOR, SUPER_ADMIN, RECEPTIONIST"
//    )
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

}
