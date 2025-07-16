package com.fspl.medica_healthcare.dtos;

import com.fspl.medica_healthcare.models.Appointment;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class PrescriptionDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;


    @ElementCollection
    @Column(name = "type")
    @NotNull(message = " type is required")
    private List<String> type;


    @NotNull(message = " medicine is required")
    private List<String> medicine;


    @NotNull(message = " dosage is required")
    private List<String> dosage;

    @NotNull(message = " schedule is required")
    private List<String> schedule;


    @NotNull(message = "quantity is required")
    private List<Integer> quantity;


    @NotNull(message = "Number of days is required")
    private List<Integer> numberOfDays;


    @NotNull(message = "Appoinment ID cannot be null")
    private long appoinmentId;

}
