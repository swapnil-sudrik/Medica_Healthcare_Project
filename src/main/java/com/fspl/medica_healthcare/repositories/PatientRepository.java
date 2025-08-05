package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("SELECT p FROM Patient p WHERE p.contactNumber = :contactNumber")
    List<Patient> findByContactNumber(@Param("contactNumber") String contactNumber);

    @Query("SELECT p FROM Patient p WHERE p.name = :name")
    List<Patient> findByName(@Param("name") String name);

    List<Patient> findByStatus(int status);

    List<Patient> findByCurrentStatus(int currentStatus);

    @Query("SELECT p FROM Patient p LEFT JOIN FETCH p.hospital " +
            "WHERE FUNCTION('MONTH', FUNCTION('STR_TO_DATE', p.dateOfBirth, '%Y-%m-%d')) = :birthdateMonth " +
            "AND FUNCTION('DAY', FUNCTION('STR_TO_DATE', p.dateOfBirth, '%Y-%m-%d')) = :birthdateDay")
    List<Patient> findPatientsByBirthdateWithHospital(@Param("birthdateMonth") int birthdateMonth,
                                                      @Param("birthdateDay") int birthdateDay);


    @Query(value = "SELECT * FROM Patient WHERE hospital_id = :hospital_id", nativeQuery = true)
    List<Patient> findAllPatientsByHospital(long hospital_id);

    @Query(value = "SELECT * FROM patient WHERE name = :name AND email_id = :emailId AND contact_number = :contactNumber", nativeQuery = true)
    Patient findPatientByDetails(@Param("name") byte[] name, @Param("emailId") byte[] emailId, @Param("contactNumber") String contactNumber
    );
}
