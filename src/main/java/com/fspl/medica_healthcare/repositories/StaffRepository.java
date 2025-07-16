package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.dtos.RoleSalaryAverageDTO;
import com.fspl.medica_healthcare.models.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmail(String email);

    // Calculate total salary expenses for the hospital
    @Query("SELECT COALESCE(SUM(s.salary), 0) FROM Staff s WHERE s.hospital.id = :id AND s.status = 1")
    Double getTotalSalaryExpensesByHospitalId(long id);

    @Query("SELECT COALESCE(SUM(s.salary), 0) FROM Staff s " +
            "WHERE s.hospital.id = :id AND s.status = 1 " +
            "AND FUNCTION('YEAR', s.createdDate) <= :year " +
            "AND FUNCTION('MONTH', s.createdDate) <= :month")
    Double getTotalSalaryExpensesByHospitalIdAndDate(long id, int month, int year);



    @Query("SELECT COUNT(s.id) FROM Staff s WHERE s.hospital.id = :id AND s.status = 1")
    Long getNumberOfStaffByHospitalId(long id);

    @Query("SELECT COUNT(s.id) FROM Staff s " +
            "WHERE s.hospital.id = :id " +
            "AND s.status = 1 " +
            "AND FUNCTION('YEAR', s.createdDate) <= :year " +
            "AND FUNCTION('MONTH', s.createdDate) <= :month")
    long getNumberOfStaffByHospitalIdAndDate(long id, int month, int year);


    @Query("SELECT s FROM Staff s WHERE s.hospital.id = :id AND s.status = 1")
    List<Staff> findAllActiveStaffByHospitalId(long id);

    @Query("SELECT s FROM Staff s WHERE s.hospital.id = :id")
    List<Staff> findAllUsersByHospitalId(long id);


//
//    @Query("SELECT s.roles as role, AVG(s.salary) as averageSalary FROM Staff s WHERE s.hospital.hospitalId = :hospitalId GROUP BY s.roles")
//    List<RoleSalaryAverageDTO> findAverageSalaryByRole(Long hospitalId);

    @Query("SELECT s.roles as role, AVG(s.salary) as averageSalary, COUNT(s.roles) as roleCount " +
            "FROM Staff s WHERE s.hospital.id = :id " +
            "GROUP BY s.roles")
    List<RoleSalaryAverageDTO> findAverageSalaryByRole(long id);

    @Query("SELECT s.roles as role, AVG(s.salary) as averageSalary, COUNT(s.roles) as roleCount " +
            "FROM Staff s " +
            "WHERE s.hospital.id = :id " +
            "AND FUNCTION('YEAR', s.createdDate) <= :year " +
            "AND FUNCTION('MONTH', s.createdDate) <= :month " +
            "GROUP BY s.roles")
    List<RoleSalaryAverageDTO> findAverageSalaryByRoleAndDate(long id, int month, int year);


}
