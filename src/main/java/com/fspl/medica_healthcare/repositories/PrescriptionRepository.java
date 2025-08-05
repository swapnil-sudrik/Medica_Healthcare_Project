package com.fspl.medica_healthcare.repositories;


import com.fspl.medica_healthcare.models.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription,Long> {

    @Query("SELECT p.medicine FROM Prescription p")
    List<List<String>>findAllMedicines();

    @Query("SELECT p FROM Prescription p JOIN p.appointment a JOIN a.patient pat WHERE pat.id = :patientId")
    List<Prescription> findPrescriptionsByPatientId(@Param("patientId") Long patientId);


    @Query("SELECT p FROM Prescription p WHERE p.appointment.id = :appointmentId")
    List <Prescription > findPrescriptionByAppointmentId(@Param("appointmentId") long appointmentId);
}

