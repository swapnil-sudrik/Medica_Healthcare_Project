package com.fspl.medica_healthcare.dtos;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CatalogDTO {
    private long id;

    @NotBlank(message = "Category is mandatory")
    private String category;

    @Size(max = 100, message = "Catalog name too long.")
    @Size(min = 2, message = "Invalid Character")
    @NotBlank(message = "Invalid characters in catalog name.")
    private String name;

    @NotNull(message = "Service fees is mandatory")
    private String fees;

    private String description;

    private String image;

    private int status;

}