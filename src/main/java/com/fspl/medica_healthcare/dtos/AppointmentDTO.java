package com.fspl.medica_healthcare.dtos;

import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.User;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
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
        private Patient patient;
        private User doctor;

        @NotNull
        @Enumerated(EnumType.STRING)
        private AppointmentStatus appointmentStatus;

        private LocalDateTime appointmentDateAndTime;
        private LocalDate nextAppointmentDate;
        private User createdUser;
        private User modifiedUser;
        private LocalDateTime createdDate;
        private LocalDateTime modifiedDate;
        private int status; // 0 = inactive, 1 = active
        private String symptoms;
        private String pulseRate;
        private String clinicalNote;
        private int fetchClinicalNote;
        private User currentDoctor;
        private String height;
        private double weight;
        private String bloodPressure;
        private String heartRate;
        private double bodyTemperature;
        private String respiratoryRate;
        private String allergies;


}
