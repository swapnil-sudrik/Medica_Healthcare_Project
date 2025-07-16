package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.RoleSalaryAverageDTO;
import com.fspl.medica_healthcare.enums.BillingStatus;
import com.fspl.medica_healthcare.enums.PaymentMode;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.templets.PdfTemplate;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/bill")
@Validated
public class BillingController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private HospitalizationService hospitalizationService;

    @Autowired
    private PdfTemplate pdfTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private HospitalService hospitalService;

    private static final Logger log = LogManager.getLogger(BillingController.class);

    // =======================================================================================================================
    // UPDATED METHOD OPTIMIZED CODE AND PROPER HANDLE EXCEPTIONS
    // =======================================================================================================================
    @PostMapping("create/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> createBill(
            @PathVariable long appointmentId,
            @RequestParam("paymentMode") @NotNull PaymentMode paymentMode,
            @RequestParam(value = "dueDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {

        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            Appointment appointment = appointmentService.findAppointmentById(appointmentId);

            if (appointment == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Appointment Not Fetch",
                                "details", "An unexpected error occurred while getting appointment"
                        ));
            }

            if(!dueDate.isAfter(LocalDate.now())){
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "DueDate must be Present or in Future.."
                ));
            }

            if (appointment.getHospital().getId()!= (loginUser.getHospital().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "Access Denied",
                        "message", "You do not have permission to access this resource."
                ));
            }

            HospitalizationInfo hospitalizationInfo = appointment.getHospitalizationInfo();
            BigDecimal totalAmount = appointment.getDoctor().getStaff().getDoctorFee();

            if (hospitalizationInfo != null) {
                totalAmount = totalAmount.add(
                                new BigDecimal(hospitalizationInfo.getCatalog().getFees())
                                        .multiply(BigDecimal.valueOf(hospitalizationInfo.getTotalDaysAdmitted()))
                        ).add(hospitalizationInfo.getNursingCharges() != null ? hospitalizationInfo.getNursingCharges() : BigDecimal.ZERO)
                        .add(hospitalizationInfo.getAdditionalCharges() != null ? hospitalizationInfo.getAdditionalCharges() : BigDecimal.ZERO);
            }

