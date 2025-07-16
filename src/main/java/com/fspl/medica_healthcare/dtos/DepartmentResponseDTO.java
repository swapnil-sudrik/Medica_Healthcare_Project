package com.fspl.medica_healthcare.dtos;


import lombok.Data;

@Data
public class DepartmentResponseDTO {
    private Long departmentId;
    private String name;
    private HospitalResponseDTO hospital;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HospitalResponseDTO getHospital() {
        return hospital;
    }

    public void setHospital(HospitalResponseDTO hospital) {
        this.hospital = hospital;
    }

    public DepartmentResponseDTO(Long departmentId, String name, HospitalResponseDTO hospital) {
        this.departmentId = departmentId;
        this.name = name;
        this.hospital = hospital;
    }
    public DepartmentResponseDTO()
    {
        super();
    }
}
