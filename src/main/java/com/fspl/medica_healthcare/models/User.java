package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;


@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Username is mandatory")
    @Email(message = "Username must be a valid email address")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$",
            message = "Username must be a valid email address and should not start with a number or contain invalid domains"
    )
    @Size(max = 100, message = "Username cannot exceed 100 characters")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Name is mandatory")
    @Pattern(
            regexp = "^[a-zA-Z\\s]+$",
            message = "Name can only contain letters and spaces"
    )
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Column(nullable = false)
    private String name;



    @NotBlank(message = "Password is mandatory")
    private String password;

    private byte[] branch;

    //    @Enumerated(EnumType.STRING) // Store enum as String in DB
//    private Roles roles;
    @NotBlank(message = "Role is mandatory")
//    @Pattern(
//            regexp = "ADMIN|DOCTOR|RECEPTIONIST|SUPER_ADMIN",
//            message = "Role is not correct. Allowed values are: ADMIN, DOCTOR, SUPER_ADMIN, RECEPTIONIST"
//    )
    @Column(nullable = false)
    private String roles;


    @ManyToOne
//    @JsonBackReference
    @JsonIgnoreProperties("user") // Prevent cyclic references
    private Hospital hospital;

    @ManyToOne
//    @JsonIgnore
    private User createdUser;

    @ManyToOne
//    @JsonIgnore
    private User modifiedUser;

    private LocalDate createdDate;

    private LocalDate modifiedDate;

    private int status;

    @OneToOne
    @JsonIgnore
    private Staff staff;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", roles='" + roles + '\'' +
//                ", hospital=" + hospital.getHospitalName() +
                ", createdDate=" + createdDate +
                ", modifiedDate=" + modifiedDate +
                ", status=" + status +
                '}';
    }
}