//            Billing bill = billingService.getBillByAppointmentId(appointment.getId());
            Billing bill = billingService.getBillByAppointmentId(loginUser,appointment.getId());

            if (bill == null) {
                bill = new Billing();
                bill.setCreatedUser(loginUser);
                bill.setCreatedDate(LocalDate.now());
                bill.setPaidAmount(BigDecimal.ZERO);
                bill.setStatus(BillingStatus.UNPAID);
            }
            bill.setModifiedUser(loginUser);
            bill.setModifiedDate(LocalDate.now());
            bill.setAppointment(appointment);
            bill.setDoctorFee(appointment.getDoctor().getStaff().getDoctorFee());
            bill.setPaymentMode(paymentMode);
            bill.setTotalAmount(totalAmount);
            bill.setBalanceAmount(totalAmount.subtract(bill.getPaidAmount()));
            bill.setDueDate(dueDate);
            bill.setHospitalizationInfo(hospitalizationInfo);

            if (!billingService.saveBill(loginUser,bill)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Issue while creating Bill"
                ));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Bill successfully created..."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Create Bill: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }


    @PutMapping("update/{billingId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> updateBill(@PathVariable long billingId,
                                        @RequestParam("paidAmount") BigDecimal paidAmount,
                                        @RequestParam("paymentMode") PaymentMode paymentMode,
                                        @RequestParam(value = "dueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {

        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

//            Billing existingBill = billingService.getBillById(billingId);
            Billing existingBill = billingService.getBillById(loginUser,billingId);

            BigDecimal totalAmount = existingBill.getTotalAmount();
            BigDecimal prePaidAmount = existingBill.getPaidAmount() != null ? existingBill.getPaidAmount() : BigDecimal.ZERO;

            BigDecimal totalPaidAmount = prePaidAmount.add(paidAmount);

            if (totalPaidAmount.compareTo(totalAmount) > 0) {
                return ResponseEntity.badRequest().body("Paid amount is bigger than total amount");
            }

            if (totalPaidAmount.compareTo(totalAmount) != 0 && dueDate == null) {
                return ResponseEntity.badRequest().body("Due date is mandatory if bill is not full paid");
            }

            BillingStatus newStatus = (totalAmount.compareTo(totalPaidAmount) == 0) ? BillingStatus.COMPLETE
                    : (totalPaidAmount.compareTo(BigDecimal.ZERO) == 0) ? BillingStatus.UNPAID
                    : BillingStatus.PARTIALLY_PAID;

            existingBill.setModifiedUser(loginUser);
            existingBill.setModifiedDate(LocalDate.now());
            existingBill.setPaymentMode(paymentMode);
            existingBill.setPaidAmount(totalPaidAmount);
            existingBill.setBalanceAmount(totalAmount.subtract(totalPaidAmount));
            existingBill.setDueDate(newStatus == BillingStatus.COMPLETE ? null : dueDate);
            existingBill.setStatus(newStatus);

            if (!billingService.saveBill(loginUser,existingBill)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Issue while updating Bill"
                ));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Bill successfully updated..."
            ));

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Update Bill: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }


    @GetMapping("/byStatus")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillsByStatus(@RequestParam(value = "status", required = false) List<BillingStatus> status) {
        User loginUser = null;
        try {
            System.out.println("babyyyyyy");
            loginUser = userService.getAuthenticateUser();

            List<BillingStatus> searchStatuses = Optional.ofNullable(status)
                    .filter(s -> !s.isEmpty())
                    .orElse(List.of(BillingStatus.COMPLETE, BillingStatus.PARTIALLY_PAID, BillingStatus.UNPAID));

            List<Billing> bills = billingService.getBillsByStatus(loginUser, searchStatuses);

            if (bills == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Bills not fetch",
                                "details", "An unexpected error occurred while getting bills"
                        ));
            } else if (bills.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "No Bills Found For Given Status"
                ));
            }
            return ResponseEntity.ok(bills);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Status: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }

    @GetMapping("/filterByBalance")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillsByBalanceRange(
            @RequestParam(value = "minBalance", required = false) BigDecimal minBalance,
            @RequestParam(value = "maxBalance", required = false) BigDecimal maxBalance) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            BigDecimal min = Optional.ofNullable(minBalance).orElse(BigDecimal.ZERO);
            BigDecimal max = Optional.ofNullable(maxBalance).orElse(BigDecimal.valueOf(Double.MAX_VALUE));

            if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Invalid Range",
                        "details", "Balance values cannot be negative."
                ));
            }

            if (min.compareTo(max) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Invalid Range",
                        "details", "minBalance cannot be greater than maxBalance"
                ));
            }

            List<Billing> bills = billingService
                    .getBillsByBalanceRange(loginUser, min, max);

            if (bills == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error", "Bills not fetch",
                                "details", "An unexpected error occurred while getting bills"
                        ));
            } else if (bills.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No Bills Found For Given Range"
                ));
            }

            return ResponseEntity.ok(bills);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bills By Balance Range: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }


    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillById(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

//            Billing bill = billingService.getBillById(id);
            Billing bill = billingService.getBillById(loginUser,id);
            if (bill == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bill Not Found For Provided id"
                ));
            }
            return ResponseEntity.ok(bill);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Id: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getAllBills() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            List<Billing> bills = billingService.getAllBills(loginUser);

            if (bills == null) {
                log.error("Failed to retrieve all bills.");
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Bills not fetch",
                                "details", "An unexpected error occurred while getting bills"
                        ));
            } else if (bills.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bills Not Exist"
                ));
            }

            return ResponseEntity.ok(bills);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bills: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }


    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getPendingBills() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            List<Billing> pendingBills = billingService.getPendingBills(loginUser, BigDecimal.ZERO);

            if (pendingBills == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Bills not fetch",
                                "details", "An unexpected error occurred while getting bills"
                        ));
            } else if (pendingBills.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bills Not Found"
                ));
            }
            return ResponseEntity.ok(pendingBills);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Pending Bills : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }

    @GetMapping("/getByAppointment/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillByAppointmentId(@PathVariable long appointmentId) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

//            Billing bill = billingService.getBillByAppointmentId(appointmentId);
            Billing bill = billingService.getBillByAppointmentId(loginUser,appointmentId);
            if (bill == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bill Not Found For Provided id"
                ));
            }

            return ResponseEntity.ok(bill);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Appointment Id: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }

    @GetMapping("/getByPatient/{id}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillByPatientId(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
//            Billing bill = billingService.getBillsByPatientId(id);
            Billing bill = billingService.getBillsByPatientId(loginUser,id);

            if (bill == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bill Not Found For Provided id : " + id
                ));
            }

            return ResponseEntity.ok(bill);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Patient Id : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }


    @GetMapping("/byPatientName")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillByPatientName(@RequestParam("name") String name) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            List<Billing> bills = billingService.getBillByPatientName(loginUser,name);

            if (bills == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bill Not Found For Provided name"
                ));
            } else if (bills.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bills Not Exist"
                ));
            }
            return ResponseEntity.ok(bills);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Patient Name : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }

    @GetMapping("/byDoctor/{doctorId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillByDoctorId(@PathVariable long doctorId) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            // Not created api for find appointment by doctor for that reason repository is used.
            List<Appointment> appointments = appointmentService.findByDoctor_Id(doctorId);
            System.out.println(appointments.size());
            if (appointments == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "while retrieving bills"
                ));
            } else if (appointments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bills Not Exist"
                ));
            }
            Map<Patient, List<Billing>> response = new HashMap();
            for (Appointment appointment : appointments) {

                if (billingService.existsByAppointmentId(loginUser,appointment.getId())) {

//                    Billing bill = billingService.getBillByAppointmentId(appointment.getId());
                    Billing bill = billingService.getBillByAppointmentId(loginUser,appointment.getId());

                    if (bill == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                "message", "Bill Not Found For Provided id"
                        ));
                    }

                    Patient patient = bill.getAppointment().getPatient();

                    if (response.containsKey(patient)) {
                        List<Billing> billings = response.get(patient);
                        billings.add(bill);
                        response.replace(patient, billings);
                    } else {
                        List<Billing> bills = new ArrayList<>();
                        bills.add(bill);
                        response.put(patient, bills);
                    }
                }
            }
            System.out.println(response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bill By Doctor Id : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }




    @GetMapping("/getByMonthAndYear")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> getBillsByMonthAndYear(
            @RequestParam(value = "month", required = false, defaultValue = "0") int month,
            @RequestParam(value = "year", required = false, defaultValue = "0") int year) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            if (month == 0 && year == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "At least one of 'month' or 'year' must be provided."
                ));
            }

            if (month < 0 || month > 12) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid month. Please provide a value between 1 and 12."
                ));
            }

            int currentYear = LocalDate.now().getYear();
            if (year != 0 && (year < 2020 || year > currentYear))  {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid year. Please provide a value between 2020 and " + currentYear + "."
                ));
            }

            List<Billing> bills = billingService.getBillsByDate(loginUser, month, year);

            if (bills.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "No bills found for the given criteria."
                ));
            }

            return ResponseEntity.ok(bills);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Bills By Month And Year : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "An unexpected error occurred",
                    "details", e.getMessage()
            ));
        }
    }



    @GetMapping("/getByDateRange")
    public ResponseEntity<?> getBillsByDateOrDateRange(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            if (startDate == null) {
                startDate = (endDate != null)
                        ? billingService.getEarliestBillingDate(loginUser)
                        : LocalDate.now();
            }

            if (endDate == null) {
                endDate = LocalDate.now();
            }

            if (startDate.isAfter(LocalDate.now())){
                return ResponseEntity.badRequest().body("Start date must be before current date");
            }

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Start date cannot be after end date."
                ));
            }

            List<Billing> bills = billingService.getBillByDateRange(loginUser, startDate, endDate);

            if (bills == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "while retrieving bills"));
            } else if (bills.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Bills Not Exist"));
            }
            return ResponseEntity.ok(bills);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Bills By Date Range : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }

    }


    @PostMapping("/send/{billingId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> sendBillByEmail(@PathVariable long billingId, @RequestParam(defaultValue = "false") boolean isReminder) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
//            Billing bill = billingService.getBillById(billingId);

            Billing bill = billingService.getBillById(loginUser,billingId);

            if (bill == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "Bill Not Found For Id: " + billingId
                ));
            }

            String recipientEmail = Optional.ofNullable(bill.getAppointment())
                    .map(Appointment::getPatient)
                    .map(Patient::getEmailId)
                    .orElse(null);

            if (recipientEmail == null || recipientEmail.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "message", "No valid email found for the patient."
                ));
            }

            byte[] pdfBytes = pdfTemplate.createBillAsBytes(bill);

            if (pdfBytes == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "Pdf Generation Failed"
                ));
            }


