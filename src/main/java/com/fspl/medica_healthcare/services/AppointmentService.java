package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.AppointmentRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;


@Service
public class AppointmentService {

    private static final Logger logger = Logger.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private UserService userService;

    // Save Appointment and handle possible exceptions
    public boolean saveAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return true; // Save successful
        } catch (Exception e) {
            logger.error("Unexpected error while saveAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser().getId());
            return false; // Save failed due to an error
        }
    }

    // Find Appointment by ID with exception handling
    public Appointment findAppointmentById(long id) {
        try {
            return appointmentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.error("Unexpected error while findAppointmentById() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }


    // Find Appointment by AppointmentStatus with exception handling
    public List<Appointment> findByAppointmentStatus(AppointmentStatus status) {
        try {
            List<Appointment> appointmentList = appointmentRepository.findByAppointmentStatus(status);
            if (appointmentList == null) {
                return Collections.emptyList();
            } else {
                return appointmentList;
            }
        } catch (Exception e) {
            logger.error("Unexpected error while findByAppointmentStatus() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }


    // Find all appointments with exception handling
    public List<Appointment> findAllAppointments() {
        try {
            return appointmentRepository.findAll();
        } catch (Exception e) {
            // throw new RuntimeException("Error while retrieving all appointments", e);
            logger.error("Unexpected error while findAllAppointments() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }

    // Find Appointments by Patient ID
    public List<Appointment> findAppointmentsByPatientId(long id) {
        try {
            return appointmentRepository.findByPatient_Id(id);
        } catch (Exception e) {
            //  throw new RuntimeException("Error while retrieving appointments for patient ID: " + id, e);
            logger.error("Unexpected error while findAppointmentsByPatientId() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }

    // Find Appointments by Date
    public List<Appointment> findAppointmentsByDate(LocalDate appointmentDate) {
        try {
            return appointmentRepository.findByAppointmentDate(appointmentDate);
        } catch (Exception e) {
            logger.error("Unexpected error while findAppointmentsByDate() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }


    // Find Appointments by branch
    public List<Appointment> getAppointmentsByBranch(String branch) {
        try {
            byte[] branches = branch.getBytes(); // Convert String to byte[]
            return appointmentRepository.findByBranch(branches);
        } catch (Exception e) {
            logger.error("Unexpected error while getAppointmentsByBranch() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }


    //    --------------------------new booking latest 15 Min Time added api--------------------------------------------------------------------
    public List<Appointment> findAppointmentsForDoctorInTimeRange(User doctor, LocalDateTime startAppointmentTime, LocalDateTime endAppointmentTime) {
        try {
            return appointmentRepository.findByDoctorAndAppointmentDateAndTimeBetween(doctor, startAppointmentTime, endAppointmentTime);
        } catch (Exception e) {
            logger.error("Unexpected error while findAppointmentsForDoctorInTimeRange() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }


    // Check if an appointment is within hospital operating hours
    public boolean isAppointmentWithinHospitalHours(LocalDateTime appointmentTime, Long hospitalId) {
        try {
            Hospital hospital = hospitalService.getHospitalById(hospitalId);
            LocalTime hospitalOpeningTime = hospital.getOpeningTime();
            LocalTime hospitalClosingTime = hospital.getClosingTime();

            return !appointmentTime.toLocalTime().isBefore(hospitalOpeningTime) && !appointmentTime.toLocalTime().isAfter(hospitalClosingTime.minusMinutes(15));
        } catch (Exception e) {
            logger.error("Unexpected error while isAppointmentWithinHospitalHours() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return false;
        }
    }

    // This method For user Team
    public List<Appointment> findByDoctor_Id(Long doctorId) {
        try {
            return appointmentRepository.findByDoctor_Id(doctorId);
        } catch (Exception e) {
            logger.error("Unexpected error while findByDoctor_Id() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }

}