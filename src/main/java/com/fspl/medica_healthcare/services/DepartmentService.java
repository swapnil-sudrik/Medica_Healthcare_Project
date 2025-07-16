package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.enums.Roles;
import com.fspl.medica_healthcare.exceptions.*;
import com.fspl.medica_healthcare.models.Department;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.DepartmentRepository;
import com.fspl.medica_healthcare.repositories.UserRepository;
import com.fspl.medica_healthcare.utils.RoleValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Service
public class DepartmentService
{
    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;


    public List<Department> getAllDepartment() {
        return departmentRepository.findAll();
    }


    public Department getDepartmentById(Long departmentId) {
        return departmentRepository.findById(departmentId).orElseThrow(() -> new RecordNotFoundException("Department not found"));
    }


    public Department saveDepartment(String name,Department department)throws IOException {
        Department savedDepartment = departmentRepository.save(department);
        return savedDepartment;
    }

//    public List<Department> getAllActiveDepartmentsByHospitalId(Long hospitalId) {
//        return departmentRepository.findAllActiveDepartmentsByHospitalId(hospitalId);
//    }

    public void deleteDepartment(Department department) {
        departmentRepository.save(department);
    }

    public Department reactivateDepartment(Department department) {
        return departmentRepository.save(department);
    }

}
