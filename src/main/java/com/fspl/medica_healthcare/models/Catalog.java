package com.fspl.medica_healthcare.models;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Catalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JsonIgnore
    private Category category;

    private String name;


    private double fees;


    @Lob
    private byte[] description;

    @Lob
    private byte[] images;

    @ManyToOne
    private Hospital hospital;

    @ManyToOne
    private User createdUser;

    @ManyToOne
    private User modifiedUser;
    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private int status;


}
