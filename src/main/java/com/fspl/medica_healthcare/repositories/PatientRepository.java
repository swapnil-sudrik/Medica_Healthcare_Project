package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query(value = "SELECT * FROM patient WHERE contact_number LIKE CONCAT(:contactNumber, '%')", nativeQuery = true)
    List<Patient> findByContactNumber(@Param("contactNumber") String contactNumber);

    @Query(value = "SELECT * FROM Patient WHERE name LIKE CONCAT(:name, '%')", nativeQuery = true)
    List<Patient> findByName(@Param("name") String name);

    List<Patient> findByStatus(int status);

    List<Patient> findByCurrentStatus(int currentStatus);

    @Query("SELECT p FROM Patient p LEFT JOIN FETCH p.hospital WHERE FUNCTION('MONTH', p.dateOfBirth) = :birthdateMonth " +
            "AND FUNCTION('DAY', p.dateOfBirth) = :birthdateDay")
    List<Patient> findPatientsByBirthdateWithHospital(@Param("birthdateMonth") int birthdateMonth,
                                                      @Param("birthdateDay") int birthdateDay);

    Patient findByNameAndEmailIdAndContactNumber(String patientName, String emailId, String contactNumber);
}
