package com.fspl.medica_healthcare.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.itextpdf.kernel.pdf.annot.PdfFileAttachmentAnnotation;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Leaves")
@Data
public class Leaves {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;

    private String concern;

    private LocalDate date;

    private String type;

    @ManyToOne
    @JsonBackReference
    private User user;

}
