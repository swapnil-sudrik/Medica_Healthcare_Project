package com.fspl.medica_healthcare.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.User;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@ToString
public class AppointmentDTO {

        private long id;
        private PatientDTO patientDTO;

        @JsonIgnoreProperties(value = {"createdUser","modifiedUser","hospital","staff"})
        private User doctor;
        @NotNull
        @Enumerated(EnumType.STRING)
        private AppointmentStatus appointmentStatus;
        private String appointmentDateAndTime;
        private String nextAppointmentDate;
        private LocalDate createdDate;
        private LocalDate modifiedDate;
        private int status; // 0 = inactive, 1 = active
        private String symptoms;
        private String pulseRate;
        private String clinicalNote;
        private Integer fetchClinicalNote;
        @JsonIgnoreProperties(value = {"createdUser","modifiedUser","hospital","staff"})
        private User currentDoctor;
        private String height;
        private Double weight;
        private String bloodPressure;
        private String heartRate;
        private Double bodyTemperature;
        private String respiratoryRate;
        private String allergies;
        @JsonIgnoreProperties(value = {"createdUser","modifiedUser","hospital","staff"})
        private User createdUser;
        @JsonIgnoreProperties(value = {"createdUser","modifiedUser","hospital","staff"})
        private User modifiedUser;


}
