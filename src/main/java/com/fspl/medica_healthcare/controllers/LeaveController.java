package com.fspl.medica_healthcare.controllers;


import com.fspl.medica_healthcare.dtos.LeaveDTO;
import com.fspl.medica_healthcare.dtos.PrescriptionDTO;
import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Leaves;
import com.fspl.medica_healthcare.models.Prescription;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.LeaveRepository;
import com.fspl.medica_healthcare.repositories.UserRepository;
import com.fspl.medica_healthcare.services.AppointmentService;
import com.fspl.medica_healthcare.services.EmailService;
import com.fspl.medica_healthcare.services.LeaveService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import com.itextpdf.text.log.SysoCounter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private EncryptionUtil encryptionUtil;


    private static final Logger logger = Logger.getLogger(LeaveController.class);



    @PostMapping("/create")
    public ResponseEntity<String> createLeave(@Valid @RequestBody LeaveDTO leaves) {
        try {
            LocalDate fromDate = parseFlexibleDate(leaves.getFromDate());
            LocalDate toDate = parseFlexibleDate(leaves.getToDate());

            if (fromDate.isAfter(toDate)) {
                return ResponseEntity.badRequest().body("The 'fromDate' cannot be after 'toDate'.");
            }

            User loginUser = userService.getAuthenticateUser();
            long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate) + 1;

            List<Leaves> leaveList = new ArrayList<>();
            for (int i = 0; i < daysBetween; i++) {
                Leaves leave = new Leaves();
                leave.setUser(loginUser);
                leave.setDate(fromDate.plusDays(i));
                leave.setConcern(leaves.getConcern());
                leave.setType(leaves.getType());
                leaveList.add(leave);
            }

            // Save leaves
            if (!leaveService.saveOrUpdateLeave(leaveList)) {
                return ResponseEntity.ok("Something Went Wrong");
            }

            // Cancel appointments during leave
            List<Appointment> cancelledAppointments = new ArrayList<>();
            for (int i = 0; i < daysBetween; i++) {
                LocalDate targetDate = fromDate.plusDays(i);
                List<Appointment> appointments = appointmentService.findAppointmentsByDate(targetDate);

                for (Appointment appointment : appointments) {
                    appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
                    appointmentService.saveAppointment(appointment);
                    cancelledAppointments.add(appointment);
                    sendCancelEmail(appointment); // Email notification
                }
            }

            return ResponseEntity.ok("Leaves created for " + daysBetween + " days. " +
                    cancelledAppointments.size() + " appointments were cancelled and patients were notified.");

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("An error occurred while creating leaves.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating leaves.");
        }
    }

    private LocalDate parseFlexibleDate(String inputDate) {
        inputDate = inputDate.replace("/", "-").trim();

        DateTimeFormatter[] formats = {
                DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy-MMM-dd", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH)
        };

        for (DateTimeFormatter fmt : formats) {
            try {
                return LocalDate.parse(inputDate, fmt);
            } catch (Exception e) {
                e.printStackTrace();
                // Log silently if needed
            }
        }

        throw new IllegalArgumentException("Invalid date format: " + inputDate);
    }

    @Async
    public void sendCancelEmail(Appointment appointment) {
        try {
            if (appointment == null || appointment.getPatient() == null) {
                logger.warn("Appointment or patient data is missing.");
                return;
            }

            String email = encryptionUtil.decrypt(new String(appointment.getPatient().getEmailId()));
            String name = encryptionUtil.decrypt(new String(appointment.getPatient().getName()));
            String dateandtime=encryptionUtil.decrypt(appointment.getAppointmentDateAndTime());

            String subject = "Your Appointment Cancelled";
            String content = "Dear " + name +
                    ", your appointment on " + dateandtime +
                    " has been cancelled due to your doctor's leave. " +
                    "Please reschedule through the hospital portal or contact the front desk.";

            emailService.sendEmail(email, subject, content);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error sending cancellation email: " + e.getMessage(), e);
        }
    }



    @GetMapping("/getLeavesByDateRange")
//    @PreAuthorize("hasAuthority('ADMIN','RECEPTIONIST','DOCTOR','NURSE')")
    public ResponseEntity<?> getLeavesInRange(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            if (fromDate == null || fromDate.isEmpty()) {
                return ResponseEntity.badRequest().body("Please provide from date.");
            }

            if (toDate == null || toDate.isEmpty()) {
                return ResponseEntity.badRequest().body("Please provide to date.");
            }

            LocalDate start = parseDate(fromDate);
            LocalDate end = parseDate(toDate);

            if (start == null || end == null) {
                return ResponseEntity.badRequest().body("Invalid date format. Use 'yyyy-MM-dd', 'dd-MM-yyyy', or 'dd-MMM-yyyy'.");
            }

            if (end.isBefore(start)) {
                return ResponseEntity.badRequest().body("The to date must be the same or after the start date.");
            }

            List<Leaves> usersOnLeave = leaveService.getLeavesInRange(start, end);

            return usersOnLeave.isEmpty()
                    ? ResponseEntity.ok("No staff members on leave for this date range.")
                    : ResponseEntity.ok(usersOnLeave);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred.");
        }
    }

    // Helper method
    private LocalDate parseDate(String input) {
        if (input == null || input.isEmpty()) return null;

        input = input.replace("/", "-").trim();

        String[] parts = input.split("-");
        if (parts.length == 3) {
            parts[1] = capitalize(parts[1]);
            input = String.join("-", parts);
        }

        String[] patterns = {
                "yyyy-MM-dd", "dd-MM-yyyy", "dd-MMM-yyyy", "yyyy-MMM-dd",
                "d-MMM-yyyy", "d-MM-yyyy", "yyyy-MM-d", "yyyy-MMM-d"
        };

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                return LocalDate.parse(input, formatter);
            } catch (Exception ignored) {

                ignored.printStackTrace();
            }
        }

        return null;
    }

    private String capitalize(String input) {
        return input.length() == 0 ? input :
                input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}




