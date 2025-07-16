package com.fspl.medica_healthcare.models;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "departmentId")
@Entity
@Data
public class Department
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    @Pattern(regexp = "^[A-Za-z]+( [A-Za-z]+)*$", message = "Name must contain only alphabetic characters")
    @Size(max = 100, message = "Department Name cannot exceed 100 characters")
    @Column(nullable = false)
    @NotBlank(message = "Department name is mandatory")
    @Size(max = 100, message = "Department name cannot exceed 100 characters")
    private String name;

    @ManyToOne
    @JsonBackReference
    private Hospital hospitalId;

    @ManyToOne
    private User created;

    @ManyToOne
    private User modified;
    private LocalDate createdDate;
    private LocalDate modifiedDate;
    private Integer status;

    public Department(Long departmentId, String name, Hospital hospitalId, User created, User modified, LocalDate createdDate, LocalDate modifiedDate, Integer status) {
        this.departmentId = departmentId;
        this.name = name;
        this.hospitalId = hospitalId;
        this.created = created;
        this.modified = modified;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.status = status;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public @Pattern(regexp = "^[A-Za-z]+( [A-Za-z]+)*$", message = "Name must contain only alphabetic characters") @Size(max = 100, message = "Department Name cannot exceed 100 characters") @NotBlank(message = "Department name is mandatory") @Size(max = 100, message = "Department name cannot exceed 100 characters") String getName() {
        return name;
    }

    public void setName(@Pattern(regexp = "^[A-Za-z]+( [A-Za-z]+)*$", message = "Name Name must contain only alphabetic characters") @Size(max = 100, message = "Department Name cannot exceed 100 characters") @NotBlank(message = "Department name is mandatory") @Size(max = 100, message = "Department name cannot exceed 100 characters") String name) {
        this.name = name;
    }

    public Hospital getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Hospital hospitalId) {
        this.hospitalId = hospitalId;
    }

    public User getCreated() {
        return created;
    }

    public void setCreated(User created) {
        this.created = created;
    }

    public User getModified() {
        return modified;
    }

    public void setModified(User modified) {
        this.modified = modified;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDate getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDate modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Department() {
        super();
    }

}
