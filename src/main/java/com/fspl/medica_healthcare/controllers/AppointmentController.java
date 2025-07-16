package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.AppointmentDTO;
import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.AppointmentService;
import com.fspl.medica_healthcare.services.EmailService;
import com.fspl.medica_healthcare.services.PatientService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.annotation.PostConstruct;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private static final Logger logger = Logger.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private UserService userService;

    @Autowired
    private EncryptionUtil util;


//    ------------------------------------------------------- Book Appointment----------------------------------------------

    @PostMapping("/bookAppointment")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> bookAppointment(@RequestBody AppointmentDTO appointmentDTO) {

        try {
            User loginUser = userService.getAuthenticateUser();
            Map<String, String> res = new HashMap<>();

            User doctor = userService.getUserById(appointmentDTO.getDoctor().getId(), loginUser);
            if (!doctor.getRoles().equals("DOCTOR")) {
                return ResponseEntity.status(404).body("Doctor not found");
            }

            Patient patientDTO = appointmentDTO.getPatient();
            LocalDateTime appointmentDateTime = appointmentDTO.getAppointmentDateAndTime();

            if (!appointmentService.isAppointmentWithinHospitalHours(appointmentDateTime, loginUser.getHospital().getId())) {
                res.put("Error", "Your chosen appointment time is outside the hospital's operational hours. Please select an available slot within the working schedule.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // Ensure the 15-minute gap between appointments for the same doctor
            LocalDateTime startTime = appointmentDateTime.minusMinutes(15);
            LocalDateTime endTime = appointmentDateTime.plusMinutes(15);
            List<Appointment> existingAppointments = appointmentService.findAppointmentsForDoctorInTimeRange(doctor, startTime, endTime);
            if (!existingAppointments.isEmpty()) {
                res.put("Error", "Doctor is not available at this time as they have a scheduled appointment");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // 1) Patient name validation
            if (appointmentDTO.getPatient().getName() == null || appointmentDTO.getPatient().getName().isEmpty() || !appointmentDTO.getPatient().getName().matches("^[a-zA-Z\\s]+$")) {
                res.put("Error", (appointmentDTO.getPatient().getName() == null || appointmentDTO.getPatient().getName().isEmpty()) ? "Patient name is required" : "Patient name should contains only alphabets and spaces");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // 2) Contact number validation
            if (appointmentDTO.getPatient().getContactNumber() == null || appointmentDTO.getPatient().getContactNumber().isEmpty() || !appointmentDTO.getPatient().getContactNumber().matches("^[0-9]{10}$")) {
                res.put("Error", (appointmentDTO.getPatient().getContactNumber() == null || appointmentDTO.getPatient().getContactNumber().isEmpty()) ? "Contact number is required" : "Contact number must be exactly 10 digits and contain only numbers");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // 3) Whatsapp number validation
            if (appointmentDTO.getPatient().getWhatsAppNumber() == null || appointmentDTO.getPatient().getWhatsAppNumber().isEmpty() || !appointmentDTO.getPatient().getWhatsAppNumber().matches("^[0-9]{10}$")) {
                res.put("Error", (appointmentDTO.getPatient().getWhatsAppNumber() == null || appointmentDTO.getPatient().getWhatsAppNumber().isEmpty()) ? "Whats App number must be required" : "Whatsapp number contains only numbers & must be 10 digits");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // 4) Email Validation
            if (!appointmentDTO.getPatient().getEmailId().matches("^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$")) {
                res.put("Error", "Email should be valid");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // 5) Gender Validation
            if (appointmentDTO.getPatient().getGender().isEmpty() || appointmentDTO.getPatient().getGender() == null || !appointmentDTO.getPatient().getGender().matches("MALE|FEMALE|OTHER")) {
                res.put("Error", appointmentDTO.getPatient().getGender().isEmpty() || appointmentDTO.getPatient().getGender() == null
                        ? "Gender selection is mandatory."
                        : "Only 'Male,' 'Female,' and 'Other' are allowed as valid gender options.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // 6) DOB validation
            LocalDate dob = appointmentDTO.getPatient().getDateOfBirth();
            if (appointmentDTO.getPatient().getDateOfBirth() == null || dob.isAfter(LocalDate.now())) {
                res.put("Error", appointmentDTO.getPatient().getDateOfBirth() == null
                        ? "Date of birth is mandatory."
                        : "Date of birth cannot be a future date.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // 7) Blood group validation
            if (appointmentDTO.getPatient().getBloodGroup() == null || appointmentDTO.getPatient().getBloodGroup().trim().isEmpty() || !appointmentDTO.getPatient().getBloodGroup().matches("^(A|B|AB|O)[+-]$")) {
                res.put("Error", appointmentDTO.getPatient().getBloodGroup() == null || appointmentDTO.getPatient().getBloodGroup().isEmpty()
                        ? "Blood group must be required"
                        : "Blood group must be one of the following: A+, A-, B+, B-, AB+, AB-, O+, O-");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            //8) Age Validation
            if (appointmentDTO.getPatient().getAge() <= 0 || appointmentDTO.getPatient().getAge() > 120) {
                res.put("Error", "Age must be a positive number and within a reasonable range (e.g., 1 to 120).");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }




            //10) Appointment date and time
            if (appointmentDTO.getAppointmentDateAndTime().isBefore(LocalDateTime.now()) || appointmentDTO.getAppointmentDateAndTime() == null) {
                res.put("Error", appointmentDTO.getAppointmentDateAndTime().isBefore(LocalDateTime.now())
                        ? "Appointment date and time must be in the present or future."
                        : "Appointment date and time must be required");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            appointmentDTO.getPatient().setName(util.encrypt(appointmentDTO.getPatient().getName()));
            appointmentDTO.getPatient().setContactNumber(util.encrypt(appointmentDTO.getPatient().getContactNumber()));
            appointmentDTO.getPatient().setWhatsAppNumber(util.encrypt(appointmentDTO.getPatient().getWhatsAppNumber()));
            appointmentDTO.getPatient().setEmailId(util.encrypt(appointmentDTO.getPatient().getEmailId()));
            appointmentDTO.getPatient().setGender(util.encrypt(appointmentDTO.getPatient().getGender()));
            appointmentDTO.getPatient().setBloodGroup(util.encrypt(appointmentDTO.getPatient().getBloodGroup()));

            // Check if the patient already exists
            Patient existingPatient = patientService.findPatientByDetails(patientDTO.getName(), patientDTO.getEmailId(), patientDTO.getContactNumber());

            Patient patient;

            if (existingPatient != null) {
                patient = existingPatient;
            } else {
                patient = patientService.savePatient(patientDTO);
            }

            // Create a new appointment
            Appointment appointment = new Appointment();
            appointment.setDoctor(userService.getUserById(appointmentDTO.getDoctor().getId(), loginUser));
            appointment.setPatient(appointmentDTO.getPatient());
            appointment.setHospital(loginUser.getHospital());
            appointment.setAppointmentDateAndTime(appointmentDTO.getAppointmentDateAndTime());
            appointment.setAppointmentStatus(AppointmentStatus.SCHEDULED);
            appointment.setCreatedUser(loginUser);
            appointment.setModifiedUser(loginUser);
            appointment.setCurrentDoctor(doctor);
            appointment.setCreatedDate(LocalDateTime.now());
            appointment.setStatus(1);

            // Save the appointment and check if it was successful
            boolean appointmentSaved = appointmentService.saveAppointment(appointment);
            if (appointmentSaved) {
                try {
                    emailService.sendEmail(appointment.getPatient().getEmailId(), "\ud83c\udf89 Mark Your Calendar – You’re Booked for Wellness!", getAppointmentConfirmationTemplete(appointment.getPatient().getName(), appointment.getDoctor().getName(), appointment.getHospital().getName(), appointment.getAppointmentStatus(), appointment.getAppointmentDateAndTime()));
                } catch (Exception emailException) {
                    logger.error("Failed to send confirmation email: " + emailException.getMessage());
                }

                appointment.getPatient().setName(util.decrypt(appointment.getPatient().getName()));
                appointment.getPatient().setContactNumber(util.decrypt(appointment.getPatient().getContactNumber()));
                appointment.getPatient().setWhatsAppNumber(util.decrypt(appointment.getPatient().getWhatsAppNumber()));
                appointment.getPatient().setEmailId(util.decrypt(appointment.getPatient().getEmailId()));
                appointment.getPatient().setGender(util.decrypt(appointment.getPatient().getGender()));
                appointment.getPatient().setBloodGroup(util.decrypt(appointment.getPatient().getBloodGroup()));

                return ResponseEntity.ok(appointment);
            } else {
                return ResponseEntity.status(500).body("Failed to Book appointment.");
            }
        } catch (RuntimeException e) {
            logger.error("Unexpected error while BookAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    //-------------------------------------------------Update Appointment---------------------------------------------------------------------
    @PutMapping("/updateAppointment/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<?> updateAppointment(@PathVariable long id, @RequestBody AppointmentDTO appointmentDTO) {
        try {

            User loginUser = userService.getAuthenticateUser();

            Appointment appointment = appointmentService.findAppointmentById(id);
            if (appointment != null) {
                Map<String, String> errors = new HashMap<>();
                Patient patient = appointment.getPatient();

                // 1) Patient name validation
                if (appointmentDTO.getPatient() != null) {

                    // 1) Patient Name validation
                    if (appointmentDTO.getPatient().getName() != null) {
                        if (!appointmentDTO.getPatient().getName().matches("^[A-Z][a-zA-Z\\s]+$")) {
                            errors.put("Patient Name", "Patient Name start with a capital letter.");
                        } else {
                            patient.setName(appointmentDTO.getPatient().getName());
                        }
                    }

                    // 2)Patient Contact Number validation
                    if (appointmentDTO.getPatient().getContactNumber() != null) {
                        if (!appointmentDTO.getPatient().getContactNumber().matches("^[1-9][0-9]{9}$")) {
                            errors.put("Contact Number", "Must be exactly 10 digits.");
                        } else {
                            patient.setContactNumber(appointmentDTO.getPatient().getContactNumber());
                        }
                    }

                    // 3)Patient WhatsApp Number validation
                    if (appointmentDTO.getPatient().getWhatsAppNumber() != null) {
                        if (!appointmentDTO.getPatient().getWhatsAppNumber().matches("^[1-9][0-9]{9}$")) {
                            errors.put("WhatsApp Number", "Must be exactly 10 digits.");
                        } else {
                            patient.setWhatsAppNumber(appointmentDTO.getPatient().getWhatsAppNumber());
                        }
                    }

                    // 4)Patient Validate Email ID
                    if (appointmentDTO.getPatient().getEmailId() != null) {
                        if (!appointmentDTO.getPatient().getEmailId().matches("^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$")) {
                            errors.put("Email", " Should be valid email address.");
                        } else {
                            patient.setEmailId(appointmentDTO.getPatient().getEmailId());
                        }
                    }

                    //  5)Patient Validate Gender
                    if (appointmentDTO.getPatient().getGender() != null) {
                        if (!appointmentDTO.getPatient().getGender().matches("MALE|FEMALE|OTHER")) {
                            errors.put("Gender", "Only allowed values: MALE, FEMALE, OTHER.");
                        } else {
                            patient.setGender(appointmentDTO.getPatient().getGender());
                        }
                    }
                    //  6) Validate Birth
                    if (appointmentDTO.getPatient().getDateOfBirth() != null) {
                        if (appointmentDTO.getPatient().getDateOfBirth().isAfter(LocalDate.now())) {
                            errors.put("Error", "Date of Birth Cannot in the future.");
                        } else {
                            patient.setDateOfBirth(appointmentDTO.getPatient().getDateOfBirth());
                        }
                    }

                    //  7) Validate Birth
                    if (appointmentDTO.getPatient().getBloodGroup() != null) {
                        if (!appointmentDTO.getPatient().getBloodGroup().matches("^(A|B|AB|O)[+-]$")) {
                            errors.put("Blood Group", "Must be one of A+, A-, B+, B-, AB+, AB-, O+, O-.");
                        } else {
                            patient.setBloodGroup(appointmentDTO.getPatient().getBloodGroup());
                        }
                    }

                    // Additional Fields Validation patient Status
                    if (appointmentDTO.getPatient().getStatus() < 0 || appointmentDTO.getPatient().getStatus() > 1) {
                        errors.put("Status", "Status must be either 0 or 1.");
                    } else {
                        patient.setStatus(appointmentDTO.getPatient().getStatus());
                    }

                    // Additional Fields Validation patient CurrentStatus
                    if (appointmentDTO.getPatient().getCurrentStatus() < 0 || appointmentDTO.getPatient().getCurrentStatus() > 1) {
                        errors.put("Current Status", "Current status must be either 0 or 1.");
                    } else {
                        patient.setCurrentStatus(appointmentDTO.getPatient().getCurrentStatus());
                    }

                    // Additional Fields Validation for Diet
                    if (appointmentDTO.getPatient().getDiet() != null && !appointmentDTO.getPatient().getDiet().isBlank()) {
                        if (!appointmentDTO.getPatient().getDiet().matches("^(VEG|NONVEG|VEG\\+EGGS)$")) {
                            errors.put("Diet", "Diet must be only VEG, NON-VEG, or VEG+EGGS.");
                        } else {
                            patient.setDiet(appointmentDTO.getPatient().getDiet().trim());
                        }
                    }

                    // Additional Fields Validation for Age
                    if (appointmentDTO.getPatient().getAge() != 0) {
                        if (appointmentDTO.getPatient().getAge() > 0 && appointmentDTO.getPatient().getAge() <= 120) {
                            patient.setAge(appointmentDTO.getPatient().getAge());
                        } else {
                            errors.put("Age", "Age must be between 0 and 120.");
                        }
                    }
                }

                //Below Code Appointment fields validation & update
                //  8) Validate AppointmentDateAndTime
                if (appointmentDTO.getAppointmentDateAndTime() != null) {
                    if (appointmentDTO.getAppointmentDateAndTime().isBefore(LocalDateTime.now())) {
                        errors.put("Appointment Date", "Appointment Date enter be in the future");
                    } else {
                        appointment.setAppointmentDateAndTime(appointmentDTO.getAppointmentDateAndTime());
                    }
                }

                // 9) Validate Blood Pressure (Only if provided)
                if (appointmentDTO.getBloodPressure() != null && !appointmentDTO.getBloodPressure().isEmpty()) {
                    if (!appointmentDTO.getBloodPressure().matches("\\d{2,3}/\\d{2,3}")) {
                        errors.put("Blood Pressure", "Invalid format. Must be systolic/diastolic (e.g., 120/80).");
                    } else {
                        String[] parts = appointmentDTO.getBloodPressure().split("/");
                        try {
                            int systolic = Integer.parseInt(parts[0]);
                            int diastolic = Integer.parseInt(parts[1]);

                            if (systolic < 90 || systolic > 200) {
                                errors.put("Blood Pressure", "Systolic value must be between 90 and 200.");
                            }
                            if (diastolic < 60 || diastolic > 120) {
                                errors.put("Blood Pressure", "Diastolic value must be between 60 and 120.");
                            }

                            // when all is valid, set the value
                            appointment.setBloodPressure(appointmentDTO.getBloodPressure());
                        } catch (Exception e) {
                            errors.put("Blood Pressure", "Invalid numbers for blood pressure.");
                        }
                    }
                }

                //  10) Validate Heart Rate
                if (appointmentDTO.getHeartRate() != null) {
                    if (Integer.parseInt(appointmentDTO.getHeartRate()) < 60 || Integer.parseInt(appointmentDTO.getHeartRate()) > 100) {
                        errors.put("Heart Rate", "Must be between 60 and 100 bpm");
                    } else {
                        appointment.setHeartRate(appointmentDTO.getHeartRate().getBytes()); // Set heart rate as String
                    }
                }

                //  11) Validate BodyTemperature
                if (appointmentDTO.getBodyTemperature() != 0) {
                    if (appointmentDTO.getBodyTemperature() < 95 || appointmentDTO.getBodyTemperature() > 107) {
                        errors.put("Body Temperature", "Must be between 95°F and 107°F");
                    } else {
                        appointment.setBodyTemperature(appointmentDTO.getBodyTemperature());
                    }
                }

                //  12) Validate Respiratory Rate
                if (appointmentDTO.getRespiratoryRate() != null) {
                    String respiratoryRateStr = appointmentDTO.getRespiratoryRate(); // Convert to String

                    if (Integer.parseInt(respiratoryRateStr) < 12 || Integer.parseInt(respiratoryRateStr) > 60) {
                        errors.put("Respiratory Rate", "Must be between 12 and 60 breaths per minute");
                    } else {
                        appointment.setRespiratoryRate(respiratoryRateStr.getBytes()); // Set as String
                    }
                }


                //  13) Validate Weight
                if (appointmentDTO.getWeight() != 0) {
                    if (appointmentDTO.getWeight() < 2.5 || appointmentDTO.getWeight() > 300) {
                        errors.put("Weight", "Must be between 2.5 Kg and 300 Kg.");
                    } else {
                        appointment.setWeight(appointmentDTO.getWeight());
                    }
                }

                //  14) Validate Height
                if (appointmentDTO.getHeight() != null) {
                    try {
                        int height = Integer.parseInt(appointmentDTO.getHeight());
                        if (height < 45 || height > 200) {
                            errors.put("Height", "Must be between 45 cm and 200 cm.");
                        } else {
                            appointment.setHeight(appointmentDTO.getHeight());
                        }
                    } catch (Exception e) {
                        errors.put("Height", "Must be a valid number.");
                    }
                }

                //  15) Validate PulseRate
                if (appointmentDTO.getPulseRate() != null) {
                    String pulseRateStr = appointmentDTO.getPulseRate(); // Convert to String

                    if (Integer.parseInt(pulseRateStr) < 30 || Integer.parseInt(pulseRateStr) > 250) {
                        errors.put("PulseRate", "Must be between 30 and 250.");
                    } else {
                        appointment.setPulseRate(pulseRateStr.getBytes()); // Set as String
                    }
                }


                //  16) Validate Next AppointmentDate
                if (appointmentDTO.getNextAppointmentDate() != null) {
                    if (appointmentDTO.getNextAppointmentDate().isBefore(LocalDate.now())) {
                        errors.put("Next Appointment Date", "Must be in the future.");
                    } else {
                        appointment.setNextAppointmentDate(appointmentDTO.getNextAppointmentDate());
                    }
                }

                //  17) Validate AppointmentStatus
                if (appointmentDTO.getAppointmentStatus() != null) {
                    if (appointmentDTO.getAppointmentStatus().toString().isBlank()) {
                        errors.put("Appointment Status", "Cant be empty");
                    } else if (appointmentDTO.getAppointmentStatus() == AppointmentStatus.ENGAGED || appointmentDTO.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
                        appointment.setAppointmentStatus(appointmentDTO.getAppointmentStatus());
                    } else {
                        errors.put("Appointment Status", "Invalid status update. Allowed statuses: COMPLETED or ENGAGED. ");
                    }
                }

                if (appointmentDTO.getClinicalNote() != null) {
                    appointment.setClinicalNote(appointmentDTO.getClinicalNote().getBytes());
                }

                if (appointmentDTO.getSymptoms() != null) {
                    appointment.setSymptoms(appointmentDTO.getSymptoms().getBytes());
                }

                if (appointmentDTO.getAllergies() != null) {
                    appointment.setAllergies(appointmentDTO.getAllergies().getBytes());
                }

                if (!errors.isEmpty()) {
                    return ResponseEntity.badRequest().body(errors);
                }

                //encrypt (byte)
                appointment.getPatient().setName(appointmentDTO.getPatient().getName() != null ? util.encrypt(appointmentDTO.getPatient().getName()) : null);
                appointment.getPatient().setContactNumber(appointmentDTO.getPatient().getContactNumber() != null ? util.encrypt(appointmentDTO.getPatient().getContactNumber()) : null);
                appointment.getPatient().setWhatsAppNumber(appointmentDTO.getPatient().getWhatsAppNumber() != null ? util.encrypt(appointmentDTO.getPatient().getWhatsAppNumber()) : null);
                appointment.getPatient().setEmailId(appointmentDTO.getPatient().getEmailId() != null ? util.encrypt(appointmentDTO.getPatient().getEmailId()) : null);
                appointment.getPatient().setGender(appointmentDTO.getPatient().getGender() != null ? util.encrypt(appointmentDTO.getPatient().getGender()) : null);
                appointment.getPatient().setBloodGroup(appointmentDTO.getPatient().getBloodGroup() != null ? util.encrypt(appointmentDTO.getPatient().getBloodGroup()) : null);
                appointment.getPatient().setCurrentDoctor(appointment.getCurrentDoctor());

                appointment.setHeartRate(appointmentDTO.getHeartRate() != null ? util.encrypt(appointmentDTO.getHeartRate()).getBytes() : null);
                appointment.setPulseRate(appointmentDTO.getPulseRate() != null ? util.encrypt(appointmentDTO.getPulseRate()).getBytes() : null);
                appointment.setRespiratoryRate(appointmentDTO.getRespiratoryRate() != null ? util.encrypt(appointmentDTO.getRespiratoryRate()).getBytes() : null);
                appointment.setAllergies(appointmentDTO.getAllergies() != null ? util.encrypt(appointmentDTO.getAllergies()).getBytes() : null);
                appointment.setSymptoms(appointmentDTO.getSymptoms() != null ? util.encrypt(appointmentDTO.getSymptoms()).getBytes() : null);
                appointment.setClinicalNote(appointmentDTO.getClinicalNote() != null ? util.encrypt(appointmentDTO.getClinicalNote()).getBytes() : null);
                appointment.setBloodPressure(appointmentDTO.getBloodPressure() != null ? util.encrypt(appointmentDTO.getBloodPressure()) : null);

                appointment.getPatient().setHospital(loginUser.getHospital());
                appointment.setModifiedDate(LocalDateTime.now());

                boolean isSaved = appointmentService.saveAppointment(appointment);
                if (isSaved) {

                    // After saving the appointment, decrypt the patient data for response(String)
                    appointment.getPatient().setName(appointment.getPatient().getName() != null ? util.decrypt(appointment.getPatient().getName()) : "");
                    appointment.getPatient().setContactNumber(appointment.getPatient().getContactNumber() != null ? util.decrypt(appointment.getPatient().getContactNumber()) : "");
                    appointment.getPatient().setWhatsAppNumber(appointment.getPatient().getWhatsAppNumber() != null ? util.decrypt(appointment.getPatient().getWhatsAppNumber()) : "");
                    appointment.getPatient().setEmailId(appointment.getPatient().getEmailId() != null ? util.decrypt(appointment.getPatient().getEmailId()) : "");
                    appointment.getPatient().setGender(appointment.getPatient().getGender() != null ? util.decrypt(appointment.getPatient().getGender()) : "");
                    appointment.getPatient().setBloodGroup(appointment.getPatient().getBloodGroup() != null ? util.decrypt(appointment.getPatient().getBloodGroup()) : "");

                    appointment.getPatient().setCreatedUser(appointment.getCreatedUser());
                    appointment.getPatient().setModifiedUser(appointment.getModifiedUser());
                    appointment.getPatient().setCurrentDoctor(appointment.getDoctor());
                    appointment.getPatient().setCreatedDate(appointment.getPatient().getCreatedDate());
                    appointment.getPatient().setModifiedDate(appointment.getPatient().getModifiedDate());

                    appointmentDTO.setId(appointment.getId());
                    appointmentDTO.setPatient(appointment.getPatient());
                    appointmentDTO.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                    appointmentDTO.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                    appointmentDTO.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                    appointmentDTO.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                    appointmentDTO.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                    appointmentDTO.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                    appointmentDTO.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(new String(appointment.getBloodPressure())) : "");
                    appointmentDTO.setCreatedUser(appointment.getCreatedUser());
                    appointmentDTO.setModifiedUser(appointment.getModifiedUser());
                    appointmentDTO.setCreatedDate(appointment.getCreatedDate());
                    appointmentDTO.setModifiedDate(appointment.getModifiedDate());
                    appointmentDTO.setDoctor(appointment.getDoctor());
                    appointmentDTO.setCurrentDoctor(appointment.getDoctor());

                    // Send an email notification after the appointment update
                    try {
                        emailService.sendEmail(appointment.getPatient().getEmailId(), "Appointment Update Notification", getAppointmentUpdateNotificationTemplete(appointment.getPatient().getName(), appointment.getDoctor().getName(), appointment.getHospital().getName(), appointment.getAppointmentStatus(), appointment.getAppointmentDateAndTime()));
                    } catch (Exception e) {
                        logger.error("Failed to send appointment update email: " + e.getMessage());
                    }

                    // Return the updated appointment with decrypted patient data
                    return ResponseEntity.ok(appointmentDTO);
                } else {
                    return ResponseEntity.status(500).body("Failed to update appointment.");
                }
            }

            return ResponseEntity.badRequest().body("Appointment not found.");
        } catch (Exception e) {
            logger.error("Unexpected error while UpdateAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
    // ------------------------------------------------------Cancelled Appointment-------------------------------------------------------------------------

    @DeleteMapping("/cancelAppointment/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<String> cancelAppointment(@PathVariable Long id) {

        try {
            Appointment appointment = appointmentService.findAppointmentById(id);
            if (appointment == null) {
                return ResponseEntity.status(404).body("Record not found");
            } else {
                // Update status to "CANCELLED"
                appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
                // Save cancellation
                boolean isSaved = appointmentService.saveAppointment(appointment);
                if (isSaved) {
                    emailService.sendEmail(appointment.getPatient().getEmailId(), "🚨 Your Appointment Has Been Cancelled!", getAppointmentCancellationTemplete(appointment.getPatient().getName(), appointment.getDoctor().getName(), appointment.getHospital().getName(), appointment.getAppointmentStatus(), appointment.getAppointmentDateAndTime()));
                    return ResponseEntity.ok("Appointment successfully cancelled.");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel the appointment.");
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error while CancelAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

//-------------------------------------------------Get Appointment By Id--------------------------------------------------------------------------------

    @GetMapping("/getAppointment/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','DOCTOR')")
    public synchronized ResponseEntity<?> getAppointmentById(@PathVariable long id) {
        try {
            // Fetch the appointment by ID
            Appointment app = appointmentService.findAppointmentById(id);

            // If the appointment is present, map to DTO and return
            if (app != null) {
                AppointmentDTO appDto = new AppointmentDTO();

                // Initialize the Patient object if it's null
                if (appDto.getPatient() == null) {
                    appDto.setPatient(new Patient()); // Initialize the patient object
                }

                // Safely set the patient data, decrypt the fields
                if (app.getPatient() != null) {
                    // Decrypt patient fields only if app.getPatient() is not null
                    Patient patient = app.getPatient(); // Get the patient object from appointment
                    Patient patientDto = appDto.getPatient(); // Get the patient DTO

                    // Decrypt the fields
                    patientDto.setId(patient.getId());
                    patientDto.setName(patient.getName() != null ? util.decrypt(patient.getName()) : "");
                    patientDto.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                    patientDto.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                    patientDto.setDateOfBirth(patient.getDateOfBirth());  // Assuming Date of Birth is not encrypted
                    patientDto.setEmailId(patient.getEmailId() != null ? util.decrypt(patient.getEmailId()) : "");
                    patientDto.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                    patientDto.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");

                    patientDto.setAge(patient.getAge());  // Age might not need decryption (assuming it's numeric)
                    patientDto.setCurrentDoctor(app.getDoctor());
                    patientDto.setCreatedUser(patient.getCreatedUser());
                    patientDto.setModifiedUser(patient.getModifiedUser());
                    patientDto.setModifiedDate(patient.getModifiedDate());
                    patientDto.setCreatedDate(patient.getCreatedDate());
                    patientDto.setDiet(patient.getDiet());
                } else {
                    // If app.getPatient() is null, return an error message
                    return ResponseEntity.status(404).body("Patient information not found...!");
                }

                /* appDto.setPatient(app.getPatient());*/
                appDto.setHeartRate(app.getHeartRate() != null ? util.decrypt(new String(app.getHeartRate())) : "");
                appDto.setPulseRate(app.getPulseRate() != null ? util.decrypt(new String(app.getPulseRate())) : "");
                appDto.setRespiratoryRate(app.getRespiratoryRate() != null ? util.decrypt(new String(app.getRespiratoryRate())) : "");
                appDto.setAllergies(app.getAllergies() != null ? util.decrypt(new String(app.getAllergies())) : "");
                appDto.setSymptoms(app.getSymptoms() != null ? util.decrypt(new String(app.getSymptoms())) : "");
                appDto.setClinicalNote(app.getClinicalNote() != null ? util.decrypt(new String(app.getClinicalNote())) : "");
                appDto.setBloodPressure(app.getBloodPressure() != null ? util.decrypt(new String(app.getBloodPressure())) : "");

                // Safely map symptoms, clinical notes, and allergies to strings

                // Map other fields (including null fields)
                appDto.setId(app.getId());
                appDto.setDoctor(app.getDoctor());
                appDto.setCurrentDoctor(app.getCurrentDoctor());
                appDto.setAppointmentStatus(app.getAppointmentStatus());
                appDto.setAppointmentDateAndTime(app.getAppointmentDateAndTime());
                appDto.setNextAppointmentDate(app.getNextAppointmentDate());
                appDto.setBodyTemperature(app.getBodyTemperature());
                appDto.setHeight(app.getHeight());
                appDto.setWeight(app.getWeight());
                appDto.setFetchClinicalNote(app.getFetchClinicalNote());
                appDto.setCreatedUser(app.getCreatedUser());
                appDto.setModifiedUser(app.getModifiedUser());
                appDto.setCreatedDate(app.getCreatedDate());
                appDto.setModifiedDate(app.getModifiedDate());
                appDto.setStatus(app.getStatus());

                return ResponseEntity.ok(appDto);
            }

            // If no appointment is found
            return ResponseEntity.status(404).body("Record not found...!");

        } catch (Exception e) {
            logger.error("Unexpected error while getAppointmentByID() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser().getId());
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }


//---------------------------------------------------Get All Appointment---------------------------------------------------------------------------

    @GetMapping("/getAllAppointment")
    @PreAuthorize("hasAnyAuthority('ADMIN','DOCTOR')")
    public synchronized ResponseEntity<?> getAllAppointments() {
        try {
            // Fetch the appointments list
            List<Appointment> appointments = appointmentService.findAllAppointments();

            // Check for null appointments list
            if (appointments == null || appointments.isEmpty()) {
                return ResponseEntity.status(404).body("Records not found...!");
            }

            // Create a list of AppointmentDTO objects
            List<AppointmentDTO> appointmentDTOList = new ArrayList<>();

            // Loop through the appointments and create a DTO for each
            for (Appointment appointment : appointments) {
                AppointmentDTO appDto = new AppointmentDTO();

                // Ensure the Patient object is initialized in the DTO
                if (appDto.getPatient() == null) {
                    appDto.setPatient(new Patient()); // Initialize the patient object if it's null
                }

                // Decrypted Patient Data
                if (appointment.getPatient() != null) {
                    Patient patient = appointment.getPatient();
                    Patient patientDto = appDto.getPatient(); // Get the Patient DTO

                    patientDto.setId(patient.getId());
                    patientDto.setName(appointment.getPatient().getName() != null ? util.decrypt(appointment.getPatient().getName()) : "");
                    patientDto.setContactNumber(appointment.getPatient().getContactNumber() != null ? util.decrypt(appointment.getPatient().getContactNumber()) : "");
                    patientDto.setWhatsAppNumber(appointment.getPatient().getWhatsAppNumber() != null ? util.decrypt(appointment.getPatient().getWhatsAppNumber()) : "");
                    patientDto.setDateOfBirth(appointment.getPatient().getDateOfBirth());  // Assuming Date of Birth is not encrypted
                    patientDto.setEmailId(appointment.getPatient().getEmailId() != null ? util.decrypt(appointment.getPatient().getEmailId()) : "");
                    patientDto.setGender(appointment.getPatient().getGender() != null ? util.decrypt(appointment.getPatient().getGender()) : "");
                    patientDto.setBloodGroup(appointment.getPatient().getBloodGroup() != null ? util.decrypt(appointment.getPatient().getBloodGroup()) : "");

                    patientDto.setAge(appointment.getPatient().getAge());  // Age might not need decryption (assuming it's numeric)
                    patientDto.setCurrentDoctor(appointment.getDoctor());
                    patientDto.setCreatedUser(appointment.getPatient().getCreatedUser());
                    patientDto.setModifiedUser(appointment.getPatient().getModifiedUser());
                    patientDto.setModifiedDate(appointment.getPatient().getModifiedDate());
                    patientDto.setCreatedDate(appointment.getPatient().getCreatedDate());
                    patientDto.setDiet(appointment.getPatient().getDiet()); // Assuming diet is not encrypted
                }

                appDto.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                appDto.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                appDto.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                appDto.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                appDto.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                appDto.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                appDto.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(new String(appointment.getBloodPressure())) : "");
                // Map other fields (including null fields)
                appDto.setId(appointment.getId());
                appDto.setDoctor(appointment.getDoctor());
                appDto.setCurrentDoctor(appointment.getCurrentDoctor());
                appDto.setAppointmentStatus(appointment.getAppointmentStatus());
                appDto.setAppointmentDateAndTime(appointment.getAppointmentDateAndTime());
                appDto.setNextAppointmentDate(appointment.getNextAppointmentDate());
                appDto.setBodyTemperature(appointment.getBodyTemperature());
                appDto.setHeight(appointment.getHeight());
                appDto.setWeight(appointment.getWeight());
                appDto.setFetchClinicalNote(appointment.getFetchClinicalNote());
                appDto.setCreatedUser(appointment.getCreatedUser());
                appDto.setModifiedUser(appointment.getModifiedUser());
                appDto.setCreatedDate(appointment.getCreatedDate());
                appDto.setModifiedDate(appointment.getModifiedDate());
                appDto.setStatus(appointment.getStatus());

                // Add the DTO to the list
                appointmentDTOList.add(appDto);
            }

            // Return the list wrapped in ResponseEntity
            return ResponseEntity.ok(appointmentDTOList);

        } catch (Exception e) {
            logger.error("Unexpected error while getAllAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }


    //-----------------------------------------------Get Patient Appointments------------------------------------------------------------------------------

    @GetMapping("/patient-appointments/{patientId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getAppointmentsByPatientId(@PathVariable Long patientId) {
        try {
            List<Appointment> patientAppointments = appointmentService.findAppointmentsByPatientId(patientId);

            if (patientAppointments.isEmpty()) {
                return ResponseEntity.status(404).body("Records not found...!");
            }

            // List to hold the AppointmentDTO objects
            List<AppointmentDTO> patientAppDTO = new ArrayList<>();

            // Process each appointment
            for (Appointment appointment : patientAppointments) {
                AppointmentDTO appointmentDTO = new AppointmentDTO();

                // If the patient is null, initialize it
                if (appointment.getPatient() == null) {
                    appointmentDTO.setPatient(new Patient());
                }

                // Decrypt Patient Data if patient is available
                if (appointment.getPatient() != null) {
                    Patient patient = appointment.getPatient();
                    Patient patientDto = new Patient(); // Create a new Patient DTO

                    patientDto.setId(patient.getId());
                    patientDto.setName(patient.getName() != null ? util.decrypt(patient.getName()) : "");
                    patientDto.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                    patientDto.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                    patientDto.setDateOfBirth(patient.getDateOfBirth());  // Assuming Date of Birth is not encrypted
                    patientDto.setEmailId(patient.getEmailId() != null ? util.decrypt(patient.getEmailId()) : "");
                    patientDto.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                    patientDto.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");

                    patientDto.setAge(patient.getAge());  // Age might not need decryption (assuming it's numeric)
                    patientDto.setCurrentDoctor(appointment.getDoctor());
                    patientDto.setCreatedUser(patient.getCreatedUser());
                    patientDto.setModifiedUser(patient.getModifiedUser());
                    patientDto.setModifiedDate(patient.getModifiedDate());
                    patientDto.setCreatedDate(patient.getCreatedDate());
                    patientDto.setDiet(patient.getDiet()); // Assuming diet is not encrypted

                    // Set patient DTO in the appointmentDTO
                    appointmentDTO.setPatient(patientDto);
                }

                // Decrypt other fields in the appointment object and set them in the DTO
                appointmentDTO.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                appointmentDTO.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                appointmentDTO.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                appointmentDTO.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                appointmentDTO.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                appointmentDTO.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                appointmentDTO.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(new String(appointment.getBloodPressure())) : "");

                // Map other fields (including null fields)
                appointmentDTO.setId(appointment.getId());
                appointmentDTO.setDoctor(appointment.getDoctor());
                appointmentDTO.setCurrentDoctor(appointment.getCurrentDoctor());
                appointmentDTO.setAppointmentStatus(appointment.getAppointmentStatus());
                appointmentDTO.setAppointmentDateAndTime(appointment.getAppointmentDateAndTime());
                appointmentDTO.setNextAppointmentDate(appointment.getNextAppointmentDate());
                appointmentDTO.setBodyTemperature(appointment.getBodyTemperature());
                appointmentDTO.setHeight(appointment.getHeight());
                appointmentDTO.setWeight(appointment.getWeight());
                appointmentDTO.setFetchClinicalNote(appointment.getFetchClinicalNote());
                appointmentDTO.setCreatedUser(appointment.getCreatedUser());
                appointmentDTO.setModifiedUser(appointment.getModifiedUser());
                appointmentDTO.setCreatedDate(appointment.getCreatedDate());
                appointmentDTO.setModifiedDate(appointment.getModifiedDate());
                appointmentDTO.setStatus(appointment.getStatus());

                // Add the DTO to the list
                patientAppDTO.add(appointmentDTO);
            }

            return ResponseEntity.ok(patientAppDTO);
        } catch (Exception e) {
            logger.error("Unexpected error while getAppointmentsByPatientId() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred while fetching patient appointments.");
        }
    }


// ---------------------------------- Search Appointment By Date -------------------------------------------------------------------------

    @GetMapping("/byDate/{appointmentDate}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getAppointmentsByDate(@PathVariable String appointmentDate) {
        try {
            // Try to parse the date from various formats
            LocalDate parsedDate = parseDate(appointmentDate);

            if (parsedDate == null) {
                return ResponseEntity.status(400).body("Invalid date format. Please use one of the following formats: yyyy-MM-dd, dd-MM-yyyy, d MMM yyyy, or d MMMM yyyy.");
            }

            // Fetch appointments for the parsed date
            List<Appointment> appointments = appointmentService.findAppointmentsByDate(parsedDate);

            if (appointments.isEmpty()) {
                return ResponseEntity.status(404).body("No appointments found for this date.");
            }

            // List to hold the AppointmentDTO objects
            List<AppointmentDTO> appointmentDTOs = new ArrayList<>();

            // Process each appointment
            for (Appointment appointment : appointments) {
                AppointmentDTO appointmentDTO = new AppointmentDTO();

                // If the patient is null, initialize it
                if (appointment.getPatient() == null) {
                    appointmentDTO.setPatient(new Patient());
                }

                // Decrypt Patient Data if patient is available
                if (appointment.getPatient() != null) {
                    Patient patient = appointment.getPatient();
                    Patient patientDto = new Patient(); // Create a new Patient DTO

                    patientDto.setId(patient.getId());
                    patientDto.setName(patient.getName() != null ? util.decrypt(patient.getName()) : "");
                    patientDto.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                    patientDto.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                    patientDto.setDateOfBirth(patient.getDateOfBirth());  // Assuming Date of Birth is not encrypted
                    patientDto.setEmailId(patient.getEmailId() != null ? util.decrypt(patient.getEmailId()) : "");
                    patientDto.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                    patientDto.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");

                    patientDto.setAge(patient.getAge());  // Age doesn't need decryption
                    patientDto.setCurrentDoctor(appointment.getDoctor());
                    patientDto.setCreatedUser(patient.getCreatedUser());
                    patientDto.setModifiedUser(patient.getModifiedUser());
                    patientDto.setModifiedDate(patient.getModifiedDate());
                    patientDto.setCreatedDate(patient.getCreatedDate());
                    patientDto.setDiet(patient.getDiet()); // Assuming diet is not encrypted

                    // Set patient DTO in the appointmentDTO
                    appointmentDTO.setPatient(patientDto);
                }

                // Decrypt other fields in the appointment object and set them in the DTO
                appointmentDTO.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                appointmentDTO.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                appointmentDTO.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                appointmentDTO.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                appointmentDTO.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                appointmentDTO.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                appointmentDTO.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(new String(appointment.getBloodPressure())) : "");

                // Map other fields (including null fields)
                appointmentDTO.setId(appointment.getId());
                appointmentDTO.setDoctor(appointment.getDoctor());
                appointmentDTO.setCurrentDoctor(appointment.getCurrentDoctor());
                appointmentDTO.setAppointmentStatus(appointment.getAppointmentStatus());
                appointmentDTO.setAppointmentDateAndTime(appointment.getAppointmentDateAndTime());
                appointmentDTO.setNextAppointmentDate(appointment.getNextAppointmentDate());
                appointmentDTO.setBodyTemperature(appointment.getBodyTemperature());
                appointmentDTO.setHeight(appointment.getHeight());
                appointmentDTO.setWeight(appointment.getWeight());
                appointmentDTO.setFetchClinicalNote(appointment.getFetchClinicalNote());
                appointmentDTO.setCreatedUser(appointment.getCreatedUser());
                appointmentDTO.setModifiedUser(appointment.getModifiedUser());
                appointmentDTO.setCreatedDate(appointment.getCreatedDate());
                appointmentDTO.setModifiedDate(appointment.getModifiedDate());
                appointmentDTO.setStatus(appointment.getStatus());

                // Add the DTO to the list
                appointmentDTOs.add(appointmentDTO);
            }

            return ResponseEntity.ok(appointmentDTOs);
        } catch (Exception e) {
            logger.error("Unexpected error while getAppointmentsByDate() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred while fetching appointments.");
        }
    }

    private LocalDate parseDate(String dateStr) {
        // Normalize month part to ensure it's correctly capitalized
        dateStr = normalizeMonth(dateStr);

        // Supported formats
        List<String> supportedFormats = Arrays.asList(
                "yyyy-MM-dd",       // e.g., 2025-02-08
                "dd-MM-yyyy",       // e.g., 08-02-2025
                "d MMM yyyy",       // e.g., 6 Feb 2025
                "d MMMM yyyy"       // e.g., 6 February 2025
        );

        for (String pattern : supportedFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                return LocalDate.parse(dateStr, formatter);  // Try parsing using each pattern
            } catch (Exception e) {
                // If parsing fails, try the next format
                System.out.println("Failed to parse date: " + dateStr + " using pattern: " + pattern);
            }
        }

        return null;  // Return null if no format matches
    }

    private String normalizeMonth(String dateStr) {
        String[] dateParts = dateStr.split(" ");
        if (dateParts.length >= 2) {
            dateParts[1] = dateParts[1].substring(0, 1).toUpperCase() + dateParts[1].substring(1).toLowerCase();
            return String.join(" ", dateParts);
        }
        return dateStr;  // Return the original string if no month part found
    }

    //------------------------------------------- Delete Soft Appointment -------------------------------------------------------

    @DeleteMapping("/deleteByID/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> deleteAppointment(@PathVariable Long id) {
        try {
            Appointment appointment = appointmentService.findAppointmentById(id);
            if (appointment == null) {
                return ResponseEntity.status(404).body("Records not found");
            } else {
                appointment.setStatus(0);
                boolean isSaved = appointmentService.saveAppointment(appointment);
                if (isSaved) {
                    return ResponseEntity.ok("Appointment successfully deleted.");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete the appointment.");
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error while deleteAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    //    -----------------------------------Branch Wise getAllAppointment----------------------------------------------------------------------------

    @GetMapping("/branchwise-appointments/{branch}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR','ADMIN')")
    public ResponseEntity<?> getAppointmentsByBranch(@PathVariable String branch) {
        try {
            User user = userService.getAuthenticateUser();
            List<Appointment> appointments = appointmentService.getAppointmentsByBranch(branch);

            if (appointments.isEmpty()) {
                return ResponseEntity.status(404).body("Records not found");
            }

            List<AppointmentDTO> appointmentDTOList = new ArrayList<>();

            // Loop through the appointments and create a DTO for each
            for (Appointment appointment : appointments) {
                AppointmentDTO appDto = new AppointmentDTO();


                // decrypt(String)
                appointment.getPatient().setName(appointment.getPatient().getName() != null ? util.decrypt(appointment.getPatient().getName()) : "");
                appointment.getPatient().setContactNumber(appointment.getPatient().getContactNumber() != null ? util.decrypt(appointment.getPatient().getContactNumber()) : "");
                appointment.getPatient().setWhatsAppNumber(appointment.getPatient().getWhatsAppNumber() != null ? util.decrypt(appointment.getPatient().getWhatsAppNumber()) : "");
                appointment.getPatient().setEmailId(appointment.getPatient().getEmailId() != null ? util.decrypt(appointment.getPatient().getEmailId()) : "");
                appointment.getPatient().setGender(appointment.getPatient().getGender() != null ? util.decrypt(appointment.getPatient().getGender()) : "");
                appointment.getPatient().setBloodGroup(appointment.getPatient().getBloodGroup() != null ? util.decrypt(appointment.getPatient().getBloodGroup()) : "");
                appointment.getPatient().setCurrentDoctor(appointment.getDoctor());  // Assuming currentDoctor is not encrypted



                // decrypt(byte)
                appDto.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                appDto.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                appDto.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                appDto.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                appDto.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                appDto.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                appDto.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(new String(appointment.getBloodPressure())) : "");

                appDto.setId(appointment.getId());
                appDto.setPatient(appointment.getPatient());
                appDto.setDoctor(appointment.getDoctor());
                appDto.setAppointmentStatus(appointment.getAppointmentStatus());
                appDto.setAppointmentDateAndTime(appointment.getAppointmentDateAndTime());
                appDto.setNextAppointmentDate(appointment.getNextAppointmentDate());
                appDto.setBodyTemperature(appointment.getBodyTemperature());
                appDto.setHeight(appointment.getHeight());
                appDto.setWeight(appointment.getWeight());
                appDto.setFetchClinicalNote(appointment.getFetchClinicalNote());
                appDto.setCreatedUser(appointment.getCreatedUser());
                appDto.setModifiedUser(appointment.getModifiedUser());
                appDto.setCreatedDate(appointment.getCreatedDate());
                appDto.setModifiedDate(appointment.getModifiedDate());
                appDto.setStatus(appointment.getStatus());
                appDto.setCurrentDoctor(appointment.getCurrentDoctor());

                appointmentDTOList.add(appDto);  // Add the DTO to the list

            }
            return ResponseEntity.ok(appointmentDTOList);
        } catch (Exception e) {
            logger.error("Unexpected error while getAppointmentsByBranch() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
//---------------------------------------Missed Appointments--------------------------------------------------------------------

    @PostConstruct
    public void sendEmailToPatientMissedAppointments() {
        new Thread(() -> {
            while (true) {
                try {

                    List<Appointment> scheduledAppointments = appointmentService.findByAppointmentStatus(AppointmentStatus.SCHEDULED);
                    List<Appointment> missedAppointments = scheduledAppointments.stream().filter(P -> P.getAppointmentDateAndTime().isBefore(LocalDateTime.now().minusMinutes(5)) && P.getMissedMailStatus() == 0).collect(Collectors.toList());

                    for (Appointment app : missedAppointments) {
                        app.setAppointmentStatus(AppointmentStatus.MISSED);

                        emailService.sendEmail(app.getPatient().getEmailId(), "🚨 Appointment Missed Notification", getAppointmentMissingTemplate(app.getPatient().getName(), app.getDoctor().getName(), app.getHospital().getName(), app.getAppointmentStatus(), app.getAppointmentDateAndTime()));

                        app.setMissedMailStatus(1);
                        appointmentService.saveAppointment(app);

                    }
                    Thread.sleep(120000); // Sleep for 2 minutes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    //    ------------------------------------------ Reminder Appointments one day before--------------------------------------------------------------

    @PostConstruct
    public void sendEmailToPatientReminderMail() {
        new Thread(() -> {
            while (true) {
                try {
                    List<Appointment> scheduledAppointments = appointmentService.findByAppointmentStatus(AppointmentStatus.SCHEDULED);
                    List<Appointment> upcomingAppointments = scheduledAppointments.stream().filter(P -> P.getAppointmentDateAndTime().isBefore(LocalDateTime.now().plusMinutes(5)) && P.getReminderMailStatus() == 0).collect(Collectors.toList());

                    for (Appointment app : upcomingAppointments) {

                        emailService.sendEmail(app.getPatient().getEmailId(), "🚨Reminder Appointment Notification", sendEmailToPatientReminderMail(app.getPatient().getName(), app.getDoctor().getName(), app.getHospital().getName(), app.getAppointmentStatus(), app.getAppointmentDateAndTime()));

                        app.setReminderMailStatus(1);
                        appointmentService.saveAppointment(app);

                    }
                    Thread.sleep(120000);// Sleep for 2 minutes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
// ********************************** Email Templates **************************************************************************

    // Book Appointment Mail
    public String getAppointmentConfirmationTemplete(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);
        return
                "<p>🎉 <b>Mark Your Calendar – You’re Booked for Wellness!</b></p>" +
                        "<p>Hey <b>" + patientName + "</b>,</p>" +
                        "<p>You’re officially scheduled for a date with better health!</p>" +
                        "<p>👨‍⚕️ <b>Doctor:</b> " + doctorName + "<br/>" +
                        "🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
                        "📅 <b>Date:</b> " + date + "<br/>" +
                        "⏰ <b>Time:</b> " + time + "</p>" +
                        "<p>💡 <i>“Invest in your health today, because you deserve nothing less than the best!”</i></p>" +
                        "<p>✨ We’re excited to make your visit a comfortable and memorable one. Got questions? We’re all ears!</p>" +
                        "<p>Stay awesome,<br><b>" + hospitalName + "</b></p>";
    }

    // Update Appointment Mail
    public String getAppointmentUpdateNotificationTemplete(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);
        return
                "<p>🎉 <b>Hi <b>" + patientName + "</b>,</b></p>" +
                        "<p>We wanted to let you know that your appointment has been successfully updated!</p>" +
                        "<p>👨‍⚕️ <b>Doctor:</b> Dr. " + doctorName + "<br/>" +
                        "🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
                        "📅 <b>Updated Date:</b> " + date + "<br/>" +
                        "⏰ <b>Time:</b> " + time + "</p>" +
                        "<p>🔄 <i>Your health matters to us, and we're here to ensure you're always taken care of!</i></p>" +
                        "<p>✨ If you have any questions or need further assistance, don’t hesitate to reach out. We're always here for you!</p>" +
                        "<p>Thank you for choosing Medica Healthcare. We can’t wait to see you!</p>" +
                        "<p>Stay awesome,<br><b>" + hospitalName + "</b></p>";
    }

    // Appointment Cancellation Mail
    public String getAppointmentCancellationTemplete(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);
        return
                "<p>🚨 <b>Hi <b>" + patientName + "</b>,</b></p>" +
                        "<p>We wanted to inform you that your appointment has been cancelled.</p>" +
                        "<p>👨‍⚕️ <b>Doctor:</b> Dr. " + doctorName + "<br/>" +
                        "🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
                        "📅 <b>Date:</b> " + date + "<br/>" +
                        "⏰ <b>Time:</b> " + time + "</p>" +
                        "<p>🔄 <i>We sincerely apologize for any inconvenience caused. Your health is our top priority, and we’re here to help you reschedule at your convenience.</i></p>" +
                        "<p>✨ If you have any questions or need further assistance, don’t hesitate to contact us. We’re always here to support you!</p>" +
                        "<p>Thank you for understanding. We look forward to assisting you again soon!</p>" +
                        "<p>Stay well,<br><b>" + hospitalName + "</b></p>";
    }

    // Missed Appointment Mail
    public String getAppointmentMissingTemplate(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);
        return
                "<p>🚨 <b>Hi <b>" + patientName + "</b>,</b></p>" +
                        "<p>We noticed that you <b>" + appointmentStatus + "</b> your appointment.</p>" +
                        "<p>👨‍⚕️ <b>Doctor:</b> Dr. " + doctorName + "<br/>" +
                        "🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
                        "📅 <b>Original Date:</b> " + date + "<br/>" +
                        "⏰ <b>Time:</b> " + time + "</p>" +
                        "<p>🔄 <i>Your health is important to us, and we kindly ask you to reschedule your appointment at the earliest convenience. Our team is here to assist you in finding a suitable date and time.</i></p>" +
                        "<p>✨ If you have any questions or need assistance, please don’t hesitate to contact us. We’re always happy to help!</p>" +
                        "<p>Thank you for your prompt attention. We look forward to serving you soon!</p>" +
                        "<p>Stay well,<br><b>Medica Healthcare Team</b></p>";

    }

    // Appointment Reminder Mail
    public String sendEmailToPatientReminderMail(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);
        return
                "<p>🚨 <b>Hi " + patientName + "</b>,</p>" +
                        "<p>This is a friendly reminder that you have an upcoming appointment scheduled.</p>" +
                        "<p>👨‍⚕️ <b>Doctor:</b> " + doctorName + "<br/>" +
                        "🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
                        "📅 <b>Scheduled Date:</b> " + date + "<br/>" +
                        "⏰ <b>Time:</b> " + time + "</p>" +
                        "<p>🔔 <i>We want to ensure you're fully prepared for your appointment, so please make sure to arrive on time.</i></p>" +
                        "<p>✨ If you need to reschedule or have any questions about your appointment, please don’t hesitate to reach out to us. We’re happy to assist!</p>" +
                        "<p>We look forward to seeing you soon and assisting with your healthcare needs.</p>" +
                        "<p>Stay healthy,<br><b>Medica Healthcare Team</b></p>";
    }

// Encryption & Decryption Methods only for Appointment Purpose

/*
public Appointment getEncryptAppointment(AppointmentDTO appointmentDTO){
        Appointment appointment = new Appointment();
        Patient patient = new Patient();
    appointment.getPatient().setName(appointmentDTO.getPatient().getName() != null ? util.encrypt(appointmentDTO.getPatient().getName()) : null);
    appointment.getPatient().setContactNumber(appointmentDTO.getPatient().getContactNumber() != null ? util.encrypt(appointmentDTO.getPatient().getContactNumber()) : null);
    appointment.getPatient().setWhatsAppNumber(appointmentDTO.getPatient().getWhatsAppNumber() != null ? util.encrypt(appointmentDTO.getPatient().getWhatsAppNumber()) : null);
    appointment.getPatient().setEmailId(appointmentDTO.getPatient().getEmailId() != null ? util.encrypt(appointmentDTO.getPatient().getEmailId()) : null);
    appointment.getPatient().setGender(appointmentDTO.getPatient().getGender() != null ? util.encrypt(appointmentDTO.getPatient().getGender()) : null);
    appointment.getPatient().setBloodGroup(appointmentDTO.getPatient().getBloodGroup() != null ? util.encrypt(appointmentDTO.getPatient().getBloodGroup()) : null);

    appointment.setHeartRate(appointmentDTO.getHeartRate() != null ? util.encrypt(appointmentDTO.getHeartRate()).getBytes() : null);
    appointment.setPulseRate(appointmentDTO.getPulseRate() != null ? util.encrypt(appointmentDTO.getPulseRate()).getBytes() : null);
    appointment.setRespiratoryRate(appointmentDTO.getRespiratoryRate() != null ? util.encrypt(appointmentDTO.getRespiratoryRate()).getBytes() : null);
    appointment.setAllergies(appointmentDTO.getAllergies() != null ? util.encrypt(appointmentDTO.getAllergies()).getBytes() : null);
    appointment.setSymptoms(appointmentDTO.getSymptoms() != null ? util.encrypt(appointmentDTO.getSymptoms()).getBytes() : null);
    appointment.setClinicalNote(appointmentDTO.getClinicalNote() != null ? util.encrypt(appointmentDTO.getClinicalNote()).getBytes() : null);
    appointment.setBloodPressure(appointmentDTO.getBloodPressure() != null ? util.encrypt(appointmentDTO.getBloodPressure()) : null);

    return appointment;
}
*/


}