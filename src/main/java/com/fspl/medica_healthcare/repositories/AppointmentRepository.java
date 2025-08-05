package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Find appointments by status
    List<Appointment> findByAppointmentStatus(AppointmentStatus appointmentStatus);


    // Find appointments for a specific patient using the patient ID
    List<Appointment> findByPatient_Id(long id);


    // Find all appointments for a given date, ignoring time
    @Query("SELECT a FROM Appointment a WHERE DATE(a.appointmentDateAndTime) = :appointmentDate")
    List<Appointment> findByAppointmentDate(@Param("appointmentDate") LocalDate appointmentDate);

    @Query("SELECT a FROM Appointment a WHERE a.createdUser.branch = :branch AND a.hospital.id =:id")
    List<Appointment> findByBranch(@Param("branch") byte[] branch, long id);


    //  This is query is used by User
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id= :id")
    List<Appointment> findByDoctor_Id(@Param("id") Long id);

    boolean existsByPatientIdAndAppointmentStatus(Long patientId, AppointmentStatus appointmentStatus);

    @Query("SELECT a FROM Appointment a")
    List<Appointment> findAllAppointments();

    @Query(value = "SELECT * FROM appointment WHERE id = :id AND hospital_id = :hospitalId", nativeQuery = true)
    Appointment findAppointmentByIdAndHospital(@Param("id") Long id, @Param("hospitalId") Long hospitalId);

    List<Appointment> findByPatientIdAndHospitalId(Long patientId, Long hospitalId);
}

