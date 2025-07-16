package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fspl.medica_healthcare.enums.AppointmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Data
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User doctor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus appointmentStatus;

    private int fetchClinicalNote;

    @ManyToOne
    private User currentDoctor;

    @NotNull(message = "Appointment date and time cannot be null")
    @JsonFormat(pattern = "dd/MM/yyyy hh:mm a")
    @Column(name = "appointment_date_and_time", nullable = false)
    private LocalDateTime appointmentDateAndTime;

    @JsonFormat(pattern = "dd/MM/yyyy")
    @Column(name = "next_appointment_date")
    private LocalDate nextAppointmentDate;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;


    @Column(nullable = false)
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    @Column(nullable = false)
    private int status; // Default to active

    @Lob
    private byte[] symptoms;

    @Lob
    private byte[] clinicalNote;

    @Lob
    private byte[] allergies;

    @Lob
    private byte[] pulseRate;

    @Lob
    private byte[] heartRate;

    @Lob
    private byte[] respiratoryRate;

    private String height;
    private double weight;
    private String bloodPressure;

    private double bodyTemperature;

    private int missedMailStatus;
    private int reminderMailStatus;

    @OneToOne
    @JsonManagedReference
    private HospitalizationInfo hospitalizationInfo;

}

