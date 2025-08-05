package com.fspl.medica_healthcare.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StaffDTO {

    private long id;

    private String email;

    private String name;

    private String address;

    private String branch;

    private String roles;

    private double doctorFee;

    private double salary;

    private int status;

    @ManyToOne
    @JsonIgnoreProperties({"createdUser", "modifiedUser"}) // Ignore to prevent recursion
    private Hospital hospital;


    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private User createdUser;
//
    @ManyToOne
    @JsonIgnoreProperties(value = {"hospital", "createdUser", "modifiedUser", "staff"}) // Ignore to prevent recursion
    private User modifiedUser;

    private LocalDate createdDate;
    private LocalDate modifiedDate;

    private boolean isCareTaker;

    private String specalization;

    private int bookingStatus;

    private  Double bookingCharge;

    private String type;

}
