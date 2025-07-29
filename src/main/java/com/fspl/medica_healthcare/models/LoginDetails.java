package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class LoginDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String username;

//    @Lob  // Store large data
//    @Column(unique = true, columnDefinition = "TEXT")
//    private String token;

    private String ipAddress;

    private LocalDateTime loginAt;

    private String browser;

    private String browserVersion;

    private String operatingSystem;

    private String deviceType;

    @ManyToOne
    @JsonIgnore
    private User created;

    @Override
    public String toString() {
        return "LoginDetails{" +
                "id=" + id +
                ", username='" + username + '\'' +
//                ", token='" + token + '\'' +
                ", issuedAt=" + loginAt +
                '}';
    }
}
