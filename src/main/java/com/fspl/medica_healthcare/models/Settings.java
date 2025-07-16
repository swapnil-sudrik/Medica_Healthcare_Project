package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Getter;
import java.time.LocalTime;
import java.util.Date;

@Entity
@Table(name = "settings")
@Data
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String hospitalOpeningTime;

    private String hospitalClosingTime;

    private String hospitalWorkingDays;

    private String hospitalOffDays;

    @Lob
    @JsonIgnore
    private byte[] hospitalLetterHead;

    @Lob
    @JsonIgnore
    private byte[] hospitalLogo;

    @OneToOne
    private Hospital hospital;

    private String noOfAmbulances;

    @NotBlank(message = "Ambulance contact number must not be blank")
    @Pattern(regexp = "^\\d{10}$", message = "Contact number must be exactly 10 digits and only digits are allowed")
    private String ambulanceContactNumber;

    private String ambulanceBookingTime;

    private String ambulanceCharges;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;

    private Date createdDate;
    private Date modifiedDate;

    private int status;

}