//            log.debug("Starting asynchronous email sending for bill ID: "+billingId+" to:"+ recipientEmail);
            CompletableFuture.supplyAsync(() -> {
                try {
                    if (bill == null || recipientEmail == null || recipientEmail.isBlank() || pdfBytes == null || pdfBytes.length == 0) {
                        return "Invalid input: Billing details, recipient email, or PDF content is missing.";
                    }

                    String subject = isReminder ? "Your Due Bill" : "Your Hospital Bill";
                    String htmlContent = isReminder ? getHospitalBillReminderTemplate(bill) : getHospitalBillTemplate(bill);

//                    log.info("Sending email for bill ID: "+billingId+" to: "+recipientEmail+" (Subject: "+subject+")");

                    return emailService.sendEmailWithAttachmentForBill(recipientEmail, subject, htmlContent, pdfBytes, "Bill.pdf").get();

                } catch (Exception e) {
                    log.error("Email sending failed for bill ID: "+billingId+". error : "+ e.getMessage());
                    e.printStackTrace();
                    return "Failed to send email : " + e.getMessage();
                }
            }).thenAccept(result -> {
                if (!result.equalsIgnoreCase("Success")) {
//                    log.info("Email sent successfully for bill ID : "+ billingId);
//                } else {
                    log.error("Failed to send email for bill ID : "+billingId+" result : "+ result);
                }
            });

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=bill.pdf")
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Sending Mail : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Failed to process request",
                    "details", e.getMessage()
            ));
        }
    }



    @GetMapping("/turnover")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getHospitalTurnoverByHospitalId() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getId());

            Double totalRevenue = billingService.getTotalRevenueByHospitalId(loginUser);
            Double totalExpenses = staffService.getTotalSalaryExpenses(loginUser);
            Double netTurnover = totalRevenue - totalExpenses;

            Map<String, Object> turnoverData = new HashMap<>();
            turnoverData.put("totalRevenue", totalRevenue);
            turnoverData.put("totalExpenses", totalExpenses);
            turnoverData.put("netTurnover", netTurnover);
            turnoverData.put("hospital", hospital);
            return ResponseEntity.ok(turnoverData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Hospital Turnover By Hospital Id : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }


    @GetMapping("/report")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getHospitalReport() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            long hospitalId = loginUser.getHospital().getId();

            Long staffCount = staffService.getCountOfStaff(loginUser);
            List<RoleSalaryAverageDTO> roleSalaryAverageDTO = staffService.getAverageSalaryByRole(loginUser);
            Double totalRevenue = billingService.getTotalRevenueByHospitalId(loginUser);
            Double totalExpenses = staffService.getTotalSalaryExpenses(loginUser);

            Double netProfit = totalRevenue - totalExpenses;

            Map<String, Object> report = new HashMap<>();
            report.put("totalStaff", staffCount);
            report.put("roles", roleSalaryAverageDTO);
            report.put("totalExpenses", totalExpenses);
            report.put("totalRevenue", totalRevenue);
            report.put("totalProfit", netProfit);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Hospital Report : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }


    @GetMapping("/report/monthYear")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getRevenue(
            @RequestParam(value = "month", required = false,defaultValue = "0") int month,
            @RequestParam(value = "year", required = false,defaultValue = "0") int year) {
        User loginUser = null;
        try {
            if (month == 0 && year == 0) {
                return ResponseEntity.badRequest().body(ExceptionMessages.MONTH_YEAR_NOT_FOUND);
            }

            if (month !=0 && (month < 1 || month > 12)) {
                return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_MONTH);
            }

            int currentYear = LocalDate.now().getYear();
            if (year != 0 && (year < 2020 || year > currentYear)) {
                return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_YEAR);
            }


            loginUser = userService.getAuthenticateUser();
            long hospitalId = loginUser.getHospital().getId();

            long staffCount = staffService.getNumberOfStaffByHospitalIdAndDate(loginUser, month, year);
            List<RoleSalaryAverageDTO> roleSalaryAverageDTO = staffService.findAverageSalaryByRoleAndDate(loginUser, month, year);
            Double totalRevenue = billingService.getTotalRevenueByHospitalIdAndDate(loginUser, month, year);
            Double totalExpenses = staffService.getTotalSalaryExpensesByHospitalIdAndDate(loginUser, month, year);

            Double netProfit = totalRevenue - totalExpenses;

            Map<String, Object> report = new HashMap<>();
            report.put("totalStaff", staffCount);
            report.put("roles", roleSalaryAverageDTO);
            report.put("totalExpenses", totalExpenses);
            report.put("totalRevenue", totalRevenue);
            report.put("totalProfit", netProfit);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Get Revenue: \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }


    //    @Scheduled(fixedRate = 10000000)
    @Scheduled(cron = "0 0 9 * * ?")
    public void getDueBills() {

        User loginUser = userService.getAuthenticateUser();
        List<Billing> bills = billingService.getDueBills(loginUser);

        if (bills == null || bills.isEmpty()) {
//            log.warn("No Bills Found for scheduling");
            System.out.println("No due bills found.");
            return;
        }

        bills.stream()
                .filter(bill -> bill != null)
                .forEach(bill -> {
                    try {
                        this.sendBillByEmail(bill.getId(), true);
                        System.out.println("Email sent to " + bill.getAppointment().getPatient().getName());
                    } catch (Exception e) {
                        System.err.println("Failed to send email for Bill ID: " + bill.getId());
                        e.printStackTrace();
                        log.error("An unexpected error occurred while Fetch Due Bills : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser.getId());
                    }
                });
    }

    public String getHospitalBillReminderTemplate(Billing bill) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f9f9f9;" +
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
                "    background: linear-gradient(135deg, #66bb6a, #2e7d32);" +
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
                ".body p {" +
                "    margin: 10px 0;" +
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
                "Hospital Bill Reminder - " + bill.getAppointment().getHospital().getName() +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear " + bill.getAppointment().getPatient().getName() + ",</p>" +
                "<p>We hope this message finds you well. We are writing to inform you that your hospital bill is due soon. Kindly ensure that the payment is made before the due date of <strong>" + bill.getDueDate() + "</strong> to avoid any delays or penalties.</p>" +
                "<p><strong>Bill Details:</strong></p>" +
                "<ul>" +
                "    <li><strong>Bill Number:</strong> " + bill.getId() + "</li>" +
                "    <li><strong>Bill Due Date:</strong> " + bill.getDueDate() + "</li>" +
                "    <li><strong>Total Amount Due:</strong> " + NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(bill.getTotalAmount()) + "</li>" +
                "</ul>" +
                "<p>To make your payment, please refer to the attached invoice for payment instructions. If you have already made the payment, please disregard this notice. Should you have any questions or require assistance, feel free to contact our billing department.</p>" +
                "<p>We greatly appreciate your timely payment and cooperation. Thank you for trusting " + bill.getAppointment().getHospital().getName() + " for your healthcare needs. We wish you continued good health.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; " + java.time.Year.now() + " " + bill.getAppointment().getHospital().getName() + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    public String getHospitalBillTemplate(Billing bill) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "    font-family: Arial, sans-serif;" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    background-color: #f9f9f9;" +
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
                "    background: linear-gradient(135deg, #66bb6a, #2e7d32);" +
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
                ".body p {" +
                "    margin: 10px 0;" +
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
                "Hospital Bill Notification - " + bill.getAppointment().getHospital().getName() +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear " + bill.getAppointment().getPatient().getName() + ",</p>" +
                "<p>We hope this message finds you well. This is to notify you that your hospital bill has been generated and is attached to this email for your reference.</p>" +
                "<p><strong>Bill Details:</strong></p>" +
                "<ul>" +
                "    <li><strong>Bill Number:</strong> " + bill.getId() + "</li>" +
                "    <li><strong>Bill Date:</strong> " + bill.getCreatedDate().format(DateTimeFormatter.ofPattern("MMMM, d , yyyy")).toString() + "</li>" +
                "    <li><strong>Total Amount:</strong> " + NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(bill.getTotalAmount()) + "</li>" +
                "</ul>" +
                "<p>Please review the attached invoice for detailed charges. Should you have any questions or need assistance, feel free to contact our billing department.</p>" +
                "<p>Thank you for choosing " + bill.getAppointment().getHospital().getName() + ". We wish you good health and a speedy recovery!</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; " + java.time.Year.now() + " " + bill.getAppointment().getHospital().getName() + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }



}
