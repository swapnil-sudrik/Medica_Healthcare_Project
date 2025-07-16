package com.fspl.medica_healthcare.dtos;

import lombok.Data;

import java.math.BigDecimal;

public interface RoleSalaryAverageDTO {
    String getRole();
    BigDecimal getAverageSalary();
    Long getRoleCount();
}
