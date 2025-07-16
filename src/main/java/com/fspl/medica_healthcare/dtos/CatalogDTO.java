package com.fspl.medica_healthcare.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CatalogDTO {
    private long id;

    @NotBlank(message = "Category is mandatory")
    private String category;

    @Size(max = 100, message = "Service Name cannot exceed 100 characters")
    @NotBlank(message = "Service name is mandatory")
    private String name;

    @NotNull(message = "Service fees is mandatory")
    private String fees;
    private String description;
    private int status;


}