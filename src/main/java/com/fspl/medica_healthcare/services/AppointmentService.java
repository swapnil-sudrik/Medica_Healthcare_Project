package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.dtos.AppointmentDTO;
import com.fspl.medica_healthcare.dtos.PatientDTO;
import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.repositories.AppointmentRepository;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;


@Service
public class AppointmentService {

    private static final Logger logger = Logger.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private UserService userService;

    @Autowired
    private EncryptionUtil util;

    @Autowired
    private SettingsService settingsService;

    // Save Appointment and handle possible exceptions
    public boolean saveAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return true; // Save successful
        } catch (Exception e) {
            logger.error("Unexpected error while saveAppointment() :" + ExceptionUtils.getStackTrace(e));
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
            logger.error("Unexpected error while findByAppointmentStatus() :" + ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    // Checks if the given patient has a scheduled appointment.
    public boolean hasScheduledNextAppointment(Long patientId) {
        try {
            return appointmentRepository.existsByPatientIdAndAppointmentStatus(patientId, AppointmentStatus.SCHEDULED);
        } catch (Exception e) {
            logger.error("Error while checking scheduled appointment for patient ID () :" + ExceptionUtils.getStackTrace(e));
            return false;
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


    public List<Appointment> findByPatientIdAndHospitalId(Long patientId, Long hospitalId) {
        try {
            return appointmentRepository.findByPatientIdAndHospitalId(patientId, hospitalId);
        } catch (Exception e) {
            logger.error("Error in findByPatientIdAndHospitalId(): " + ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Failed to fetch appointments for patient ID: " + patientId + " and hospital ID: " + hospitalId);
        }
    }


    //Find appointments By Date
    public List<Appointment> findAppointmentsByDate(LocalDate appointmentDate) {
        List<Appointment> filteredAppointments = new ArrayList<>();

        try {
            List<Appointment> allAppointments = appointmentRepository.findAllAppointments();

            for (Appointment appointment : allAppointments) {
                try {
                    String decryptedDateTime = util.decrypt(appointment.getAppointmentDateAndTime());

                    // Adjust parsing to handle both date and datetime strings
                    LocalDate appointmentLocalDate = LocalDate.parse(decryptedDateTime.substring(0, 10));

                    if (appointmentLocalDate.equals(appointmentDate)) {
                        filteredAppointments.add(appointment);
                    }
                } catch (Exception ex) {
                    logger.warn("Decryption failed for appointment ID: " + appointment.getId(), ex);
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error while findAppointmentsByDate(): " +
                    ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User: " + userService.getAuthenticateUser());
        }

        return filteredAppointments;
    }

    // Find Appointments by branch
    public List<Appointment> getAppointmentsByBranch(byte[] branch, long hospitalId) {
        try {
            return appointmentRepository.findByBranch(branch, hospitalId);
        } catch (Exception e) {
            logger.error("Unexpected error while getAppointmentsByBranch() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return null;
        }
    }


    // Check if an appointment is within hospital operating hours
    public boolean isAppointmentWithinHospitalHours(LocalDateTime appointmentTime, Long hospitalId) {
        try {
            Hospital hospital = hospitalService.getHospitalById(hospitalId);

            Optional<Settings> optionalSettings = settingsService.findSettingsByHospital(hospital);
            if (optionalSettings.isEmpty()) {
                logger.warn("No settings found for hospital with ID: " + hospitalId);
                return false;
            }

            Settings settings = optionalSettings.get();

            LocalTime hospitalOpeningTime = LocalTime.parse(settings.getHospitalOpeningTime());
            LocalTime hospitalClosingTime = LocalTime.parse(settings.getHospitalClosingTime());

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

    public Appointment findAppointmentByIdAndHospital(Long id, Long hospitalId) {
        return appointmentRepository.findAppointmentByIdAndHospital(id, hospitalId);
    }


    public AppointmentDTO appointmentToAppointmentDto(Appointment appointment) {
        AppointmentDTO appointmentDto = new AppointmentDTO();
        Patient patient = appointment.getPatient();
        PatientDTO patientDTO = new PatientDTO();

        if (patient != null) {
            patientDTO.setId(patient.getId());
            patientDTO.setName(util.decrypt(new String(appointment.getPatient().getName())));
            patientDTO.setContactNumber(util.decrypt(patient.getContactNumber()));
            patientDTO.setEmailId(util.decrypt(new String(patient.getEmailId())));
            patientDTO.setWhatsAppNumber(util.decrypt(patient.getWhatsAppNumber()));
            patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));
            patientDTO.setGender(util.decrypt(patient.getGender()));
            patientDTO.setBloodGroup(util.decrypt(patient.getBloodGroup()));
            patientDTO.setAge(Integer.parseInt(util.decrypt(patient.getAge() + "")));
            patientDTO.setStatus(patient.getStatus());
            patientDTO.setCurrentStatus(patient.getCurrentStatus());
            patientDTO.setDiet(patient.getDiet() != null ? util.decrypt(patient.getDiet()) : "");
            patientDTO.setCreatedUser(patient.getCreatedUser());
            patientDTO.setModifiedUser(patient.getModifiedUser());
            patientDTO.setCreatedDate(patient.getCreatedDate());
            patientDTO.setModifiedDate(patient.getModifiedDate());
        }
        appointmentDto.setId(appointment.getId());
        appointmentDto.setPatientDTO(patientDTO);
        appointmentDto.setDoctor(appointment.getDoctor());
        appointmentDto.setAppointmentStatus(appointment.getAppointmentStatus());
        appointmentDto.setAppointmentDateAndTime(util.decrypt(appointment.getAppointmentDateAndTime()));
        appointmentDto.setNextAppointmentDate(appointment.getNextAppointmentDate());
        appointmentDto.setCreatedUser(appointment.getCreatedUser());
        appointmentDto.setModifiedUser(appointment.getModifiedUser());
        appointmentDto.setCreatedDate(appointment.getCreatedDate());
        appointmentDto.setModifiedDate(appointment.getModifiedDate());
        appointmentDto.setStatus(appointment.getStatus());
        appointmentDto.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
        appointmentDto.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
        appointmentDto.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
        appointmentDto.setFetchClinicalNote(appointment.getFetchClinicalNote());
        appointmentDto.setCurrentDoctor(appointment.getCurrentDoctor() != null ? appointment.getCurrentDoctor() : null);
        appointmentDto.setHeight(appointment.getHeight() != null ? util.decrypt(appointment.getHeight()) : "");
        appointmentDto.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(appointment.getBloodPressure()) : "");
        appointmentDto.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
        appointmentDto.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
        appointmentDto.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
        return appointmentDto;
    }

    public boolean isAppointmentNotOnWeekOffDays(LocalDateTime appointmentTime, Long hospitalId) {
        Hospital hospital = hospitalService.getHospitalById(hospitalId);
        Optional<Settings> optionalSettings = settingsService.findSettingsByHospital(hospital);

        if (optionalSettings.isEmpty()) {
            logger.warn("No Setting found for hospital with ID: " + hospitalId);
            return false;
        }

        Settings settings = optionalSettings.get();
        String hospitalOffDays = settings.getHospitalOffDays();

        if (hospitalOffDays == null || hospitalOffDays.isEmpty()) {
            return true;
        }

        DayOfWeek appointmentDay = appointmentTime.getDayOfWeek();
        List<String> offDaysList = Arrays.asList(hospitalOffDays.split(","));

        return !offDaysList.contains(appointmentDay.toString());
    }

}