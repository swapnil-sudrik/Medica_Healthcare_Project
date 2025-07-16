package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;


@Entity
@Table(name = "prescription")
@Data
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;
    private List<String> type;
    private List<String> medicine;
    private List<String> dosage;
    private List<String> schedule;


    @NotNull
    private List<Integer> quantity;


    @NotNull(message = "Number of days is required")
    @Column(nullable = false)
    private List<Integer> numberOfDays;


    @ManyToOne
    @JsonBackReference
    private Appointment appointment;


    @ManyToOne
    @JsonBackReference
    private User createdUser;


    @ManyToOne
    @JsonBackReference
    private User loginUser;


    @Column(nullable = false)
    private LocalDateTime createdDate;


    @Column(nullable = false)
    private LocalDateTime modifiedDate;


    @Column(nullable = false)
    private int status; // true = 1, false = 0


    @Override
    public String toString() {
        return "Prescription{" +
                "schedule=" + schedule +
                ", prescriptionId=" + Id +
                ", type=" + type +
                ", medicine=" + medicine +
                ", dosage=" + dosage +
                ", quantity=" + quantity +
                ", numberOfDays=" + numberOfDays +
                ", createdDate=" + createdDate +
                ", modifiedDate=" + modifiedDate +
                ", status=" + status +
                '}';
    }
}

