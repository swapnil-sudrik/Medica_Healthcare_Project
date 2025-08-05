package com.fspl.medica_healthcare.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String fromUser;

    @ManyToOne
    private User user;

    @ManyToOne
    private Hospital hospital;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;
}
