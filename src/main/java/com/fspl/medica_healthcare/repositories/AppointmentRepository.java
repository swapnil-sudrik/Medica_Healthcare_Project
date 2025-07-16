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


    // Find appointments for a doctor working in a specific branch
    @Query("SELECT a FROM Appointment a WHERE a.doctor.branch = :branch")
    //@Query("SELECT a FROM Appointment a WHERE a.doctor.hospital.branch = :branch")
    // @Query("SELECT a FROM Appointment a WHERE a.hospital.branch = :branch")
    List<Appointment> findByBranch(@Param("branch") byte[] branch);
    //List<Appointment> findByBranch(@Param("branch") String branch);


    // Find appointments for a given doctor within a specific time range, checking availability with a 15-minute buffer before booking
    @Query("SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.appointmentDateAndTime BETWEEN :startTime AND :endTime")
    List<Appointment> findByDoctorAndAppointmentDateAndTimeBetween(
            @Param("doctor") User doctor,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);


    //  This is query is used by User
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id= :id")
    List<Appointment> findByDoctor_Id(@Param("id") Long id);
}

