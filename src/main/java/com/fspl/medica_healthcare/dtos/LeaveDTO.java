package com.fspl.medica_healthcare.dtos;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class LeaveDTO {


    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;

    @NotNull(message = "User ID is required.")
    private long userid;

    @NotNull(message = "Concern is required.")
    @Size(min = 5, message = "Concern should have at least 5 characters.")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Concern should contain only letters and spaces.")
    private String concern;

    @NotNull(message = "From date is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String fromDate;

    @NotNull(message = "To date is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String toDate;

    @NotNull(message = "Leave type is required.")
    @Pattern(regexp = "^(CASUAL|SICK|PRIVILEGE|MATERNITY|PATERNITY|VACATION)$", message = "Invalid leave type. Allowed values are: CASUAL, SICK, PRIVILEGE, MATERNITY, PATERNITY,VACATION.")
    private String type;

}
