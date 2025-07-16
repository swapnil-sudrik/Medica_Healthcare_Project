package com.fspl.medica_healthcare.models;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
public class Catalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String category;


    private String name;


    private Double fees;


    @Lob
    private byte[] description;

    @ManyToOne
    private Hospital hospital;

    @ManyToOne
    private User created;

    @ManyToOne
    private User modified;
    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private int status;
}
