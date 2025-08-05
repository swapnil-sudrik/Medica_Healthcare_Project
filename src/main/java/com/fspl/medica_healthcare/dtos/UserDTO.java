package com.fspl.medica_healthcare.dtos;

import com.fspl.medica_healthcare.models.User;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UserDTO {

//    private long id;

    @NotBlank(message = "Username is mandatory")
    @Email(message = "Username must be a valid email address")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$",
            message = "Username must be a valid email address and should not start with a number or contain invalid domains"
    )
    @Size(max = 100, message = "Username cannot exceed 100 characters")
    private String username;

    //    @NotBlank(message = "Password is mandatory")
//    private String password;

    @NotBlank(message = "Name is mandatory")
    @Pattern(
            regexp = "^[a-zA-Z\\s]+$",
            message = "Name can only contain letters and spaces"
    )
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Address is mandatory")
    private String address;

    private String branch;

    private String roles;

    private double doctorFee;

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private double salary;

    //add staff changes...
    private boolean careTaker;
    private String specalization;
    private int bookingStatus;
    private  Double bookingCharge;
    private String type;

    @NotBlank(message = "contact number is mandatory")
    @Pattern(regexp = "\\d{10}", message = "Contact number must be exactly 10 digits or please put the correct number")
    private String contactNumber;

    @NotNull(message = "Date of Birth is mandatory")
    private LocalDate dateOfBirth;

    //////////////////////////////////////////


//(10,2) means:
//            10 → Total digits allowed (both before and after the decimal point).
//            2 → Number of decimal places (digits after the decimal point).
//    Example: 12345678.99 (valid), 123456789.99 (invalid due to 11 digits total).
//    DEFAULT 0.00
//    If no value is provided for salary, the default value will be 0.00.
//    This ensures that NULL is not inserted if a value is missing.

//    private LocalDate createdDate;
//    private LocalDate modifiedDate;
//
//    private int status;
//    private UserDTO createdUser;
//    private UserDTO modifiedUser;
//    private HospitalResponseDTO hospital;
//    private StaffDTO staff;

}
