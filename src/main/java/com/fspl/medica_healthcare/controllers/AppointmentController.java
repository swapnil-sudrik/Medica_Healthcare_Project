package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.AppointmentDTO;
import com.fspl.medica_healthcare.dtos.PatientDTO;
import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.annotation.PostConstruct;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
    private LeaveService leaveService;

    @Autowired
    private EncryptionUtil util;

    @Autowired
    private SettingsService settingsService;


//    ------------------------------------------------------- Book Appointment----------------------------------------------

    @PostMapping("/bookAppointment")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> bookAppointment(@RequestBody AppointmentDTO appointmentDTO) {

        try {
            //Get authenticated user
            User loginUser = userService.getAuthenticateUser();
            Map<String, String> res = new HashMap<>();

            if (appointmentDTO.getDoctor() == null) {
                res.put("Error", "Please select a doctor for appointment booking.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            //Check the dr is exist or not by Id
            User doctor = userService.getUserById(appointmentDTO.getDoctor().getId(), loginUser);
            if (doctor == null || doctor.getRoles() == null || !doctor.getRoles().equals("DOCTOR")) {
                return ResponseEntity.status(404).body("This is not doctor");
            }

            String doctorBranch = new String(doctor.getBranch());
            String loginUserBranch = new String(loginUser.getBranch());
            if (!doctorBranch.equalsIgnoreCase(loginUserBranch)) {
                return ResponseEntity.status(400).body("Doctor not found...!");
            }

            Patient patient = new Patient();
            PatientDTO patientDTO = appointmentDTO.getPatientDTO();
            if(patientDTO == null) return ResponseEntity.status(400).body("Patient information is required...!");

            // Name Validation
            if (patientDTO.getName() == null || patientDTO.getName().trim().isEmpty() || patientDTO.getName().trim().length() < 2) {
                res.put("PatientName: ", patientDTO.getName() == null || patientDTO.getName().trim().isEmpty()
                        ? "Patient name is required...!"
                        : "Patient Name at least 2 characters eg.Om");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            } else {
                if (!patientDTO.getName().trim().matches("^[a-zA-Z\\s]*$")) {
                    res.put("PatientName: ", "Patient Name can only contain letters and spaces (no numbers or special symbol)");
                } else {
                    String finalName = patientDTO.getName().replaceAll("\\s+", " ").replaceAll("[\\n\\r\\t]", "").trim().toUpperCase();
                    patient.setName(util.encrypt(finalName).getBytes(StandardCharsets.UTF_8));
                }
            }

            // Contact number Validation
            String contactNumber = patientDTO.getContactNumber().trim();
            if (contactNumber == null || contactNumber.trim().isEmpty()) {
                res.put("ContactNumber: ", "Contact number is required...!");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }
            if (contactNumber.matches("^0[1-9][0-9]{9}$")) {
                patient.setContactNumber(util.encrypt(contactNumber));
            } else if (contactNumber.matches("^[1-9][0-9]{9}$")) {
                patient.setContactNumber(util.encrypt(contactNumber));
            } else {
                res.put("Contactnumber: ", "Invalid contact number. Enter a 10-digit mobile (no leading zero) or an 11-digits landline (starting with zero)...!");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // Whatsapp number validation
            if (patientDTO.getWhatsAppNumber() == null || patientDTO.getWhatsAppNumber().isEmpty() || !patientDTO.getWhatsAppNumber().matches("^[1-9][0-9]{9}$")) {
                res.put("WhatsAppNumber: ", (patientDTO.getWhatsAppNumber() == null || patientDTO.getWhatsAppNumber().isEmpty()) ? "Whats App number must be required" : "Whatsapp number must be exactly 10 digits long, contain only numbers, and not start with zero.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }
            String whatsappNumber = patientDTO.getWhatsAppNumber().trim();
            patient.setWhatsAppNumber(util.encrypt(whatsappNumber));

            // Email Validation
            String emailRegex = "^(?!.*\\.\\.)(?![._%+-])[a-zA-Z0-9._%+-]{1,50}(?<![._%+-])@(?:(?:gmail|yahoo|outlook|hotmail|protonmail|icloud)\\.[a-zA-Z]{2,10}|[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+)$";
            if (patientDTO.getEmailId() == null || patientDTO.getEmailId().isEmpty() || !patientDTO.getEmailId().trim().matches(emailRegex)) {
                res.put("EmailId", patientDTO.getEmailId() == null || patientDTO.getEmailId().isEmpty() ? "Email is required" : "Email should be valid");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            } else {
                String validateEmail = patientDTO.getEmailId().trim().replaceAll("\\s+", " ").replaceAll("[\\n\\r\\t]", "").toLowerCase();
                patient.setEmailId(util.encrypt(validateEmail).getBytes(StandardCharsets.UTF_8));
            }

            // Gender Validation
            if (patientDTO.getGender() == null || patientDTO.getGender().trim().isEmpty()) {
                res.put("Gender", "Gender selection is mandatory.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            String gender = patientDTO.getGender().trim().toUpperCase();
            if (!(gender.equalsIgnoreCase("MALE") || gender.equalsIgnoreCase("FEMALE") || gender.equalsIgnoreCase("OTHER"))) {
                res.put("Gender: ", "Only 'MALE', 'FEMALE', and 'OTHER' are allowed as valid gender options.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }
            patient.setGender(util.encrypt(gender));

            // Dob validation
            String dateOfBirth = patientDTO.getDateOfBirth().trim();
            LocalDate dob = parseDate(dateOfBirth);
            if (dob == null || patientDTO.getDateOfBirth().isEmpty() || dob.isAfter(LocalDate.now()) || dob.isBefore(LocalDate.now().minusYears(122))) {
                res.put("DateOfBirth: ", dob == null || patientDTO.getDateOfBirth().isEmpty()
                        ? "Date of birth is required...!"
                        : dob.isAfter(LocalDate.now())
                        ? "Date of birth must be in past...!"
                        : "Date of birth not more than 122 years ago...!");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }
            patient.setDateOfBirth(util.encrypt(dob + ""));

            // setting Age
            Period period = Period.between(dob, LocalDate.now());
            int yearAge = period.getYears();
            String strAge = util.encrypt(yearAge + "");
            patient.setAge(strAge);

            // Blood group validation
            String bloodGroup = patientDTO.getBloodGroup().trim().toUpperCase();
            if (patientDTO.getBloodGroup() == null || patientDTO.getBloodGroup().trim().isEmpty()) {
                res.put("BloodGroup: ","Blood group must be required");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }
            if(!(bloodGroup.equalsIgnoreCase("A+") || bloodGroup.equalsIgnoreCase("A-") || bloodGroup.equalsIgnoreCase("AB+")
                    || bloodGroup.equalsIgnoreCase("AB-") || bloodGroup.equalsIgnoreCase("B+") || bloodGroup.equalsIgnoreCase("B-")
                    || bloodGroup.equalsIgnoreCase("O+") || bloodGroup.equalsIgnoreCase("O-"))){
                res.put("BloodGroup: ","Blood group must be one of the following: A+, A-, B+, B-, AB+, AB-, O+, O-");
                return new ResponseEntity<>(res,HttpStatus.BAD_REQUEST);
            }
            patient.setBloodGroup(util.encrypt(bloodGroup));


            if(appointmentDTO.getAppointmentStatus() == null || String.valueOf(appointmentDTO.getAppointmentStatus()).isEmpty() || appointmentDTO.getAppointmentStatus() != AppointmentStatus.SCHEDULED){
                res.put("AppointmentStatus: ", appointmentDTO.getAppointmentStatus() == null || String.valueOf(appointmentDTO.getAppointmentStatus()).isEmpty()
                        ? "Appointment Status is required...!"
                        : "Only Scheduled is allowed at booking...!");
                return new ResponseEntity<>(res,HttpStatus.BAD_REQUEST);
            }

            // AppointmentDateTime Validation
            if (appointmentDTO.getAppointmentDateAndTime() == null || appointmentDTO.getAppointmentDateAndTime().isEmpty()) {
                res.put("AppointmentDateAndTime: ", "Appointment date and time must be required.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            LocalDateTime appointmentDateTime = parseDateTime(appointmentDTO.getAppointmentDateAndTime().trim());

            if (appointmentDateTime == null) {
                res.put("AppointmentDateAndTime: ", "Invalid date format. Supported formats: yyyy-MM-dd'T'HH:mm:ss and many more.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            if (appointmentDateTime.isBefore(LocalDateTime.now()) || appointmentDateTime.isAfter(LocalDateTime.now().plusMonths(2))) {
                res.put("AppointmentDateAndTime: ", appointmentDateTime.isBefore(LocalDateTime.now())
                        ? "Appointment date and time must be in the present or future."
                        : "Appointment date and time not more than 2 months");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            if (!appointmentService.isAppointmentNotOnWeekOffDays(appointmentDateTime,loginUser.getHospital().getId())) {
                res.put("Error", "Hospital is closed on weekends. Please select a weekday.");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }

            // Check if the patient already exists based on name, email, and contact number
            Patient existingPatient = patientService.findPatientByDetails(patient.getName(), patient.getEmailId(), patient.getContactNumber());
            if (existingPatient != null) {
                patient = existingPatient; // Use existing patient
            } else {
                patient = patientService.savePatient(patient);
            }

            // Doctor Availability Functionality
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime appDateTime = parseDateTime(appointmentDTO.getAppointmentDateAndTime());
            LocalDate appointmentDate = appDateTime.toLocalDate();
            LocalDateTime startTime = appDateTime.minusMinutes(15);
            LocalDateTime endTime = appDateTime.plusMinutes(15);
            List<Appointment> doctorsAppointments = appointmentService.findByDoctor_Id(appointmentDTO.getDoctor().getId());
            for (Appointment existingAppointment : doctorsAppointments) {
                try {
                    if (existingAppointment.getAppointmentDateAndTime() != null) {
                        String decryptedTime = util.decrypt(existingAppointment.getAppointmentDateAndTime());
                        LocalDateTime time = LocalDateTime.parse(decryptedTime, formatter);
                        if (!time.isBefore(startTime) && !time.isAfter(endTime)) {
                            res.put("Error", "Doctor not available as they have a scheduled appointment.");
                            return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to check doctor's appointment: " + ExceptionUtils.getStackTrace(e));
                }
            }

            // Doctor Leaves Checking Functionality
            boolean checkDoctorsLeave = leaveService.isDoctorOnLeave(appointmentDTO.getDoctor().getId(), appointmentDate);
            if (checkDoctorsLeave) {
                res.put("Error", "Doctor is on leave. Please select another date and time");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }


            Appointment appointment = new Appointment();
            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setHospital(loginUser.getHospital());
            appointment.setAppointmentDateAndTime(util.encrypt(String.valueOf(appointmentDateTime)));
            appointment.setAppointmentStatus(AppointmentStatus.SCHEDULED);
            appointment.setCreatedUser(loginUser);
            appointment.setModifiedUser(loginUser);
            appointment.setCreatedDate(LocalDate.now());
            appointment.setStatus(1);

            // Save the appointment
            boolean appointmentSaved = appointmentService.saveAppointment(appointment);
            if (appointmentSaved) {
                // Send confirmation email
                try {
                    // emailService.sendEmail(util.decrypt(new String(appointment.getPatient().getEmailId())), "\ud83c\udf89 Mark Your Calendar – You’re Booked for Wellness!", getAppointmentConfirmationTemplate(util.decrypt(new String(appointment.getPatient().getName())), appointment.getDoctor().getName(), appointment.getHospital().getName(), appointment.getAppointmentStatus(),appointmentDateTime));
                } catch (Exception emailException) {
                    logger.error("Failed to send confirmation email: " + emailException.getMessage());
                }
                AppointmentDTO appointmentDTO1 = appointmentService.appointmentToAppointmentDto(appointment);
                return ResponseEntity.status(HttpStatus.CREATED).body(appointmentDTO1);
            } else {
                return ResponseEntity.status(500).body("Failed to Book appointment.");
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error("Unexpected error while BookAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser().getId() + "Request Body: " + appointmentDTO);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

// ---------------------------------- Search Appointment By Date -------------------------------------------------------------------------

    @GetMapping("/getAppointmentsbyDate/{appointmentDate}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getAppointmentsByDate(@PathVariable String appointmentDate) {
        try {
            // Try to parse the date from various formats
            LocalDate parsedDate = parseDate(appointmentDate);

            if (parsedDate == null) {
                return ResponseEntity.status(400).body("Invalid date format. Please enter valid format...!");
            }

            // Fetch appointments for the parsed date
            List<Appointment> appointments = appointmentService.findAppointmentsByDate(parsedDate);

            if (appointments.isEmpty()) {
                return ResponseEntity.status(404).body("No appointments found for this date.");
            }

            List<AppointmentDTO> appointmentDTOList = new ArrayList<>();

            for (Appointment appointment : appointments) {
                AppointmentDTO appointmentDto = new AppointmentDTO();
                appointmentDto.setPatientDTO(patientToPatientDto(appointment.getPatient()));
                appointmentDto.setId(appointment.getId());
                appointmentDto.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                appointmentDto.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                appointmentDto.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                appointmentDto.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                appointmentDto.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                appointmentDto.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                appointmentDto.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(appointment.getBloodPressure()) : "");
                appointmentDto.setDoctor(appointment.getDoctor());
                appointmentDto.setCurrentDoctor(appointment.getCurrentDoctor());
                appointmentDto.setAppointmentStatus(appointment.getAppointmentStatus());
                appointmentDto.setAppointmentDateAndTime(util.decrypt(appointment.getAppointmentDateAndTime()));
                appointmentDto.setNextAppointmentDate(appointment.getNextAppointmentDate());
                appointmentDto.setBodyTemperature(appointment.getBodyTemperature());
                appointmentDto.setHeight(appointment.getHeight());
                appointmentDto.setWeight(appointment.getWeight());
                appointmentDto.setFetchClinicalNote(appointment.getFetchClinicalNote());
                appointmentDto.setCreatedUser(appointment.getCreatedUser());
                appointmentDto.setModifiedUser(appointment.getModifiedUser());
                appointmentDto.setCreatedDate(appointment.getCreatedDate());
                appointmentDto.setModifiedDate(appointment.getModifiedDate());
                appointmentDto.setStatus(appointment.getStatus());

                appointmentDTOList.add(appointmentDto);
            }

            return ResponseEntity.ok(appointmentDTOList);
        } catch (Exception e) {
            logger.error("Unexpected error while getAppointmentsByDate() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User: " + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred while fetching appointments.");
        }
    }
//------------------------------------------- Get Appointments By PatientID -------------------------------------------------------

    @GetMapping("/getAppointmentsbyPatientId/{patientId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','DOCTOR','RECEPTIONIST')")
    public ResponseEntity<?> getAppointmentsByPatientId(@PathVariable Long patientId) {
        User loginUser = userService.getAuthenticateUser();

        try {
            // 1. Extract user role and hospital info
            Hospital hospital = loginUser.getHospital();
            Long hospitalId = (hospital != null) ? hospital.getId() : null;
            String role = loginUser.getRoles() != null ? loginUser.getRoles().toUpperCase() : "";

            // 2. Extract and clean branches
            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            if (hospitalId == null || role.isEmpty()) {
                return ResponseEntity.badRequest().body("User's hospital or role information is missing.");
            }

            // 3. Fetch appointments by patient and hospital
            List<Appointment> patientAppointments = appointmentService.findByPatientIdAndHospitalId(patientId, hospitalId);

            if (patientAppointments == null || patientAppointments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No appointments found for this patient.");
            }

            // 4. Role-based access control for RECEPTIONIST
            if ("RECEPTIONIST".equals(role)) {
                boolean hasBranchAccess = patientAppointments.stream().anyMatch(app -> {
                    String appBranch = app.getCreatedUser() != null && app.getCreatedUser().getBranch() != null
                            ? new String(app.getCreatedUser().getBranch()).trim()
                            : null;
                    return branchList.stream().anyMatch(branch -> branch.equalsIgnoreCase(appBranch));
                });

                if (!hasBranchAccess) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Access denied: appointments not found for your branch.");
                }
            }

            // 5. Check if all appointments are inactive
            boolean allInactive = patientAppointments.stream().allMatch(app -> app.getStatus() == 0);
            if (allInactive) {
                return ResponseEntity.status(404).body("All appointments are inactive.");
            }

            // 6. Convert to AppointmentDTOs
            List<AppointmentDTO> appointmentDTOs = new ArrayList<>();

            for (Appointment app : patientAppointments) {
                AppointmentDTO appDto = new AppointmentDTO();
                Patient patient = app.getPatient();

                appDto.setId(app.getId());
                appDto.setPatientDTO(patientToPatientDto(patient)); // Use your existing method
                appDto.setAppointmentDateAndTime(app.getAppointmentDateAndTime() != null ? util.decrypt(app.getAppointmentDateAndTime()) : "");
                appDto.setNextAppointmentDate(app.getNextAppointmentDate() != null ? util.decrypt(app.getNextAppointmentDate()) : "");
                appDto.setHeartRate(app.getHeartRate() != null ? util.decrypt(new String(app.getHeartRate())) : "");
                appDto.setPulseRate(app.getPulseRate() != null ? util.decrypt(new String(app.getPulseRate())) : "");
                appDto.setRespiratoryRate(app.getRespiratoryRate() != null ? util.decrypt(new String(app.getRespiratoryRate())) : "");
                appDto.setAllergies(app.getAllergies() != null ? util.decrypt(new String(app.getAllergies())) : "");
                appDto.setSymptoms(app.getSymptoms() != null ? util.decrypt(new String(app.getSymptoms())) : "");
                appDto.setClinicalNote(app.getClinicalNote() != null ? util.decrypt(new String(app.getClinicalNote())) : "");
                appDto.setBloodPressure(app.getBloodPressure() != null ? util.decrypt(app.getBloodPressure()) : "");
                appDto.setDoctor(app.getDoctor());
                appDto.setCurrentDoctor(app.getCurrentDoctor());
                appDto.setAppointmentStatus(app.getAppointmentStatus());
                appDto.setBodyTemperature(app.getBodyTemperature());
                appDto.setHeight(app.getHeight());
                appDto.setWeight(app.getWeight());
                appDto.setFetchClinicalNote(app.getFetchClinicalNote());
                appDto.setCreatedUser(app.getCreatedUser());
                appDto.setModifiedUser(app.getModifiedUser());
                appDto.setCreatedDate(app.getCreatedDate());
                appDto.setModifiedDate(app.getModifiedDate());
                appDto.setStatus(app.getStatus());
                appointmentDTOs.add(appDto);
            }
            return ResponseEntity.ok(appointmentDTOs);

        } catch (Exception e) {
            logger.error("Error in getAppointmentsByPatientId(): " + ExceptionUtils.getStackTrace(e) +
                    " | User: " + (loginUser != null ? loginUser.getId() : "Unauthenticated"));
            return ResponseEntity.status(500).body("Unexpected error occurred: " + e.getMessage());
        }
    }

    //-------------------------------------------------Update Appointment---------------------------------------------------------------------

    @PutMapping("/updateAppointmentbyId/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<?> updateAppointment(@PathVariable long id, @RequestBody AppointmentDTO appointmentDTO) {

        try {

            //Get authenticated user
            User loginUser = userService.getAuthenticateUser();

            // Find the appointment by id
            Appointment appointment = appointmentService.findAppointmentById(id);
            if (appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
                return ResponseEntity.status(409).body("Appointment already completed, Cannot update again.!");
            }

            if (appointment.getAppointmentStatus().toString().isEmpty() || appointment.getAppointmentStatus() == null) {
                return ResponseEntity.status(400).body("Appointment status is required");
            }

            if (appointment != null) {
                // check Appointment found then proceed with update
                Map<String, String> res = new HashMap<>();
                Patient patient = appointment.getPatient();

                // Patient Data validation
                if (appointmentDTO.getPatientDTO() != null) {

                    // 1) Patient Name validation
                    if (appointmentDTO.getPatientDTO().getName() != null) {
                        if (!appointmentDTO.getPatientDTO().getName().trim().isEmpty()) {
                            if (!appointmentDTO.getPatientDTO().getName().trim().matches("^[a-zA-Z\\s]*$")) {
                                res.put("PatientName: ", "Patient Name can only contain letters and spaces (no numbers or special symbol)");
                            } else {
                                patient.setName(util.encrypt(appointmentDTO.getPatientDTO().getName().trim().toUpperCase()).getBytes());
                            }
                        }
                    }

                    // 2) Patient Contact Number validation
                    if (appointmentDTO.getPatientDTO().getContactNumber() != null) {
                        if (!appointmentDTO.getPatientDTO().getContactNumber().trim().isEmpty()) {
                            if (!appointmentDTO.getPatientDTO().getContactNumber().matches("[0-9]+")) {
                                res.put("ContactNumber: ", "Contact Number should only contain digits (0-9) without spaces or special characters.");
                            } else if (appointmentDTO.getPatientDTO().getContactNumber().startsWith("0")) {
                                res.put("ContactNumber: ", "Contact Number should not start with 0.");
                            } else if (!appointmentDTO.getPatientDTO().getContactNumber().matches("^[1-9][0-9]{9}$")) {
                                res.put("ContactNumber: ", "Must be exactly 10 digits.");
                            } else {
                                patient.setContactNumber(util.encrypt(appointmentDTO.getPatientDTO().getContactNumber()));
                            }
                        }
                    }

                    // 3) Patient WhatsApp Number validation
                    if (appointmentDTO.getPatientDTO().getWhatsAppNumber() != null) {
                        if (!appointmentDTO.getPatientDTO().getWhatsAppNumber().trim().isEmpty() && !appointmentDTO.getPatientDTO().getWhatsAppNumber().isBlank()) {
                            if (!appointmentDTO.getPatientDTO().getWhatsAppNumber().matches("[0-9]+")) {
                                res.put("WhatsAppNumber: ", "WhatsApp Number should only contain digits (0-9) without spaces or special characters.");
                            } else if (appointmentDTO.getPatientDTO().getWhatsAppNumber().startsWith("0")) {
                                res.put("WhatsAppNumber: ", "WhatsApp Number should not start with 0.");
                            } else if (!appointmentDTO.getPatientDTO().getWhatsAppNumber().matches("^[1-9][0-9]{9}$")) {
                                res.put("WhatsAppNumber: ", "WhatsApp Number must be exactly 10 digits.");
                            } else {
                                patient.setWhatsAppNumber(util.encrypt(appointmentDTO.getPatientDTO().getWhatsAppNumber()));
                            }
                        } else {
                            appointment.getPatient().setWhatsAppNumber(appointment.getPatient().getWhatsAppNumber());
                        }
                    }

                    // 4) Patient Email ID validation
                    if (appointmentDTO.getPatientDTO().getEmailId() != null) {
                        if (!appointmentDTO.getPatientDTO().getEmailId().trim().matches("^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$")) {
                            res.put("EmailId: ", "Should be a valid email address.");
                        } else if (!appointmentDTO.getPatientDTO().getEmailId().trim().matches("^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.(com|in|gov|COM)$")) {
                            res.put("EmailId: ", "Email domain must end with .com or .in and .gov");
                        } else {
                            patient.setEmailId(util.encrypt(appointmentDTO.getPatientDTO().getEmailId().trim().toLowerCase()).getBytes());
                        }
                    }

                    //  5) Patient Gender validation
                    if (appointmentDTO.getPatientDTO().getGender() != null) {
                        if (!appointmentDTO.getPatientDTO().getGender().matches("MALE|FEMALE|OTHER")) {
                            res.put("Gender: ", "Gender Only allowed values: MALE, FEMALE, OTHER.");
                        } else {
                            patient.setGender(appointmentDTO.getPatientDTO().getGender());
                        }
                    }

                    // 6) Patient Birth Date validation and age calculation
                    String dobInput = appointmentDTO.getPatientDTO().getDateOfBirth();
                    if (dobInput != null && !dobInput.trim().isEmpty()) {
                        LocalDate dob = parseDate(dobInput); // Use your flexible parsing method

                        if (dob == null) {
                            res.put("DateOfBirth: ", "Invalid date format for Date of Birth.");
                        } else if (dob.isAfter(LocalDate.now())) {
                            res.put("DateOfBirth: ", "Date of Birth cannot be in the future.");
                        } else if (dob.isBefore(LocalDate.now().minusYears(120))) {
                            res.put("DateOfBirth: ", "Date of Birth cannot be more than 120 years old.");
                        } else {
                            // Store dob in LocalDate ISO format: yyyy-MM-dd
                            patient.setDateOfBirth(util.encrypt(dob.toString()));

                            // Calculate and set age
                            int age = Period.between(dob, LocalDate.now()).getYears();
                            appointment.getPatient().setAge(util.encrypt(String.valueOf(age)));
                        }
                    }


                    //  7) Patient Blood Group validation
                    if (appointmentDTO.getPatientDTO().getBloodGroup() != null) {
                        if (!appointmentDTO.getPatientDTO().getBloodGroup().matches("^(A|B|AB|O)[+-]$")) {
                            res.put("BloodGroup", "Blood Group Must be one of A+, A-, B+, B-, AB+, AB-, O+, O-.");
                        } else {
                            patient.setBloodGroup(appointmentDTO.getPatientDTO().getBloodGroup());
                        }
                    }


                    if (appointmentDTO.getPatientDTO().getStatus() != null) {
                        if (appointmentDTO.getPatientDTO().getStatus() < 0 || appointmentDTO.getPatientDTO().getStatus() > 1) {
                            res.put("Status: ", "Status must be either 0 or 1.");
                        } else {
                            patient.setStatus(appointmentDTO.getPatientDTO().getStatus());
                        }
                    } else {
                        patient.setStatus(appointment.getPatient().getStatus());
                    }



                    if (appointmentDTO.getPatientDTO().getCurrentStatus() != null) {
                        if (appointmentDTO.getPatientDTO().getCurrentStatus() < 0 || appointmentDTO.getPatientDTO().getCurrentStatus() > 1) {
                            res.put("CurrentStatus: ", "Current status must be either 0 or 1.");
                        } else {
                            patient.setCurrentStatus(appointmentDTO.getPatientDTO().getCurrentStatus());
                        }
                    } else {
                        patient.setCurrentStatus(appointment.getPatient().getCurrentStatus());
                    }


                    //  10) Patient Diet validation
                    if (appointmentDTO.getPatientDTO().getDiet() != null && !appointmentDTO.getPatientDTO().getDiet().isBlank()) {
                        if (!appointmentDTO.getPatientDTO().getDiet().matches("^(VEG|NONVEG|VEG\\+EGGS)$")) {
                            res.put("Diet: ", "Diet must be only VEG, NON-VEG, or VEG+EGGS.");
                        } else {
                            patient.setDiet(appointmentDTO.getPatientDTO().getDiet().trim());
                        }
                    }


                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found in you request");
                }
                // Below Code Appointment fields validation
                // 12) Blood Pressure validation
                if (appointmentDTO.getBloodPressure() != null && !appointmentDTO.getBloodPressure().isEmpty()) {
                    if (!appointmentDTO.getBloodPressure().matches("\\d{2,3}/\\d{2,3}")) {
                        res.put("BloodPressure: ", "Blood Pressure Invalid format. Must be systolic/diastolic (e.g., 120/80).");
                    } else {
                        String[] parts = appointmentDTO.getBloodPressure().split("/");
                        try {
                            int systolic = Integer.parseInt(parts[0]);
                            int diastolic = Integer.parseInt(parts[1]);

                            if (systolic < 90 || systolic > 200) {
                                res.put("BloodPressure: ", "Systolic value must be between 90 and 200.");
                            }
                            if (diastolic < 60 || diastolic > 120) {
                                res.put("BloodPressure: ", "Diastolic value must be between 60 and 120.");
                            }

                            // when all is valid, set the value
                            appointment.setBloodPressure(appointmentDTO.getBloodPressure());
                        } catch (Exception e) {
                            res.put("BloodPressure: ", "Invalid numbers for blood pressure.");
                        }
                    }
                }

                // 13) Heart Rate validation
                if (appointmentDTO.getHeartRate() != null && !appointmentDTO.getHeartRate().trim().isEmpty()) {
                    if (!appointmentDTO.getHeartRate().trim().matches("\\d+")) {
                        res.put("HeartRate: ", "Heart Rate should contain only digits. Special characters are not allowed.");
                    } else {
                        try {
                            int heartRateValue = Integer.parseInt(appointmentDTO.getHeartRate().trim());
                            if (heartRateValue < 60 || heartRateValue > 100) {
                                res.put("HeartRate: ", "Heart Rate Must be between 60 and 100 bpm");
                            } else {
                                appointment.setHeartRate(appointmentDTO.getHeartRate().trim().getBytes()); // Convert String to byte[]
                            }
                        } catch (NumberFormatException e) {
                            res.put("HeartRate: ", "Heart Rate must be a valid numeric value.");
                        }
                    }
                }

                //  14) BodyTemperature validation
                if (appointmentDTO.getBodyTemperature() != null) {
                    if (!String.valueOf(appointmentDTO.getBodyTemperature()).matches("^\\d+(\\.\\d+)?$")) {
                        res.put("BodyTemperature: ", "Body Temperature must be a valid number");
                    } else if (appointmentDTO.getBodyTemperature() < 95 || appointmentDTO.getBodyTemperature() > 107) {
                        res.put("BodyTemperature: ", "Body Temperature must be between 95°F and 107°F");
                    } else {
                        appointment.setBodyTemperature(appointmentDTO.getBodyTemperature());
                    }
                }


                //  15) Respiratory Rate validation
                if (appointmentDTO.getRespiratoryRate() != null && !appointmentDTO.getRespiratoryRate().isEmpty()) {
                    if (appointmentDTO.getRespiratoryRate().matches(".[\\$%^&()_+=!@#<>?/|{}\\[\\]\\-].*")) {
                        res.put("RespiratoryRate: ", "Respiratory Rate only contains digits, special characters are not allowed.");
                    } else {
                        try {
                            int respiratoryRateValue = Integer.parseInt(appointmentDTO.getRespiratoryRate());
                            if (respiratoryRateValue < 12 || respiratoryRateValue > 60) {
                                res.put("RespiratoryRate: ", "Respiratory Rate must be between 12 and 60 breaths per minute");
                            } else {
                                appointment.setRespiratoryRate(appointmentDTO.getRespiratoryRate().getBytes());  // Convert String to byte[]
                            }
                        } catch (NumberFormatException e) {
                            res.put("RespiratoryRate: ", "Respiratory Rate must be a valid numeric value.");
                        }
                    }
                }

                //  16) Weight validation
                if (appointmentDTO.getWeight() != null) {
                    if (appointmentDTO.getWeight() < 2.5 || appointmentDTO.getWeight() > 300) {
                        res.put("Weight: ", "Weight must be between 2.5 Kg and 300 Kg.");
                    } else {
                        appointment.setWeight(appointmentDTO.getWeight());
                    }
                }

                //  17) Height validation
                if (appointmentDTO.getHeight() != null) {
                    if (!appointmentDTO.getHeight().trim().isEmpty()) {
                        try {
                            int height = Integer.parseInt(appointmentDTO.getHeight());
                            if (height < 45 || height > 200) {
                                res.put("Height: ", "Height must be between 45 cm and 200 cm.");
                            } else {
                                appointment.setHeight(appointmentDTO.getHeight());
                            }
                        } catch (Exception e) {
                            res.put("Height: ", "Height must be a valid number.");
                        }
                    }
                }

                //  18) PulseRate validation
                if (appointmentDTO.getPulseRate() != null && !appointmentDTO.getPulseRate().isEmpty()) {
                    if (appointmentDTO.getPulseRate().matches(".[\\$%^&()_+=!@#<>?/|{}\\[\\]\\-].*")) {
                        res.put("PulseRate: ", "PulseRate only contains digits, special characters are not allowed.");
                    } else {
                        try {
                            int pulseRateValue = Integer.parseInt(appointmentDTO.getPulseRate());
                            if (pulseRateValue < 30 || pulseRateValue > 250) {
                                res.put("PulseRate: ", "PulseRate must be between 30 and 250.");
                            } else {
                                appointment.setPulseRate(appointmentDTO.getPulseRate().getBytes()); // Convert String to byte[]
                            }
                        } catch (NumberFormatException e) {
                            res.put("PulseRate: ", "PulseRate must be a valid numeric value.");
                        }
                    }
                }

                //  19) Next AppointmentDate validation

                LocalDateTime dateTime = LocalDateTime.parse(util.decrypt(appointment.getAppointmentDateAndTime()));
                LocalDate appointmentDate = dateTime.toLocalDate();
                if (appointmentDTO.getNextAppointmentDate() != null && !appointmentDTO.getNextAppointmentDate().trim().isEmpty()) {

                    LocalDate nextAppointmentDate = LocalDate.parse(appointmentDTO.getNextAppointmentDate());

                    if (nextAppointmentDate.isBefore(LocalDate.now()) || nextAppointmentDate.isAfter(LocalDate.now().plusYears(1))) {
                        res.put("NextAppointmentDate: ", nextAppointmentDate.isBefore(LocalDate.now()) ? "Next Appointment Date Must be in the future." : "Next Appointment Date Must be in under 1 Years");
                    } else if (nextAppointmentDate.isBefore(appointmentDate)) {
                        res.put("NextAppointmentDate: ", "Next Appointment Date must be After Appointment Date");
                    } else {
                        appointment.setNextAppointmentDate(util.encrypt(appointmentDTO.getNextAppointmentDate()));
                    }
                }

                //  20) AppointmentStatus validation
                if (appointmentDTO.getAppointmentStatus() != null) {
                    if (appointmentDTO.getAppointmentStatus().toString().isEmpty()) {
                        res.put("Appointment Status", "Can't be empty");
                    } else if (appointmentDTO.getAppointmentStatus() == AppointmentStatus.ENGAGED || appointmentDTO.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
                        appointment.setAppointmentStatus(appointmentDTO.getAppointmentStatus());
                    } else {
                        res.put("Appointment Status", "Invalid status update. Allowed statuses: COMPLETED or ENGAGED. ");
                    }
                } else {
                    appointment.setAppointmentStatus(appointment.getAppointmentStatus());
                }

                // 21) Fetch ClinicalNote validation
                if (appointmentDTO.getFetchClinicalNote() != null) {
                    if (appointmentDTO.getFetchClinicalNote() < 0 || appointmentDTO.getFetchClinicalNote() > 1) {
                        res.put("FetchClinicalNote: ", "Fetch ClinicalNote must be either 0 or 1.");
                    } else {
                        appointment.setFetchClinicalNote(appointmentDTO.getFetchClinicalNote());
                    }
                }


                // 22) ClinicalNote validation
                if (appointmentDTO.getClinicalNote() != null && !appointmentDTO.getClinicalNote().isEmpty()) {
                    if (!appointmentDTO.getClinicalNote().matches("^[a-zA-Z,\\.\\s]+$")) {
                        res.put("ClinicalNote: ", "ClinicalNote must contain only letters, spaces, and commas and dots.");
                    } else {
                        appointment.setClinicalNote(appointmentDTO.getClinicalNote().getBytes());
                    }
                }

                // 23) Symptoms validation
                if (appointmentDTO.getSymptoms() != null && !appointmentDTO.getSymptoms().isEmpty()) {
                    if (!appointmentDTO.getSymptoms().matches("^[a-zA-Z,\\.\\s]+$")) {
                        res.put("Symptoms: ", "Symptoms must contain only letters, spaces, and commas and dots.");
                    } else {
                        appointment.setSymptoms(appointmentDTO.getSymptoms().getBytes());
                    }
                }

                // 24) Allergies validation
                if (appointmentDTO.getAllergies() != null && !appointmentDTO.getAllergies().isEmpty()) {
                    if (!appointmentDTO.getAllergies().matches("^[a-zA-Z,\\.\\s]+$")) {
                        res.put("Allergies: ", "Allergies must contain only letters, spaces, and commas and dots.");
                    } else {
                        appointment.setAllergies(appointmentDTO.getAllergies().getBytes());
                    }
                }

                // Appointment Date and time set
                if (appointmentDTO.getAppointmentDateAndTime() != null) {
                    appointment.setAppointmentDateAndTime(appointment.getAppointmentDateAndTime());
                }

                // Check if there are any validation errors
                if (!res.isEmpty()) {
                    return ResponseEntity.badRequest().body(res);
                }

                // Encrypt and set patient data
                if (appointmentDTO.getPatientDTO() != null) {

                /*    if (appointmentDTO.getPatient().getWhatsAppNumber() != null) {
                        appointment.getPatient().setWhatsAppNumber(util.encrypt(appointmentDTO.getPatient().getWhatsAppNumber()));
                    }*/
                    if (appointmentDTO.getPatientDTO().getBloodGroup() != null) {
                        appointment.getPatient().setBloodGroup(util.encrypt(appointmentDTO.getPatientDTO().getBloodGroup()));
                    }

                    if (appointmentDTO.getPatientDTO().getGender() != null) {
                        appointment.getPatient().setGender(util.encrypt(appointmentDTO.getPatientDTO().getGender()));
                    }

              /*      if (appointmentDTO.getPatient().getDateOfBirth() != null) {
                        appointment.getPatient().setDateOfBirth(util.encrypt(appointmentDTO.getPatient().getDateOfBirth()));
                    }*/

                    if (appointmentDTO.getPatientDTO().getBloodGroup() != null) {
                        appointment.getPatient().setBloodGroup(util.encrypt(appointmentDTO.getPatientDTO().getBloodGroup()));
                    }
                    appointment.getPatient().setCurrentDoctor(appointment.getCurrentDoctor());// Patients Set CurrentDoctor
                }

                // Encrypt and set appointment data
                if (appointmentDTO.getHeartRate() != null) {
                    appointment.setHeartRate(util.encrypt(appointmentDTO.getHeartRate()).getBytes());
                }
                if (appointmentDTO.getPulseRate() != null) {
                    appointment.setPulseRate(util.encrypt(appointmentDTO.getPulseRate()).getBytes());
                }
                if (appointmentDTO.getRespiratoryRate() != null) {
                    appointment.setRespiratoryRate(util.encrypt(appointmentDTO.getRespiratoryRate()).getBytes());
                }
                if (appointmentDTO.getAllergies() != null) {
                    appointment.setAllergies(util.encrypt(appointmentDTO.getAllergies()).getBytes());
                }
                if (appointmentDTO.getSymptoms() != null) {
                    appointment.setSymptoms(util.encrypt(appointmentDTO.getSymptoms()).getBytes());
                }
                if (appointmentDTO.getClinicalNote() != null) {
                    appointment.setClinicalNote(util.encrypt(appointmentDTO.getClinicalNote()).getBytes());
                }
                if (appointmentDTO.getBloodPressure() != null) {
                    appointment.setBloodPressure(util.encrypt(appointmentDTO.getBloodPressure()));
                }

                appointment.getPatient().setCreatedUser(appointment.getCreatedUser());
                appointment.getPatient().setModifiedUser(appointment.getModifiedUser());
                appointment.setCurrentDoctor(appointment.getDoctor()); // Appointments Set CurrentDoctor
                appointment.getPatient().setCurrentDoctor(appointment.getCurrentDoctor()); // Patients Set CurrentDoctor
                appointment.getPatient().setCreatedDate(appointment.getPatient().getCreatedDate());
                appointment.getPatient().setModifiedDate(appointment.getPatient().getModifiedDate());
                appointment.getPatient().setHospital(loginUser.getHospital());
                appointment.setModifiedDate(LocalDate.now());


                // Automatically age Calculation
                if (appointmentDTO.getPatientDTO().getDateOfBirth() != null) {
                    if (!appointmentDTO.getPatientDTO().getDateOfBirth().trim().isEmpty()) {
                        LocalDate DOB = parseDate(appointmentDTO.getPatientDTO().getDateOfBirth());
                        if (DOB != null) {
                            int age = Period.between(DOB, LocalDate.now()).getYears();
                            appointment.getPatient().setAge(util.encrypt(String.valueOf(age)));
                        }
                    }
                }

                // Save the appointment
                boolean isSaved = appointmentService.saveAppointment(appointment);
                if (isSaved) {
                    // Send an email notification after the appointment update
                    try {
                        //emailService.sendEmail(util.decrypt(new String(appointment.getPatient().getEmailId())), "Appointment Update Notification", getAppointmentUpdateNotificationTemplate(util.decrypt(new String(appointment.getPatient().getName())), appointment.getDoctor().getName(), appointment.getHospital().getName(), appointment.getAppointmentStatus(), dateTime));
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to send appointment update email: " + e.getMessage());
                    }


                    // Set appointment DTO for response
                    appointmentDTO.setId(appointment.getId());
                    appointmentDTO.setPatientDTO(patientToPatientDto(appointment.getPatient()));
                    appointmentDTO.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                    appointmentDTO.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                    appointmentDTO.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                    appointmentDTO.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                    appointmentDTO.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                    appointmentDTO.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                    appointmentDTO.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(appointment.getBloodPressure()) : "");

                    if (appointment.getAppointmentDateAndTime() != null) {
                        appointmentDTO.setAppointmentDateAndTime(util.decrypt(appointment.getAppointmentDateAndTime()));
                    }

                    if (appointment.getNextAppointmentDate() != null) {
                        appointmentDTO.setNextAppointmentDate(util.decrypt(appointment.getNextAppointmentDate()));
                    }
                    //   appointmentDTO.setNextAppointmentDate(util.decrypt(appointment.getNextAppointmentDate()));
                    appointmentDTO.setCreatedUser(appointment.getCreatedUser());
                    appointmentDTO.setModifiedUser(appointment.getModifiedUser());
                    appointmentDTO.setCreatedDate(appointment.getCreatedDate());
                    appointmentDTO.setModifiedDate(appointment.getModifiedDate());
                    appointmentDTO.setDoctor(appointment.getDoctor()); // Set Doctor
                    appointmentDTO.setCurrentDoctor(appointment.getDoctor()); // Set Current Doctor
                    appointmentDTO.setFetchClinicalNote(appointment.getFetchClinicalNote());
                    appointmentDTO.setStatus(appointment.getStatus());


                    appointmentDTO.setHeight(appointment.getHeight());
                    appointmentDTO.setWeight(appointment.getWeight());
                    appointmentDTO.setAppointmentStatus(appointment.getAppointmentStatus());
                    appointmentDTO.setFetchClinicalNote(appointment.getFetchClinicalNote());
                    appointmentDTO.setNextAppointmentDate(appointment.getNextAppointmentDate());
                    appointmentDTO.setBodyTemperature(appointment.getBodyTemperature());

                    // Return the updated appointment details with decrypted patient data
                    return ResponseEntity.ok(appointmentDTO);
                } else {
                    return ResponseEntity.status(500).body("Failed to update appointment.");
                }
            } else {
                // If the appointment is not found, return a response
                return ResponseEntity.status(404).body("Appointment not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unexpected error while UpdateAppointment() :" + ExceptionUtils.getStackTrace(e) + "LoggedIn User" + userService.getAuthenticateUser() + "Request Body: " + appointmentDTO);
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    // ------------------------------------------------------Cancelled Appointment-------------------------------------------------------------------------

    @DeleteMapping("/cancelAppointmentbyId/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<String> cancelAppointment(@PathVariable Long id) {
        try {
            User loginUser = userService.getAuthenticateUser();

            // Fetch the appointment by ID
            Appointment appointment = appointmentService.findAppointmentById(id);
            if (appointment == null) {
                return ResponseEntity.status(404).body("Record not found");
            }

            // Check if the logged-in user is the creator
            if (loginUser.getId() != appointment.getCreatedUser().getId()) {
                return ResponseEntity.status(403).body("Message: Do not have permission to access this resource...!");
            }

            // Status checks
            if (appointment.getStatus() == 0) {
                return ResponseEntity.status(404).body("Records not found...!");
            }

            if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
                return ResponseEntity.status(400).body("Appointment Already Cancelled");
            }

            if (appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED
                    || appointment.getAppointmentStatus() == AppointmentStatus.ENGAGED) {
                return ResponseEntity.status(400).body("You Cannot cancel COMPLETED or ENGAGED appointments");
            }

            // Decrypt and parse appointment date/time
            String decryptedDateTime = util.decrypt(appointment.getAppointmentDateAndTime());
            LocalDateTime appointmentDateTime = LocalDateTime.parse(decryptedDateTime); // ISO-8601 parsing

            // Cancel the appointment
            appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
            boolean isSaved = appointmentService.saveAppointment(appointment);

            if (isSaved) {
                // Decrypt patient's email and name from byte[]
                String decryptedEmail = util.decrypt(new String(appointment.getPatient().getEmailId()));
                String decryptedName = util.decrypt(new String(appointment.getPatient().getName()));

                // Send email notification
              /*  emailService.sendEmail(
                        decryptedEmail,
                        "🚨 Your Appointment Has Been Cancelled!",
                        getAppointmentCancellationTemplate(
                                decryptedName,
                                appointment.getDoctor().getName(),
                                appointment.getHospital().getName(),
                                appointment.getAppointmentStatus(),
                                appointmentDateTime
                        )
                );*/

                return ResponseEntity.ok("Appointment successfully cancelled.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel the appointment.");
            }

        } catch (Exception e) {
            logger.error("Unexpected error while cancelAppointment() : " + ExceptionUtils.getStackTrace(e)
                    + " LoggedIn User: " + userService.getAuthenticateUser());
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }


//-------------------------------------------------Get Appointment By Id--------------------------------------------------------------------------------

    @GetMapping("/getAppointmentbyId/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','DOCTOR','RECEPTIONIST')")
    public ResponseEntity<?> getAppointmentById(@PathVariable long id) {
        User loginUser = userService.getAuthenticateUser();

        try {
            // 1. Extract user role and hospital info
            Hospital hospital = loginUser.getHospital();
            Long hospitalId = (hospital != null) ? hospital.getId() : null;
            String role = loginUser.getRoles() != null ? loginUser.getRoles().toUpperCase() : "";

            // 2. Extract and clean branches
            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            if (hospitalId == null || role.isEmpty()) {
                return ResponseEntity.badRequest().body("User's hospital or role information is missing.");
            }

            // 3. Fetch appointment by ID and hospital
            Appointment app = appointmentService.findAppointmentByIdAndHospital(id, hospitalId);
            if (app == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found.");
            }

            // 4. Access control for RECEPTIONIST
            if ("RECEPTIONIST".equals(role)) {
                byte[] appBranchBytes = app.getCreatedUser().getBranch();
                String appBranch = appBranchBytes != null ? new String(appBranchBytes).trim() : null;

                boolean hasAccess = branchList.stream()
                        .anyMatch(branch -> branch.equalsIgnoreCase(appBranch));

                if (!hasAccess) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Access denied: appointment not found for your branch.");
                }
            }

            // 5. Check appointment status
            if (app.getStatus() == 0) {
                return ResponseEntity.status(404).body("Appointment record is inactive.");
            }

            // 6. Build DTOs
            AppointmentDTO appDto = new AppointmentDTO();
            PatientDTO patientDto = new PatientDTO();
            Patient patient = app.getPatient();

            // 7. Set AppointmentDTO
            appDto.setPatientDTO(patientToPatientDto(patient));//used method

            appDto.setAppointmentDateAndTime(app.getAppointmentDateAndTime() != null ? util.decrypt(app.getAppointmentDateAndTime()) : "");
            appDto.setNextAppointmentDate(app.getNextAppointmentDate() != null ? util.decrypt(app.getNextAppointmentDate()) : "");
            appDto.setHeartRate(app.getHeartRate() != null ? util.decrypt(new String(app.getHeartRate())) : "");
            appDto.setPulseRate(app.getPulseRate() != null ? util.decrypt(new String(app.getPulseRate())) : "");
            appDto.setRespiratoryRate(app.getRespiratoryRate() != null ? util.decrypt(new String(app.getRespiratoryRate())) : "");
            appDto.setAllergies(app.getAllergies() != null ? util.decrypt(new String(app.getAllergies())) : "");
            appDto.setSymptoms(app.getSymptoms() != null ? util.decrypt(new String(app.getSymptoms())) : "");
            appDto.setClinicalNote(app.getClinicalNote() != null ? util.decrypt(new String(app.getClinicalNote())) : "");
            appDto.setBloodPressure(app.getBloodPressure() != null ? util.decrypt(app.getBloodPressure()) : "");

            appDto.setId(app.getId());
            appDto.setDoctor(app.getDoctor());
            appDto.setCurrentDoctor(app.getCurrentDoctor());
            appDto.setAppointmentStatus(app.getAppointmentStatus());
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

        } catch (Exception e) {
            logger.error("Error in getAppointmentById(): " + ExceptionUtils.getStackTrace(e) +
                    " | User: " + (loginUser != null ? loginUser.getId() : "Unauthenticated"));
            return ResponseEntity.status(500).body("Unexpected error occurred: " + e.getMessage());
        }
    }


//---------------------------------------------------Get All Appointment---------------------------------------------------------------------------

    @GetMapping("/getAllAppointments")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<?> getAllAppointments() {

        User loginUser = userService.getAuthenticateUser();

        try {
            // 1. Get role and hospital
            String role = loginUser.getRoles() != null ? loginUser.getRoles().toUpperCase() : "";
            boolean isReceptionist = "RECEPTIONIST".equals(role);
            boolean isBranchAdmin = "ADMIN".equals(role);
            boolean isDoctor = "DOCTOR".equals(role);

            Hospital hospital = loginUser.getHospital();
            Long hospitalId = (hospital != null) ? hospital.getId() : null;

            if (hospitalId == null || role.isEmpty()) {
                return ResponseEntity.badRequest().body("User's hospital or role information is missing.");
            }

            // 2. Get receptionist's branches
            List<String> branchList;
            if (isReceptionist) {
                branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                        .map(String::trim)
                        .toList();
            } else {
                branchList = new ArrayList<>();
            }

            // 3. Fetch all appointments
            List<Appointment> appointments = appointmentService.findAllAppointments();
            if (appointments == null || appointments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Records not found...!");
            }

            // 4. Filter appointments
            List<Appointment> filteredAppointments = appointments.stream()
                    .filter(a -> a.getStatus() != 0)
                    .filter(a -> a.getHospital() != null && Objects.equals(a.getHospital().getId(), hospitalId))
                    .filter(a -> {
                        if (isReceptionist) {
                            String createdBranch = a.getCreatedUser() != null && a.getCreatedUser().getBranch() != null
                                    ? new String(a.getCreatedUser().getBranch()).trim()
                                    : "";
                            return branchList.contains(createdBranch);
                        }
                        return true; // Admin and Doctor: see all branches under hospital
                    })
                    .collect(Collectors.toList());

            // 5. Group by branch
            Map<String, List<AppointmentDTO>> branchWiseAppointments = new HashMap<>();

            for (Appointment appointment : filteredAppointments) {
                String branch = appointment.getCreatedUser() != null && appointment.getCreatedUser().getBranch() != null
                        ? new String(appointment.getCreatedUser().getBranch()).trim()
                        : "";

                AppointmentDTO appDto = new AppointmentDTO();

                appDto.setPatientDTO(patientToPatientDto(appointment.getPatient()));
                appDto.setId(appointment.getId());
                appDto.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                appDto.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                appDto.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                appDto.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                appDto.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                appDto.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                appDto.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(appointment.getBloodPressure()) : "");
                appDto.setDoctor(appointment.getDoctor());
                appDto.setCurrentDoctor(appointment.getCurrentDoctor());
                appDto.setAppointmentStatus(appointment.getAppointmentStatus());
                appDto.setAppointmentDateAndTime(util.decrypt(appointment.getAppointmentDateAndTime()));
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
                branchWiseAppointments.computeIfAbsent(branch, k -> new ArrayList<>()).add(appDto);
            }

            return ResponseEntity.ok(branchWiseAppointments);

        } catch (Exception e) {
            logger.error("Unexpected error in getAllAppointments(): " + ExceptionUtils.getStackTrace(e)
                    + " | User: " + (loginUser != null ? loginUser.getId() : "null"));
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }


    //------------------------------------------- Delete Soft Appointment -------------------------------------------------------

    @DeleteMapping("/deleteAppointmentbyId/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> deleteAppointment(@PathVariable Long id) {
        try {
            //find the appointment by its id
            Appointment appointment = appointmentService.findAppointmentById(id);
            if (appointment == null) {
                return ResponseEntity.status(404).body("Records not found");
            } else {
                //change status to 0
                appointment.setStatus(0);

                //save the updated appointment status
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
    @GetMapping("/getAppointmentsbyBranch")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR', 'ADMIN')")
    public synchronized ResponseEntity<?> getAppointmentsByBranch() {
        User loginUser = null;

        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return ResponseEntity.badRequest().body("Something went Wrong");
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();
            List<Appointment> appointments = new ArrayList<>();
            for (String branch : branchList) {
                List<Appointment> AllAppoinments = appointmentService.getAppointmentsByBranch(branch.getBytes(), loginUser.getHospital().getId());
                appointments.addAll(AllAppoinments);
            }

            if (appointments == null || appointments.isEmpty()) {
                return ResponseEntity.status(404).body("Records not found");
            }

            List<AppointmentDTO> appointmentDTOList = new ArrayList<>();

            // Loop through the appointments and create a DTO for each
            for (Appointment appointment : appointments) {
                AppointmentDTO appointmentDto = new AppointmentDTO();
                Patient patient = appointment.getPatient();
//                PatientDTO patientDto = new PatientDTO();
                // Map other appointment fields
                appointmentDto.setId(appointment.getId());
                appointmentDto.setPatientDTO(patientToPatientDto(patient));
                appointmentDto.setDoctor(appointment.getDoctor());
                appointmentDto.setCurrentDoctor(appointment.getCurrentDoctor());
                appointmentDto.setAppointmentStatus(appointment.getAppointmentStatus());
                appointmentDto.setBodyTemperature(appointment.getBodyTemperature());
                appointmentDto.setHeight(appointment.getHeight());
                appointmentDto.setWeight(appointment.getWeight());
                appointmentDto.setFetchClinicalNote(appointment.getFetchClinicalNote());
                appointmentDto.setCreatedUser(appointment.getCreatedUser());
                appointmentDto.setModifiedUser(appointment.getModifiedUser());
                appointmentDto.setCreatedDate(appointment.getCreatedDate());
                appointmentDto.setModifiedDate(appointment.getModifiedDate());
                appointmentDto.setStatus(appointment.getStatus());
                appointmentDto.setAppointmentDateAndTime(appointment.getAppointmentDateAndTime() != null ? util.decrypt(appointment.getAppointmentDateAndTime()) : "");
                appointmentDto.setNextAppointmentDate(appointment.getNextAppointmentDate() != null ? util.decrypt(appointment.getNextAppointmentDate()) : "");
                appointmentDto.setHeartRate(appointment.getHeartRate() != null ? util.decrypt(new String(appointment.getHeartRate())) : "");
                appointmentDto.setPulseRate(appointment.getPulseRate() != null ? util.decrypt(new String(appointment.getPulseRate())) : "");
                appointmentDto.setRespiratoryRate(appointment.getRespiratoryRate() != null ? util.decrypt(new String(appointment.getRespiratoryRate())) : "");
                appointmentDto.setAllergies(appointment.getAllergies() != null ? util.decrypt(new String(appointment.getAllergies())) : "");
                appointmentDto.setSymptoms(appointment.getSymptoms() != null ? util.decrypt(new String(appointment.getSymptoms())) : "");
                appointmentDto.setClinicalNote(appointment.getClinicalNote() != null ? util.decrypt(new String(appointment.getClinicalNote())) : "");
                appointmentDto.setBloodPressure(appointment.getBloodPressure() != null ? util.decrypt(appointment.getBloodPressure()) : "");

                // Add the DTO to the list
                appointmentDTOList.add(appointmentDto);
            }

            // Return the list wrapped in ResponseEntity
            return ResponseEntity.ok(appointmentDTOList);

        }  catch (Exception e) {
            e.printStackTrace();
            logger.error("Unexpected error in getAppointmentsByBranch(): " + ExceptionUtils.getStackTrace(e) + " LoggedIn User: " + loginUser);
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }



    //---------------------------------------Missed Appointments--------------------------------------------------------------------

    @PostConstruct
    public void sendEmailToPatientMissedAppointments() {
        // Start a new thread to handle the missed appointment
        new Thread(() -> {
            while (true) {
                try {
                    // Fetch all scheduled appointments
                    List<Appointment> scheduledAppointments = appointmentService.findByAppointmentStatus(AppointmentStatus.SCHEDULED);

                    // Filter the appointments that are missed and have not yet received a missed appointment notification
                    List<Appointment> missedAppointments = scheduledAppointments.stream()
                            .filter(P -> {

                                String decryptedData = util.decrypt(P.getAppointmentDateAndTime()); // Decrypt string
                                return LocalDateTime.parse(decryptedData).isBefore(LocalDateTime.now().minusMinutes(50)) && P.getMissedMailStatus() == 0;
                            }).collect(Collectors.toList());


                    // Loop through each missed appointment
                    for (Appointment app : missedAppointments) {

                        // Update the appointment status to "MISSED"
                        app.setAppointmentStatus(AppointmentStatus.MISSED);

                        System.err.println("Your Appointment Missed : ->" + LocalDateTime.now());
                        emailService.sendEmail(util.decrypt(new String(app.getPatient().getEmailId())), "🚨 Appointment Missed Notification", getAppointmentMissingTemplate(
                                util.decrypt(new String(app.getPatient().getName())),
                                app.getDoctor().getName(),
                                app.getHospital().getName(),
                                app.getAppointmentStatus(),
                                LocalDateTime.parse(util.decrypt(app.getAppointmentDateAndTime()))));

                        // Set the missed mail status to 1
                        app.setMissedMailStatus(1);

                        appointmentService.saveAppointment(app);
                    }

                    // Sleep for 2 minutes before checking for missed appointments again
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    // If the thread is interrupted, handle the interruption gracefully
                    Thread.currentThread().interrupt();
                }
            }
        }).start(); // Start the email sending thread for missed appointments
    }


    //    ------------------------------------------ Reminder Appointments one day before--------------------------------------------------------------
    //  Checks for appointments scheduled exactly 24 hours from the current time and sends reminder emails to those patients.

    @PostConstruct
    public void sendEmailToPatientReminderMail() {
        new Thread(() -> {
            while (true) {
                try {
                    // Fetch all scheduled appointments
                    List<Appointment> scheduledAppointments = appointmentService.findByAppointmentStatus(AppointmentStatus.SCHEDULED);

                    // Compares appointment time minus 24 hours (rounded to minutes) with current time (also rounded to minutes)
                    // truncatedTo(ChronoUnit.MINUTES)--> It removes seconds, milliseconds, and nanoseconds
                    List<Appointment> upcomingAppointments = scheduledAppointments.stream().filter(app -> {

                        String decryptedData = util.decrypt(app.getAppointmentDateAndTime()); // Decrypt string

                        return app.getReminderMailStatus() == 0 && LocalDateTime.parse(decryptedData).minusHours(24).truncatedTo(ChronoUnit.MINUTES).isEqual(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

                        // alternative logic : compares only dates, but not time-specific
                        // LocalDateTime.parse(decryptedData).toLocalDate().isEqual(LocalDate.now().plusDays(1))

                    }).collect(Collectors.toList());

                    // Send emails to the filtered appointments
                    for (Appointment app : upcomingAppointments) {

                        emailService.sendEmail(util.decrypt(new String(app.getPatient().getEmailId())), "🚨Reminder Appointment Notification", sendEmailToPatientReminderMail(
                                util.decrypt(new String(app.getPatient().getName())),
                                app.getDoctor().getName(),
                                app.getHospital().getName(),
                                app.getAppointmentStatus(),
                                LocalDateTime.parse(util.decrypt(app.getAppointmentDateAndTime()))));

                        // Update the reminder mail status to 1
                        app.setReminderMailStatus(1);
                        appointmentService.saveAppointment(app);
                    }

                    Thread.sleep(60000); // Sleep for 1 minute
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start(); // Start the email sending thread
    }


    //------------------------------------------ You are queue Appointments --------------------------------------------------------------
    //  If so, it sends a queue notification email to the next SCHEDULED patients for the same doctor and updates their queueMailStatus
    @PostConstruct
    public void QueueNotification() {
        new Thread(() -> {
            while (true) {
                try {
                    // Fetch all appointments currently in ENGAGED status
                    List<Appointment> engagedAppointments = appointmentService.findByAppointmentStatus(AppointmentStatus.ENGAGED);

                    for (Appointment engagedAppointment : engagedAppointments) {

                        LocalDateTime engagedTime = LocalDateTime.parse(util.decrypt(engagedAppointment.getAppointmentDateAndTime()));
                        LocalDateTime triggerTime = engagedTime.plusMinutes(15);

                        // Check if current time is after the trigger time
                        if (LocalDateTime.now().isAfter(triggerTime)) {

                            // Fetch all SCHEDULED appointments
                            List<Appointment> scheduledAppointments = appointmentService.findByAppointmentStatus(AppointmentStatus.SCHEDULED);

                            List<Appointment> filteredAppointments = scheduledAppointments.stream()

                                    .filter(a -> {
                                        String decryptedData = util.decrypt(a.getAppointmentDateAndTime()); // Decrypt string

                                        return LocalDateTime.parse(decryptedData).toLocalDate().isEqual(LocalDate.now()) &&   // - Appointment is today
                                                LocalDateTime.parse(decryptedData).isAfter(triggerTime) && // - Appointment time is after the trigger time
                                                a.getDoctor().getId() == engagedAppointment.getDoctor().getId();  // - Appointment is with the same doctor
                                    }).collect(Collectors.toList());


                            // Send queue notification email if not already sent
                            for (Appointment appointment : filteredAppointments) {
                                if (appointment.getQueueMailStatus() == 0) {
                                    emailService.sendEmail(util.decrypt(new String(appointment.getPatient().getEmailId())), "🚨 You are in Queue", sendQueueNotificationTemplate(
                                            util.decrypt(new String(appointment.getPatient().getName())),
                                            appointment.getDoctor().getName(),
                                            appointment.getHospital().getName(),
                                            appointment.getAppointmentStatus(),
                                            LocalDateTime.parse(util.decrypt(appointment.getAppointmentDateAndTime()))));

                                    // Update status to indicate queue email has been sent
                                    appointment.setQueueMailStatus(1);
                                    appointmentService.saveAppointment(appointment);
                                    System.out.println("Queue email sent to: " +   util.decrypt(new String(appointment.getPatient().getName())));
                                }
                            }
                        }
                    }
                    // Wait 2 minutes before next check
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }


    // ************************************ Next Appointment Booking Reminder Notification **************************************************
    //------------------------------------------ Send mail at 9 AM --book-slot-emails------------------------------------------------------------
    //  Checks the time every few seconds and sends reminder emails at exactly 9:00 AM.
    //  It notifies patients who had a completed appointment and haven’t booked their next appointment despite a doctor-suggested follow-up date being today.

    @PostConstruct
    public void startAppointmentReminderThread() {
        new Thread(() -> {
            while (true) {
                try {
                    LocalTime now = LocalTime.now();

                    // Check if the current time is exactly 9:00 AM
                    if (now.getHour() == 9 && now.getMinute() == 0) {

                        List<Appointment> completedAppointments = appointmentService.findByAppointmentStatus(AppointmentStatus.COMPLETED);

                        List<Appointment> pendingAppointments = completedAppointments.stream().filter(

                                app -> {
                                    String decriptStringNextAppointments = util.decrypt(app.getNextAppointmentDate());

                                    return LocalDate.parse(decriptStringNextAppointments) != null // Ensure doctor suggested a next date
                                            && LocalDate.parse(decriptStringNextAppointments).isEqual(LocalDate.now()) // Check if it's today
                                            && !appointmentService.hasScheduledNextAppointment(app.getPatient().getId()); // Patient has not yet booked the next appointment

                                }).collect(Collectors.toList());

                        // Send emails to patients who haven't scheduled their next appointment
                        for (Appointment app : pendingAppointments) {
                            System.out.println("Next Day Remider Mail :  "+util.decrypt(new String(app.getPatient().getName())));
                            emailService.sendEmail(util.decrypt(new String(app.getPatient().getEmailId())), "📅 Book Your Slot for Next Appointment", sendEmailToPatientBookSlotMail(
                                    util.decrypt(new String(app.getPatient().getName())),
                                    app.getDoctor().getName(),
                                    app.getHospital().getName(),
                                    LocalDate.parse(util.decrypt(app.getNextAppointmentDate()))));

                        }
                        Thread.sleep(60000); // Sleep for 1 minute to avoid duplicate execution in the same minute
                    } else {
                        Thread.sleep(5000); // Sleep for 5 seconds before checking again
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }


// ********************************** Email Templates **************************************************************************

    // Book Appointment Mail
    public String getAppointmentConfirmationTemplate(String patientName, String doctorName, String hospitalName, AppointmentStatus appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);

        return "<!DOC TYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f4f4f9;" +
                "}" +
                ".container {" +
                "    width: 100%;" +
                "    max-width: 600px;" +
                "    margin: 20px auto;" +
                "    background: #ffffff;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
                "    overflow: hidden;" +
                "}" +
                ".header {" +
                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +  // BLUE gradient
                "    color: #ffffff;" +
                "    text-align: center;" +
                "    padding: 20px;" +
                "    font-size: 24px;" +
                "}" +
                ".body {" +
                "    padding: 20px;" +
                "    line-height: 1.6;" +
                "    color: #333333;" +
                "}" +
                ".footer {" +
                "    background-color: #f4f4f9;" +
                "    color: #888888;" +
                "    text-align: center;" +
                "    padding: 10px;" +
                "    font-size: 12px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Appointment Confirmation" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear <b>" + patientName + "</b>,</p>" +
                "<p>🎉 Your appointment has been successfully booked!</p>" +
                "<p><b>Details:</b></p>" +
                "<ul>" +
                "<li><b>Doctor:</b> " + doctorName + "</li>" +
                "<li><b>Hospital:</b> " + hospitalName + "</li>" +
                "<li><b>Status:</b> " + appointmentStatus.name() + "</li>" +
                "<li><b>Date:</b> " + date + "</li>" +
                "<li><b>Time:</b> " + time + "</li>" +
                "</ul>" +
                "<p>💡 <i>“Invest in your health today, because you deserve nothing less than the best!”</i></p>" +
                "<p>✨ We’re excited to welcome you. Got questions? We’re just a call away!</p>" +
                "<p>Stay healthy,<br><b>" + hospitalName + "</b></p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved.<br>" +
                "<i>This is an automated message, please do not reply.</i>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    //Update Appointment Mail
    public String getAppointmentUpdateNotificationTemplate(String patientName, String doctorName, String hospitalName, AppointmentStatus appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);

        return "<!DOC TYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f4f4f9;" +
                "}" +
                ".container {" +
                "    width: 100%;" +
                "    max-width: 600px;" +
                "    margin: 20px auto;" +
                "    background: #ffffff;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
                "    overflow: hidden;" +
                "}" +
                ".header {" +
                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
                "    color: #ffffff;" +
                "    text-align: center;" +
                "    padding: 20px;" +
                "    font-size: 24px;" +
                "}" +
                ".body {" +
                "    padding: 20px;" +
                "    line-height: 1.6;" +
                "    color: #333333;" +
                "}" +
                ".footer {" +
                "    background-color: #f4f4f9;" +
                "    color: #888888;" +
                "    text-align: center;" +
                "    padding: 10px;" +
                "    font-size: 12px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Appointment Update Notification" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Hi <b>" + patientName + "</b>,</p>" +
                "<p>🔄 We wanted to let you know that your appointment has been successfully <b>updated</b>!</p>" +
                "<p><b>Updated Details:</b></p>" +
                "<ul>" +
                "<li><b>Doctor:</b> Dr. " + doctorName + "</li>" +
                "<li><b>Hospital:</b> " + hospitalName + "</li>" +
                "<li><b>Status:</b> " + appointmentStatus.name() + "</li>" +
                "<li><b>Updated Date:</b> " + date + "</li>" +
                "<li><b>Time:</b> " + time + "</li>" +
                "</ul>" +
                "<p>💡 <i>Your health matters to us, and we're here to ensure you’re always taken care of!</i></p>" +
                "<p>✨ If you have any questions or need further assistance, don’t hesitate to reach out. We're always here for you!</p>" +
                "<p>Thank you for choosing <b>" + hospitalName + "</b>. We can’t wait to see you!</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved.<br>" +
                "<i>This is an autogenerated email, please do not reply.</i>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Appointment Cancellation Mail
    public String getAppointmentCancellationTemplate(String patientName, String doctorName, String hospitalName, AppointmentStatus appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f4f4f9;" +
                "}" +
                ".container {" +
                "    width: 100%;" +
                "    max-width: 600px;" +
                "    margin: 20px auto;" +
                "    background: #ffffff;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
                "    overflow: hidden;" +
                "}" +
                ".header {" +
                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
                "    color: #ffffff;" +
                "    text-align: center;" +
                "    padding: 20px;" +
                "    font-size: 24px;" +
                "}" +
                ".body {" +
                "    padding: 20px;" +
                "    line-height: 1.6;" +
                "    color: #333333;" +
                "}" +
                ".footer {" +
                "    background-color: #f4f4f9;" +
                "    color: #888888;" +
                "    text-align: center;" +
                "    padding: 10px;" +
                "    font-size: 12px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Appointment Cancellation Notice" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Hi <b>" + patientName + "</b>,</p>" +
                "<p>🚨 We regret to inform you that your appointment has been <b>cancelled</b>.</p>" +
                "<p><b>Appointment Details:</b></p>" +
                "<ul>" +
                "<li><b>Doctor:</b> Dr. " + doctorName + "</li>" +
                "<li><b>Hospital:</b> " + hospitalName + "</li>" +
                "<li><b>Status:</b> " + appointmentStatus.name() + "</li>" +
                "<li><b>Date:</b> " + date + "</li>" +
                "<li><b>Time:</b> " + time + "</li>" +
                "</ul>" +
                "<p>🔄 <i>We sincerely apologize for any inconvenience caused. Your health is our top priority, and we’re here to help you reschedule at your convenience.</i></p>" +
                "<p>✨ If you have any questions or need further assistance, don’t hesitate to contact us. We’re always here to support you!</p>" +
                "<p>Thank you for understanding. We look forward to assisting you again soon!</p>" +
                "<p>Stay well,<br><b>" + hospitalName + "</b></p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved.<br>" +
                "<i>This is an autogenerated email, please do not reply.</i>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Missed Appointment Mail
    public String getAppointmentMissingTemplate(String patientName, String doctorName, String hospitalName, AppointmentStatus appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f4f4f9;" +
                "}" +
                ".container {" +
                "    width: 100%;" +
                "    max-width: 600px;" +
                "    margin: 20px auto;" +
                "    background: #ffffff;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
                "    overflow: hidden;" +
                "}" +
                ".header {" +
                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
                "    color: #ffffff;" +
                "    text-align: center;" +
                "    padding: 20px;" +
                "    font-size: 24px;" +
                "}" +
                ".body {" +
                "    padding: 20px;" +
                "    line-height: 1.6;" +
                "    color: #333333;" +
                "}" +
                ".footer {" +
                "    background-color: #f4f4f9;" +
                "    color: #888888;" +
                "    text-align: center;" +
                "    padding: 10px;" +
                "    font-size: 12px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Missed Appointment Notice" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>🚨 Hi <b>" + patientName + "</b>,</p>" +
                "<p>We noticed that you <b>" + appointmentStatus + "</b> your appointment.</p>" +
                "<p><b>Appointment Details:</b></p>" +
                "<ul>" +
                "<li><b>Doctor:</b> Dr. " + doctorName + "</li>" +
                "<li><b>Hospital:</b> " + hospitalName + "</li>" +
                "<li><b>Original Date:</b> " + date + "</li>" +
                "<li><b>Time:</b> " + time + "</li>" +
                "</ul>" +
                "<p>🔄 <i>Your health is important to us, and we kindly ask you to reschedule your appointment at your earliest convenience. Our team is here to assist you in finding a suitable date and time.</i></p>" +
                "<p>✨ If you have any questions or need assistance, please don’t hesitate to contact us. We’re always happy to help!</p>" +
                "<p>Thank you for your prompt attention. We look forward to serving you soon!</p>" +
                "<p>Stay well,<br><b>" + hospitalName + " Team</b></p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved.<br>" +
                "<i>This is an autogenerated email, please do not reply.</i>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Appointment Reminder Mail
    public String sendEmailToPatientReminderMail(String patientName, String doctorName, String hospitalName, AppointmentStatus appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f4f4f9;" +
                "}" +
                ".container {" +
                "    width: 100%;" +
                "    max-width: 600px;" +
                "    margin: 20px auto;" +
                "    background: #ffffff;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
                "    overflow: hidden;" +
                "}" +
                ".header {" +
                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
                "    color: #ffffff;" +
                "    text-align: center;" +
                "    padding: 20px;" +
                "    font-size: 24px;" +
                "}" +
                ".body {" +
                "    padding: 20px;" +
                "    line-height: 1.6;" +
                "    color: #333333;" +
                "}" +
                ".footer {" +
                "    background-color: #f4f4f9;" +
                "    color: #888888;" +
                "    text-align: center;" +
                "    padding: 10px;" +
                "    font-size: 12px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Upcoming Appointment Reminder" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>🚨 Hi <b>" + patientName + "</b>,</p>" +
                "<p>This is a friendly reminder that you have an upcoming appointment scheduled.</p>" +
                "<p><b>Appointment Details:</b></p>" +
                "<ul>" +
                "<li><b>Doctor:</b> Dr. " + doctorName + "</li>" +
                "<li><b>Hospital:</b> " + hospitalName + "</li>" +
                "<li><b>Scheduled Date:</b> " + date + "</li>" +
                "<li><b>Time:</b> " + time + "</li>" +
                "</ul>" +
                "<p>🔔 <i>We want to ensure you're fully prepared for your appointment, so please make sure to arrive on time.</i></p>" +
                "<p>✨ If you need to reschedule or have any questions about your appointment, please don’t hesitate to reach out to us. We’re happy to assist!</p>" +
                "<p>We look forward to seeing you soon and assisting with your healthcare needs.</p>" +
                "<p>Stay healthy,<br><b>" + hospitalName + " Team</b></p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved.<br>" +
                "<i>This is an autogenerated email, please do not reply.</i>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Appointment Queue Mail Template
    public String sendQueueNotificationTemplate(String patientName, String doctorName, String hospitalName, AppointmentStatus appointmentStatus, LocalDateTime appointmentDateAndTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = appointmentDateAndTime.toLocalDate().format(dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = appointmentDateAndTime.toLocalTime().format(timeFormatter);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f4f4f9;" +
                "}" +
                ".container {" +
                "    width: 100%;" +
                "    max-width: 600px;" +
                "    margin: 20px auto;" +
                "    background: #ffffff;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
                "    overflow: hidden;" +
                "}" +
                ".header {" +
                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
                "    color: #ffffff;" +
                "    text-align: center;" +
                "    padding: 20px;" +
                "    font-size: 24px;" +
                "}" +
                ".body {" +
                "    padding: 20px;" +
                "    line-height: 1.6;" +
                "    color: #333333;" +
                "}" +
                ".footer {" +
                "    background-color: #f4f4f9;" +
                "    color: #888888;" +
                "    text-align: center;" +
                "    padding: 10px;" +
                "    font-size: 12px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Appointment Status Update" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>🚨 Hi <b>" + patientName + "</b>,</p>" +
                "<p>We wanted to inform you that your appointment is currently in the queue due to a prior engaged appointment. We apologize for the delay.</p>" +
                "<p><b>Appointment Details:</b></p>" +
                "<ul>" +
                "<li><b>Doctor:</b> Dr. " + doctorName + "</li>" +
                "<li><b>Hospital:</b> " + hospitalName + "</li>" +
                "<li><b>Date:</b> " + date + "</li>" +
                "<li><b>Time:</b> " + time + "</li>" +
                "</ul>" +
                "<p>🔄 <i>Your appointment might be slightly delayed, but we’ll keep you updated as soon as it's your turn. We apologize for the inconvenience.</i></p>" +
                "<p>✨ If you have any questions or need further assistance, please don’t hesitate to reach out to us. We’re here to assist you!</p>" +
                "<p>Thank you for your patience, and we look forward to providing you with the best care possible.</p>" +
                "<p>Stay well,<br><b>" + hospitalName + "</b></p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved.<br>" +
                "<i>This is an autogenerated email, please do not reply.</i>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    // Next Appointment Book-Slot-Mail
    public String sendEmailToPatientBookSlotMail(String patientName, String doctorName, String hospitalName, LocalDate nextAppointmentDate) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = nextAppointmentDate.format(dateFormatter);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f4f4f9;" +
                "}" +
                ".container {" +
                "    width: 100%;" +
                "    max-width: 600px;" +
                "    margin: 20px auto;" +
                "    background: #ffffff;" +
                "    border-radius: 10px;" +
                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
                "    overflow: hidden;" +
                "}" +
                ".header {" +
                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
                "    color: #ffffff;" +
                "    text-align: center;" +
                "    padding: 20px;" +
                "    font-size: 24px;" +
                "}" +
                ".body {" +
                "    padding: 20px;" +
                "    line-height: 1.6;" +
                "    color: #333333;" +
                "}" +
                ".footer {" +
                "    background-color: #f4f4f9;" +
                "    color: #888888;" +
                "    text-align: center;" +
                "    padding: 10px;" +
                "    font-size: 12px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Next Appointment Reminder" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>📅 <b>Hi " + patientName + "</b>,</p>" +
                "<p>We noticed that your doctor recommended a follow-up appointment for <b>" + date + "</b>, but it looks like you haven’t booked it yet.</p>" +
                "<p><b>Appointment Details:</b></p>" +
                "<ul>" +
                "<li><b>Doctor:</b> Dr. " + doctorName + "</li>" +
                "<li><b>Hospital:</b> " + hospitalName + "</li>" +
                "<li><b>Recommended Follow-up Date:</b> " + date + "</li>" +
                "</ul>" +
                "<p>🔔 <i>To ensure continuous care, please book your next appointment as soon as possible.</i></p>" +
                "<p>✨ <b>Book your appointment now </b></p>" +
                "<p>Stay healthy,<br><b>Medica Healthcare Team</b></p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved.<br>" +
                "<i>This is an autogenerated email, please do not reply.</i>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

// ********************************************************************************************************************************************

    // LocalDate Parse Methods
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            System.out.println("Input date is null or empty.");
            return null;
        }
        // Normalize month part to ensure it's correctly capitalized (e.g., mar to Mar)
        dateStr = normalizeMonth(dateStr);

        List<String> supportedFormats = Arrays.asList(
                // ISO & Universal formats
                "yyyy-MM-dd", "dd-MM-yyyy", "MM-dd-yyyy",
                "yyyy.MM.dd", "dd.MM.yyyy", "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy", "MMM dd yyyy", "MM dd yyyy",

                // Short formats
                "yy-MM-dd", "dd-MM-yy", "MM-dd-yy",
                "yy/MM/dd", "dd/MM/yy", "MM/dd/yy",
                "yy.MM.dd", "dd.MM.yy", "MM.dd.yy",

                // Long & Localized formats
                "d MMM yyyy", "dd MMM yyyy", "d MMMM yyyy", "dd MMMM yyyy",
                "d-MMM-yyyy", "d-MMMM-yyyy", "dd-MMM-yyyy", "dd-MMMM-yyyy",
                "d MMM, yyyy", "d MMMM, yyyy", "dd MMM, yyyy", "dd MMMM, yyyy",
                "MMMM d, yyyy", "MMMM dd, yyyy", "MMM d, yyyy", "MMM dd, yyyy",
                "yyyy MMMM dd", "yyyy MMM dd",

                // Dot and slash variants used in Russia, Eastern Europe, and parts of Asia
                "d.M.yyyy", "d.M.yy", "dd.MM.yyyy", "dd.MM.yy",
                "d/M/yyyy", "d/M/yy", "dd/M/yyyy", "dd/MM/yy",

                // Japanese/Chinese/Korean-style
                //"yyyy年MM月dd日", "yy年MM月dd日",

                // Indian Government format
                "dd-MMM-yyyy",

                // Passport/Govt/Bank standard (common globally)
                "dd MMM yyyy", "dd MMM yy", "d MMM yyyy", "d MMM yy",

                // Weekday included formats
                "EEEE, MMMM d, yyyy", "EEE, MMM d, yyyy", "EEEE, d MMMM yyyy",

                // Compact formats
                "yyyyMMdd", "ddMMyyyy", "MMddyyyy", "yyMMdd", "MMddyy",

                // Year & Month only
                "MM-yyyy", "yyyy-MM", "MMMM yyyy", "MMM yyyy"
        );

        for (String pattern : supportedFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
//                System.out.println("Failed to parse date: " + dateStr + " using pattern: " + pattern);
            }
        }

        return null;
    }

    private String normalizeMonth(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return dateStr;
        }
        String[] dateParts = dateStr.split(" ");
        if (dateParts.length >= 2) {
            dateParts[1] = dateParts[1].substring(0, 1).toUpperCase() + dateParts[1].substring(1).toLowerCase();
            return String.join(" ", dateParts);
        }
        return dateStr;
    }


    //LocalDateTime Parse Methods
    private LocalDateTime parseDateTime(String strDateTime) {
        if (strDateTime == null || strDateTime.trim().isEmpty()) {
            System.out.println("Input date is null or empty.");
            return null;
        }
        // Normalize month part to ensure it's correctly capitalized (e.g., mar to Mar)
        strDateTime = normalizeMonths(strDateTime);

        List<String> supportedFormats = Arrays.asList(
                "EEE MMM dd HH:mm:ss yyyy",                 // Unix style
                "EEE, dd MMM yyyy HH:mm:ss",                // RFC 1123
                "EEEE dd MMMM yyyy HH:mm:ss",               // Full weekday + full date
                "EEEE, MMMM dd, yyyy hh:mm a",              // US long
                "HH:mm:ss dd-MM-yyyy",                      // Time before date
                "MM-dd-yyyy HH:mm:ss",                      // US dash style
                "MMM dd, yyyy HH:mm",                       // US readable
                "MMMM dd, yyyy hh:mm:ss a",                 // Full US with AM/PM
                "YYYY-'W'ww-e",                             // ISO week date
                "dd MMM yyyy HH:mm",                        // Common log format
                "dd MMM yyyy, HH:mm:ss",                    // Logs & emails
                "dd-MM-yyyy HH:mm:ss",                      // EU style
                "dd.MM.yyyy HH:mm",                         // Dot separated (DE, RU)
                "dd/MM/yy HH:mm:ss",                        // Short year format
                "dd/MM/yyyy HH:mm:ss",                      // India, Europe
                "yyMMddHHmmss",                             // Compact timestamp
                "yyyy-MM-dd HH:mm:ss",                      // SQL style
                "yyyy-MM-dd'T'HH:mm",                       // ISO no seconds
                "yyyy-MM-dd'T'HH:mm:ss",                    // ISO 8601
                "yyyy-MM-dd'T'HH:mm:ss.SSS",                // ISO with ms
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",             // ISO with ms + offset
                "yyyy-MM-dd'T'HH:mm:ssXXX",                 // ISO with offset
                "yyyy-MM-dd'T'HH:mm:ssZ",                   // ISO with timezone Z
                "yyyy.MM.dd G 'at' HH:mm:ss z",             // Era + zone
                "yyyy/MM/dd HH:mm:ss",                      // Slash-separated
                "yyyy/MM/dd'T'HH:mm:ss",                    // ISO with slashes
                "yyyyMMdd HHmm",                            // Basic timestamp
                "yyyyMMdd'T'HHmmss",                        // No separator
                "yyyy年MM月dd日 HH:mm:ss",                   // Chinese full
                "yyyy年MM月dd日 HH時mm分"                    // Japanese style
        );

        for (String pattern : supportedFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                return LocalDateTime.parse(strDateTime, formatter);
            } catch (Exception e) {
//                System.out.println("Failed to parse date: " + dateStr + " using pattern: " + pattern);
            }
        }

        return null;
    }

    private String normalizeMonths(String strDateTime) {
        if (strDateTime == null || strDateTime.trim().isEmpty()) {
            return strDateTime;
        }
        String[] dateParts = strDateTime.split(" ");
        if (dateParts.length >= 2) {
            dateParts[1] = dateParts[1].substring(0, 1).toUpperCase() + dateParts[1].substring(1).toLowerCase();
            return String.join(" ", dateParts);
        }
        return strDateTime;
    }

//*********************************************************************************************************************************************

    // Converter Methods Entity to DTOs

    public PatientDTO patientToPatientDto(Patient patient) {
        try {
            PatientDTO patientDTO = new PatientDTO();
            patientDTO.setId(patient.getId());
            patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");
            patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
            patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
            patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");
            patientDTO.setDateOfBirth(patient.getDateOfBirth() != null ? util.decrypt(patient.getDateOfBirth()) : "");
            patientDTO.setGender(util.decrypt(patient.getGender()));
            //System.out.println("Gender: "+ patientDTO.setGender(util.decrypt(patient.getGender())));
            patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");
            patientDTO.setAge(Integer.parseInt(util.decrypt(patient.getAge() + "")));
            patientDTO.setStatus(patient.getStatus());
            patientDTO.setCurrentStatus(patient.getCurrentStatus());
            patientDTO.setDiet(patient.getDiet());
            patientDTO.setCurrentDoctor(patient.getCurrentDoctor());
            patientDTO.setCreatedDate(patient.getCreatedDate());
            patientDTO.setModifiedDate(patient.getModifiedDate());
            patientDTO.setCreatedUser(patient.getCreatedUser());
            patientDTO.setModifiedUser(patient.getModifiedUser());
            return patientDTO;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}

