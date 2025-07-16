package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.services.EmailService;
import com.fspl.medica_healthcare.services.PatientService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.annotation.PostConstruct;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private static final Logger logger = Logger.getLogger(PatientController.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private PatientService patientService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/activePatients")
    public synchronized ResponseEntity<?> getActivePatients() {
        try {
            List<Patient> activePatients = patientService.getPatientsByStatus(1);

            if (activePatients != null) {
                return ResponseEntity.ok(activePatients);
            }
            else {
                return ResponseEntity.badRequest().body("Error : No active patients found.");
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("Error fetching active patients : " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/inactivePatients")
    public synchronized ResponseEntity<?> getInactivePatients() {
        try {
            List<Patient> inactivePatients = patientService.getPatientsByStatus(0);

            if (inactivePatients != null)
                return ResponseEntity.ok(inactivePatients);
            else
                return ResponseEntity.badRequest().body("Error: No inactive patients found.");
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("Error fetching inactive patients : " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/currentStatus/activePatients")
    public synchronized ResponseEntity<Object> getActiveCurrentStatusPatients() {
        try {
            List<Patient> currentActivePatients = patientService.getPatientsByCurrentStatus(1);

            if (currentActivePatients != null)
                return ResponseEntity.ok(currentActivePatients);
            else
                return ResponseEntity.badRequest().body("Error : No active patients found with current status.");
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("Error fetching active patients : " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/currentStatus/inactivePatients")
    public synchronized ResponseEntity<?> getInactiveCurrentStatusPatients() {
        try {
            List<Patient> currentInactivePatients = patientService.getPatientsByCurrentStatus(0);

            if (currentInactivePatients != null)
                return ResponseEntity.ok(currentInactivePatients);
            else
                return ResponseEntity.badRequest().body("Error : No inactive patients found with current status.");
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("Error fetching inactive patients : " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/byPatientId/{id}")
    public ResponseEntity<?> getPatientById(@PathVariable long id) {
        try {
            if (id < 0) {
                return ResponseEntity.badRequest().body("Error : Patient ID must be a valid positive number.");
            }

            Patient patient = patientService.getPatientById(id);

            if (patient != null)
                return ResponseEntity.ok(patient);
            else
                return ResponseEntity.badRequest().body("Error : No patient found with ID : " + id);

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("An error occurred while fetching patient data : " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/byContact/{contactNumber}")
    public ResponseEntity<?> getPatientsByContactNumber(@PathVariable String contactNumber) {
        try {
            if (contactNumber == null || contactNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: Contact number is required.");
            }
            if (contactNumber.length() > 10) {
                return ResponseEntity.badRequest().body("Error: Contact number should contain only 10 digits.");
            }
            if (!contactNumber.matches("^\\d+$")) {
                return ResponseEntity.badRequest().body("Error: Contact number should contain only digits.");
            }

            List<Patient> patients = patientService.getPatientsByContactNumber(contactNumber);

            if (patients != null)
                return ResponseEntity.ok(patients);
            else
                return ResponseEntity.badRequest().body("No patients found with contact number starting with : " + contactNumber);

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("An error occurred while fetching patient data : " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/byName")
    public ResponseEntity<?> getPatientsByName(@RequestParam String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Error : Name is required.");
            }

            if (!name.matches("^[a-zA-Z ]+$")) {
                return ResponseEntity.badRequest().body("Error : Name should contain only alphabets and spaces.");
            }

            name = name.trim().toLowerCase();

            List<Patient> patients = patientService.getPatientsByName(name);

            if (patients != null)
                return ResponseEntity.ok(patients);
            else
                return ResponseEntity.badRequest().body("No patients found with name : " + name);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("An error occurred while fetching patient data : " + e.getMessage());
        }
    }

    @PostConstruct
    public void scheduleBirthdayEmails() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRun = now.withHour(17).withMinute(6).withSecond(0);

            if (now.isAfter(nextRun)) {
                nextRun = nextRun.plusDays(1);
            }

            long initialDelay = java.time.Duration.between(now, nextRun).toMinutes();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    System.out.println("Running birthday email at: " + LocalDateTime.now());

                    int birthdateMonth = LocalDateTime.now().getMonthValue();
                    int birthdateDay = LocalDateTime.now().getDayOfMonth();

                    List<Patient> patientBirthdayList = patientService.getPatientBirthdayWithHospital(birthdateMonth, birthdateDay);

                    if (patientBirthdayList == null) {
                        System.out.println("No patients found with a birthday today.");
                        return;
                    }

                    int i = 0;
                    while (i < patientBirthdayList.size()) {
                        Patient patient = patientBirthdayList.get(i);
                        try {
                            emailService.sendEmail(patient.getEmailId(),
                                    "🎂 Happy Birthday " + patient.getName() + "!",
                                    "<!DOCTYPE html>" +
                                            "<html>" +
                                            "<head>" +
                                            "<meta charset='UTF-8'>" +
                                            "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                                            "<style>" +
                                            "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; margin: 0; }" +
                                            ".container { max-width: 600px; background: white; padding: 20px; border-radius: 10px; text-align: center; " +
                                            "box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.1); margin: auto; }" +
                                            "h1 { color: #ff6600; font-size: 24px; }" +
                                            "p { font-size: 16px; color: #333; line-height: 1.5; }" +
                                            ".footer { margin-top: 20px; font-size: 14px; color: #777; }" +
                                            ".banner { width: 100%; border-radius: 10px; }" +
                                            "</style>" +
                                            "</head>" +
                                            "<body>" +
                                            "<div class='container'>" +
                                            "<h1>🎉 Happy Birthday, " + patient.getName() + "! 🎂</h1>" +
                                            "<p>We at <strong>" + patient.getHospital().getName() + " \uD83C\uDF82</h1></strong> wish you a day filled with joy, laughter, and good health.</p>" +
                                            "<p>May this year bring you happiness, success, and wellness.</p>" +
                                            "<p>Stay healthy and enjoy your special day! 🎊</p>" +
                                            "<div class='footer'>Best Wishes,<br/><strong> " + patient.getHospital().getName() + " Team</strong></div>" +
                                            "</div>" +
                                            "</body></html>");
                            System.out.println("Email sent to: " + patient.getEmailId());
                        } catch (Exception e) {
                            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
                            System.err.println("Failed to send email to " + patient.getEmailId() + ": " + e.getMessage());
                        }
                        i++;
                    }
                    System.out.println("Birthday emails are being processed.");
                } catch (Exception e) {
                    logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
                    System.err.println("Error occurred while running the birthday email scheduler: " + e.getMessage());
                }
            }, initialDelay, 60 * 24, TimeUnit.MINUTES);
            System.out.println("Birthday email scheduler initialized. First run in " + initialDelay + " minutes.");
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            System.err.println("Error initializing the birthday email scheduler: " + e.getMessage());
        }
    }
}