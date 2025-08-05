package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.*;
import com.fspl.medica_healthcare.enums.AppointmentStatus;
import com.fspl.medica_healthcare.enums.InvoiceStatus;
import com.fspl.medica_healthcare.enums.PaymentMode;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.templets.PdfTemplate;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
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
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/invoice")
@Validated
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private HospitalizationService hospitalizationService;

    @Autowired
    private DepositService depositService;

    @Autowired
    private PdfTemplate pdfTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private EncryptionUtil util;



//    private static final Logger log = LogManager.getLogger(InvoiceController.class);

    private static final Logger log = Logger.getLogger(InvoiceController.class);


    // =======================================================================================================================
    // UPDATED METHOD OPTIMIZED CODE AND PROPER HANDLE EXCEPTIONS
    // =======================================================================================================================


    @PostMapping("generateInvoice/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> generateInvoice(@PathVariable long appointmentId,
                                                          @Valid @ModelAttribute InsuranceDTO insuranceDTO) {

        User loginUser = null;
        try {
            // Retrieve the user who is currently logged in.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            // Find the appointment using the appointment ID.
            Appointment appointment = appointmentService.findAppointmentById(appointmentId);

            // Check if the appointment is found. If not, return.
            if (appointment == null || appointment.getHospital() == null) {
                return ResponseEntity.badRequest().body("Appointment or Hospital Not Found!!");
            }

            // Make sure the appointment is for the same hospital as the logged-in user.
            if (appointment.getHospital().getId() != (loginUser.getHospital().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to access this resource.");
            }

            if (!appointment.getAppointmentStatus().equals(AppointmentStatus.COMPLETED)) {
                return ResponseEntity.badRequest().body("Appointment is not complete");
            }

            // Get the hospitalization details for the appointment, if any.
            HospitalizationInfo hospitalizationInfo = appointment.getHospitalizationInfo();

            // Start with the doctor's fee as the initial total amount.
            double totalAmount = appointment.getDoctor().getStaff().getDoctorFee();
            double allDepositAmount = 0;
            List<Deposit> allDeposit = null;

            // If there is hospitalization, add those costs to the total.
            if (hospitalizationInfo != null) {
                totalAmount = totalAmount + (hospitalizationInfo.getCatalog().getFees() * hospitalizationInfo.getTotalDaysAdmitted())
                        + hospitalizationInfo.getNursingCharges()
                        + hospitalizationInfo.getAdditionalCharges()
                        + hospitalizationInfo.getCanteenCharges();

                allDeposit = depositService.getAllDepositByAppointmentId(loginUser, appointmentId);

                if (allDeposit != null || !allDeposit.isEmpty()) {
                    for (Deposit deposit : allDeposit) {
                        allDepositAmount = allDepositAmount + deposit.getDepositAmount();
                    }
                }
            }

            // Check if a invoice already exists for this appointment.
            Invoice invoice = invoiceService.getInvoiceByAppointmentId(loginUser.getHospital().getId(), loginUser.getBranch(), appointment.getId());

            // If no invoice exists, create a new one.
            if (invoice == null) {
                invoice = new Invoice();
                invoice.setCreatedUser(loginUser);
                invoice.setCreatedDate(LocalDate.now());
                invoice.setPaidAmount(allDepositAmount);
                invoice.setStatus(InvoiceStatus.UNPAID);
                invoice.setPaymentMode(PaymentMode.UNDEFINED);
                invoice.setDueDate(LocalDate.now().plusDays(10));
            }

            // check the hospitalization if present or not for adding insurance deltails
            if (hospitalizationInfo != null) {
                // check the payement mode is 'INSURANCE' if true set the insurance details
                if (insuranceDTO.getPaymentMode() != null && insuranceDTO.getPaymentMode().equals(PaymentMode.INSURANCE)) {
                    if(insuranceDTO.getPolicyNumber() <=0){
                        return ResponseEntity.badRequest().body("please provide valid policy number!!");
                    }
                    if(insuranceDTO.getPolicyCompanyName()==null || insuranceDTO.getPolicyCompanyName().trim().isEmpty()){
                        return ResponseEntity.badRequest().body("please provide valid policy company name!!");
                    }
                    if(insuranceDTO.getPolicyStatus() == null){
                        return ResponseEntity.badRequest().body("please provide valid policy status!!");
                    }
                    if(insuranceDTO.getPolicyAmount() <=0){
                        return ResponseEntity.badRequest().body("please provide valid policy amount!!");
                    }
                    invoice.setPolicyNumber(insuranceDTO.getPolicyNumber());
                    invoice.setPolicyCompanyName(insuranceDTO.getPolicyCompanyName());
                    invoice.setPolicyStatus(insuranceDTO.getPolicyStatus());
                    invoice.setPolicyAmount(insuranceDTO.getPolicyAmount());
                }
            } else {
                if (insuranceDTO.getQuickCareCharge() >= 0) {
                    invoice.setQuickCareCharges(insuranceDTO.getQuickCareCharge());
                } else {
                    return ResponseEntity.badRequest().body("QuickCharges can't be negative");
                }
            }

            totalAmount+=invoice.getQuickCareCharges();

            // Update the invoice details.
            invoice.setModifiedUser(loginUser);
            invoice.setModifiedDate(LocalDate.now());
            invoice.setAppointment(appointment);
            invoice.setDoctorFee(appointment.getDoctor().getStaff().getDoctorFee());
            invoice.setTotalAmount(totalAmount);
            invoice.setBalanceAmount(totalAmount - (invoice.getPaidAmount() + allDepositAmount));
            invoice.setHospitalizationInfo(hospitalizationInfo);

            // Update the invoice status based on the total amount and the amount paid.
            InvoiceStatus newStatus = (totalAmount == invoice.getPaidAmount()) ? InvoiceStatus.COMPLETE
                    : (invoice.getPaidAmount() == 0.0) ? InvoiceStatus.UNPAID
                    : InvoiceStatus.PARTIALLY_PAID;

            invoice.setStatus(newStatus);

            // Save the invoice and handle any errors.
            if (!invoiceService.saveInvoice(loginUser, invoice)) {
                return ResponseEntity.badRequest().body("Issue while creating Invoice");
            }

            Invoice savedInvoice = invoiceService.getInvoiceByAppointmentId(loginUser.getHospital().getId(), loginUser.getBranch(), appointmentId);
//            byte[] pdfBytes = pdfTemplate.createInvoiceAsBytes(savedInvoice);
            byte[] pdfBytes = pdfTemplate.createInvoiceAsBytes(savedInvoice, allDeposit);

            // If PDF generation failed, return.
            if (pdfBytes == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pdf Generation Failed");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice.pdf")
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Create Invoice: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }


    @PutMapping("updateInvoice/{invoiceId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> updateInvoice(@PathVariable long invoiceId,
                                                        @Valid @ModelAttribute InsuranceDTO insuranceDTO) {

        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            if (insuranceDTO.getDueDate() != null && insuranceDTO.getDueDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body("Due date cannot be in the past. Please select today's date or a future date.");
            }

            // Find the existing invoice using the invoice ID.
            Invoice existingInvoice = invoiceService.getInvoiceById(loginUser.getHospital().getId(), loginUser.getBranch(), invoiceId);

            // Get the total amount and the amount already paid.
            double totalAmount = existingInvoice.getTotalAmount();
            double prePaidAmount = existingInvoice.getPaidAmount();
            double quickCareCharge = existingInvoice.getQuickCareCharges();

            totalAmount+=quickCareCharge;
            // Calculate the new total paid amount.
            double totalPaidAmount = prePaidAmount + insuranceDTO.getPaidAmount();

            // Check if the paid amount is greater than the total amount.
            if (totalPaidAmount > totalAmount) {
                return ResponseEntity.badRequest().body("Paid amount is bigger than total amount");
            }

            // If the invoice isn't fully paid, a due date is required.
            if (!(totalPaidAmount == totalAmount) && insuranceDTO.getDueDate() == null) {
                return ResponseEntity.badRequest().body("Due date is mandatory if invoice is not full paid");
            }

            // Update the invoice status based on the total amount and the amount paid.
            InvoiceStatus newStatus = (totalAmount == totalPaidAmount) ? InvoiceStatus.COMPLETE
                    : (totalPaidAmount == 0.0) ? InvoiceStatus.UNPAID
                    : InvoiceStatus.PARTIALLY_PAID;

            HospitalizationInfo hospitalizationInfo = existingInvoice.getHospitalizationInfo();

            // check the hospitalization if present or not for adding insurance deltails
            if (hospitalizationInfo != null) {
                // check the payement mode is 'INSURANCE' if true set the insurance details
                if (insuranceDTO.getPaymentMode() != null && insuranceDTO.getPaymentMode().equals(PaymentMode.INSURANCE)) {
                    if(insuranceDTO.getPolicyNumber() <=0){
                        return ResponseEntity.badRequest().body("please provide valid policy number!!");
                    }
                    if(insuranceDTO.getPolicyCompanyName()==null || insuranceDTO.getPolicyCompanyName().trim().isEmpty()){
                        return ResponseEntity.badRequest().body("please provide valid policy company name!!");
                    }
                    if(insuranceDTO.getPolicyStatus() == null){
                        return ResponseEntity.badRequest().body("please provide valid policy status!!");
                    }
                    if(insuranceDTO.getPolicyAmount() <=0){
                        return ResponseEntity.badRequest().body("please provide valid policy amount!!");
                    }
                    existingInvoice.setPolicyNumber(insuranceDTO.getPolicyNumber());
                    existingInvoice.setPolicyCompanyName(insuranceDTO.getPolicyCompanyName());
                    existingInvoice.setPolicyStatus(insuranceDTO.getPolicyStatus());
                    existingInvoice.setPolicyAmount(insuranceDTO.getPolicyAmount());
                }
            } else {
                if (insuranceDTO.getQuickCareCharge() >= 0) {
                    existingInvoice.setQuickCareCharges(insuranceDTO.getQuickCareCharge());
                } else {
                    return ResponseEntity.badRequest().body("otherCharges can't be negative");
                }
            }

            // Update the invoice details.
            existingInvoice.setModifiedUser(loginUser);
            existingInvoice.setModifiedDate(LocalDate.now());
            if(!insuranceDTO.getPaymentMode().equals(PaymentMode.INSURANCE)){
                existingInvoice.setPaymentMode(insuranceDTO.getPaymentMode() != null ? insuranceDTO.getPaymentMode() : PaymentMode.UNDEFINED);
            }
            existingInvoice.setPaidAmount(totalPaidAmount);
            existingInvoice.setBalanceAmount(totalAmount - totalPaidAmount);
            existingInvoice.setDueDate(newStatus.equals(InvoiceStatus.COMPLETE) ? null : insuranceDTO.getDueDate());
            existingInvoice.setStatus(newStatus);

            // Save the updated invoice and handle if any errors.
            if (!invoiceService.saveInvoice(loginUser, existingInvoice)) {
                return ResponseEntity.badRequest().body("Issue while updating Invoice");
            }

            // Return a success message.
            return ResponseEntity.status(HttpStatus.CREATED).body("Invoice successfully updated...");

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Update Invoice: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }




//    @PutMapping("updateInvoice/{invoiceId}")
//    @PreAuthorize("hasAuthority('RECEPTIONIST')")
//    public synchronized ResponseEntity<?> updateInvoice(@PathVariable long invoiceId,
//                                                        @RequestParam("paidAmount") double paidAmount,
//                                                        @RequestParam("paymentMode") PaymentMode paymentMode,
//                                                        @RequestParam(value = "dueDate", required = false)
//                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
//
//        User loginUser = null;
//        try {
//            // Retrieve the logged-in user.
//            loginUser = userService.getAuthenticateUser();
//
//            if (loginUser == null || loginUser.getHospital() == null) {
//                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
//            }
//
//            if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
//                return ResponseEntity.badRequest().body("Due date cannot be in the past. Please select today's date or a future date.");
//            }
//
//            // Find the existing invoice using the invoice ID.
//            Invoice existingInvoice = invoiceService.getInvoiceById(loginUser.getHospital().getId(), loginUser.getBranch(), invoiceId);
//
//            // Get the total amount and the amount already paid.
//            double totalAmount = existingInvoice.getTotalAmount();
//            double prePaidAmount = existingInvoice.getPaidAmount();
//
//            // Calculate the new total paid amount.
//            double totalPaidAmount = prePaidAmount + paidAmount;
//
//            // Check if the paid amount is greater than the total amount.
//            if (totalPaidAmount > totalAmount) {
//                return ResponseEntity.badRequest().body("Paid amount is bigger than total amount");
//            }
//
//            // If the invoice isn't fully paid, a due date is required.
//            if ((totalPaidAmount == totalAmount) && dueDate == null) {
//                return ResponseEntity.badRequest().body("Due date is mandatory if invoice is not full paid");
//            }
//
//            // Update the invoice status based on the total amount and the amount paid.
//            InvoiceStatus newStatus = (totalAmount == totalPaidAmount) ? InvoiceStatus.COMPLETE
//                    : (totalPaidAmount == 0.0) ? InvoiceStatus.UNPAID
//                    : InvoiceStatus.PARTIALLY_PAID;
//
//            // Update the invoice details.
//            existingInvoice.setModifiedUser(loginUser);
//            existingInvoice.setModifiedDate(LocalDate.now());
//            existingInvoice.setPaymentMode(paymentMode != null ? paymentMode : PaymentMode.UNDEFINED);
//            existingInvoice.setPaidAmount(totalPaidAmount);
//            existingInvoice.setBalanceAmount(totalAmount - totalPaidAmount);
//            existingInvoice.setDueDate(newStatus.equals(InvoiceStatus.COMPLETE) ? null : dueDate);
//            existingInvoice.setStatus(newStatus);
//
//            // Save the updated invoice and handle if any errors.
//            if (!invoiceService.saveInvoice(loginUser, existingInvoice)) {
//                return ResponseEntity.badRequest().body("Issue while updating Invoice");
//            }
//
//            // Return a success message.
//            return ResponseEntity.status(HttpStatus.CREATED).body("Invoice successfully updated...");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("An unexpected error occurred while Update Invoice: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
//            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
//        }
//    }


    @GetMapping("/getInvoiceByStatus")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceByStatus(@RequestParam InvoiceStatus status) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            // check if status is provided or not
            if (status == null) {
                return ResponseEntity.badRequest().body("please provide valid status");
            }


            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();
            List<Invoice> invoices = new ArrayList<>();

            for (String branch : branchList) {
                // Get the invoice based on the selected statuses.
                List<Invoice> invoice = invoiceService.getInvoiceByStatus(loginUser.getHospital().getId(), branch.getBytes(), status);
                invoices.addAll(invoice);
            }

            // Handle cases where no invoice were null or empty.
            if (invoices == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            } else if (invoices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Invoice Found For Given Status");
            }

            List<InvoiceResponseDTO> dtos = new ArrayList<>();

            for (Invoice invoiceBill : invoices) {
                List<Deposit> allDeposit = new ArrayList<>();

                // Get the Deposit by appointment Id
                if (invoiceBill.getHospitalizationInfo() != null) {
                    allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                    // Handle cases where the invoice is null.
                    if (allDeposit == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                    } else if (allDeposit.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                    }
                }
                dtos.add(this.convertToInvoiceDTO(invoiceBill, allDeposit));
            }

            return ResponseEntity.ok().body(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Status: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }

    @GetMapping("/getInvoiceByBalanceRange")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceByBalanceRange(
            @RequestParam(defaultValue = "0.0") double minBalance,
            @RequestParam(defaultValue = "1.7976931348623157E308") double maxBalance) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            if (minBalance == 0 && maxBalance == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("please provide minBalance or maxBalance");
            }

            if (minBalance < 0 || maxBalance < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Balance values cannot be negative.");
            }

            if (minBalance > maxBalance) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("minBalance cannot be greater than maxBalance");
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();
            List<Invoice> invoices = new ArrayList<>();

            for (String branch : branchList) {
                // Get the invoice based on the selected statuses.
                List<Invoice> invoice = invoiceService.getInvoiceByBalanceRange(loginUser.getHospital().getId(), branch.getBytes(), minBalance, maxBalance);
                if (invoice != null) invoices.addAll(invoice);
            }

            // Handle cases where no invoice were null or empty.
            if (invoices == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            } else if (invoices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Invoice Found For Given Status");
            }

            List<InvoiceResponseDTO> dtos = new ArrayList<>();

            for (Invoice invoiceBill : invoices) {
                List<Deposit> allDeposit = new ArrayList<>();

                // Get the Deposit by appointment Id
                if (invoiceBill.getHospitalizationInfo() != null) {
                    allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                    // Handle cases where the invoice is null.
                    if (allDeposit == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                    } else if (allDeposit.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                    }
                }
                dtos.add(this.convertToInvoiceDTO(invoiceBill, allDeposit));
            }

            return ResponseEntity.ok().body(dtos);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Balance Range: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }


    @GetMapping("/getInvoiceById/{id}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceById(@PathVariable long id) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            Invoice getInvoice = null;
            for (String branch : branchList) {
                // Get the invoice based on the selected statuses.
                getInvoice = invoiceService.getInvoiceById(loginUser.getHospital().getId(), branch.getBytes(), id);
                if (getInvoice != null) {
                    break;
                }
            }

            // Handle cases where no invoice were null or empty.
            if (getInvoice == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            }

            List<Deposit> allDeposit = new ArrayList<>();

            // Get the Deposit by appointment Id
            if (getInvoice.getHospitalizationInfo() != null) {
                allDeposit = depositService.getAllDepositByAppointmentId(loginUser, getInvoice.getAppointment().getId());

                // Handle cases where the invoice is null.
                if (allDeposit == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                } else if (allDeposit.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                }
            }
            InvoiceResponseDTO dto = this.convertToInvoiceDTO(getInvoice, allDeposit);

            return ResponseEntity.ok().body(dto);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Id: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }

    @GetMapping("/getAllInvoice")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getAllInvoice() {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }


            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();
            List<Invoice> invoices = new ArrayList<>();

            for (String branch : branchList) {
                // Get all invoice.
                List<Invoice> invoice = invoiceService.getAllInvoice(loginUser.getHospital().getId(), branch.getBytes());
                invoices.addAll(invoice);
            }

            // Handle cases where no invoice were null or empty.
            if (invoices == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            } else if (invoices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Invoice Found For Given Status");
            }

            List<InvoiceResponseDTO> dtos = new ArrayList<>();

            for (Invoice invoiceBill : invoices) {
                List<Deposit> allDeposit = new ArrayList<>();

                // Get the Deposit by appointment Id
                if (invoiceBill.getHospitalizationInfo() != null) {
                    allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                    // Handle cases where the invoice is null.
                    if (allDeposit == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                    } else if (allDeposit.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                    }
                }
                dtos.add(this.convertToInvoiceDTO(invoiceBill, allDeposit));
            }

            return ResponseEntity.ok().body(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }

    @GetMapping("/getInvoiceByAppointmentId/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceByAppointmentId(@PathVariable long appointmentId) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            Invoice getInvoice = null;
            for (String branch : branchList) {
                // Get the invoice based on the selected statuses.
                getInvoice = invoiceService.getInvoiceByAppointmentId(loginUser.getHospital().getId(), branch.getBytes(), appointmentId);
                if (getInvoice != null) {
                    break;
                }
            }

            // Handle cases where no invoice were null or empty.
            if (getInvoice == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            }

            List<Deposit> allDeposit = new ArrayList<>();

            // Get the Deposit by appointment Id
            if (getInvoice.getHospitalizationInfo() != null) {
                allDeposit = depositService.getAllDepositByAppointmentId(loginUser, getInvoice.getAppointment().getId());

                // Handle cases where the invoice is null.
                if (allDeposit == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                } else if (allDeposit.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                }
            }
            InvoiceResponseDTO dto = this.convertToInvoiceDTO(getInvoice, allDeposit);

            return ResponseEntity.ok().body(dto);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Appointment Id: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }

    @GetMapping("/getByPatient/{id}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceByPatientId(@PathVariable long id) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            Invoice getInvoice = null;
            for (String branch : branchList) {
                // Get the invoice based on the selected statuses.
                getInvoice = invoiceService.getInvoiceByPatientId(loginUser.getHospital().getId(), branch.getBytes(), id);
                if (getInvoice != null) {
                    break;
                }
            }

            // Handle cases where no invoice were null or empty.
            if (getInvoice == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            }

            List<Deposit> allDeposit = new ArrayList<>();

            // Get the Deposit by appointment Id
            if (getInvoice.getHospitalizationInfo() != null) {
                allDeposit = depositService.getAllDepositByAppointmentId(loginUser, getInvoice.getAppointment().getId());

                // Handle cases where the invoice is null.
                if (allDeposit == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                } else if (allDeposit.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                }
            }
            InvoiceResponseDTO dto = this.convertToInvoiceDTO(getInvoice, allDeposit);

            // Return the invoice.
            return ResponseEntity.ok().body(dto);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Patient Id : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }


    @GetMapping("/getInvoiceByPatientName")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceByPatientName(@RequestParam("name") String name) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();
            List<Invoice> invoices = new ArrayList<>();

            for (String branch : branchList) {
                // Get the invoice associated with the patient name.
                List<Invoice> getInvoice = invoiceService.getInvoiceByPatientName(loginUser.getHospital().getId(), branch.getBytes(), util.encrypt(name).getBytes());
                invoices.addAll(getInvoice);
            }

            // Handle cases where no invoice were null or empty.
            if (invoices == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            } else if (invoices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Invoice Found For Given Status");
            }

            List<InvoiceResponseDTO> dtos = new ArrayList<>();

            for (Invoice invoiceBill : invoices) {

//                String name = invoiceBill.getAppointment().getPatient().getName();

                List<Deposit> allDeposit = new ArrayList<>();

                // Get the Deposit by appointment Id
                if (invoiceBill.getHospitalizationInfo() != null) {
                    allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                    // Handle cases where the invoice is null.
                    if (allDeposit == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                    } else if (allDeposit.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                    }
                }
                dtos.add(this.convertToInvoiceDTO(invoiceBill, allDeposit));
            }

            return ResponseEntity.ok().body(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Patient Name : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }

    @GetMapping("/getInvoiceByDoctorId/{doctorId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceByDoctorId(@PathVariable long doctorId) {
        User loginUser = null;
        try {

            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            // Find all appointments associated with the doctor ID.
            List<Appointment> appointments = appointmentService.findByDoctor_Id(doctorId);

            // If no appointments were found, return a message.
            if (appointments.isEmpty()) {
                return ResponseEntity.ok("No appointments found for this doctor.");
            }

            // Create a map to store invoice grouped by patient ID.
            Map<Long, List<InvoiceResponseDTO>> response = new HashMap<>();

            // Iterate through the appointments and retrieve the associated invoice.
            for (Appointment appointment : appointments) {

                List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                        .map(String::trim)
                        .toList();

                List<Invoice> invoices = new ArrayList<>();

                for (String branch : branchList) {
                    // Get all invoice.
                    Invoice getInvoice = invoiceService.getInvoiceByAppointmentId(loginUser.getHospital().getId(), branch.getBytes(), appointment.getId());
                    if (getInvoice != null) invoices.add(getInvoice);
                }

                // Handle cases where no invoice were null or empty.
                if (invoices == null) {
                    return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
                } else if (invoices.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Invoice Found For Given Status");
                }

                for (Invoice invoiceBill : invoices) {
                    List<Deposit> allDeposit = new ArrayList<>();

                    // Get the Deposit by appointment Id
                    if (invoiceBill.getHospitalizationInfo() != null) {
                        allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                        // Handle cases where the invoice is null.
                        if (allDeposit == null) {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                        } else if (allDeposit.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                        }
                    }
                    InvoiceResponseDTO dto = this.convertToInvoiceDTO(invoiceBill, allDeposit);
                    Long patientId = invoiceBill.getAppointment().getPatient().getId();
                    response.computeIfAbsent(patientId, k -> new ArrayList<>()).add(dto);
                }
            }

            // If no invoice found, return a message.
            if (response.isEmpty()) {
                return ResponseEntity.ok("No invoice found for this doctor.");
            }

            // Return the map of invoice.
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Doctor Id : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }


    @GetMapping("/getInvoiceByMonthAndYear")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> getInvoiceByMonthAndYear(
            @RequestParam(value = "month", required = false, defaultValue = "0") int month,
            @RequestParam(value = "year", required = false, defaultValue = "0") int year) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            // Validate the month and year parameters.
            if (month == 0 && year == 0) {
                return ResponseEntity.badRequest().body("At least one of 'month' or 'year' must be provided.");
            }

            if (month != 0 && (month < 1 || month > 12)) {
                return ResponseEntity.badRequest().body("Invalid month. Please provide a value between 1 and 12.");
            }

            int currentYear = LocalDate.now().getYear();
            if (year != 0 && (year < 2020 || year > currentYear)) {
                return ResponseEntity.badRequest().body("Invalid year. Please provide a value between 2020 and " + currentYear + ".");
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();
            List<Invoice> invoices = new ArrayList<>();

            for (String branch : branchList) {
                // Get the invoice based on the provided month and year.
                List<Invoice> getInvoice = invoiceService.getInvoiceByMonthAndYear(loginUser.getHospital().getId(), branch.getBytes(), month, year);
                invoices.addAll(getInvoice);
            }

            // Handle cases where no invoice were null or empty.
            if (invoices == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            } else if (invoices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Invoice Found For Given Status");
            }

            List<InvoiceResponseDTO> dtos = new ArrayList<>();

            for (Invoice invoiceBill : invoices) {
                List<Deposit> allDeposit = new ArrayList<>();

                // Get the Deposit by appointment Id
                if (invoiceBill.getHospitalizationInfo() != null) {
                    allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                    // Handle cases where the invoice is null.
                    if (allDeposit == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                    } else if (allDeposit.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                    }
                }
                dtos.add(this.convertToInvoiceDTO(invoiceBill, allDeposit));
            }

            // If no invoice were found, return a message.
            if (dtos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No invoice found for the given criteria.");
            }

            return ResponseEntity.ok().body(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Invoice By Month And Year : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }


    @GetMapping("/getInvoiceByDateRange")
    public synchronized ResponseEntity<?> getInvoiceByDateRange(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            // Check if both startDate and endDate are not provided.
            if (startDate == null && endDate == null) {
                return ResponseEntity.badRequest().body("At least one date (startDate or endDate) must be provided.");
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            LocalDate earliestDate = null;
            for (String branch : branchList) {
                LocalDate temp = invoiceService.getEarliestInvoiceDate(loginUser.getHospital().getId(), branch.getBytes());
                if (temp != null) {
                    earliestDate = temp;
                }
            }

            // set the start and end dates for the search if not provided.
            if (startDate == null) {
                startDate = (endDate != null)
                        ? earliestDate
                        : LocalDate.now();
            }

            if (endDate == null) {
                endDate = LocalDate.now();
            }

            // start date must be in the past.
            if (startDate.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body("Start date must be before current date");
            }

            // start date is not after end date
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().body("Start date cannot be after end date.");
            }

            List<Invoice> invoices = new ArrayList<>();

            for (String branch : branchList) {
                // Get the invoice within the specified date range.
                List<Invoice> invoice = invoiceService.getInvoiceByDateRange(loginUser.getHospital().getId(), branch.getBytes(), startDate, endDate);
                invoices.addAll(invoice);
            }

            // Handle cases where no invoice were null or empty.
            if (invoices == null) {
                return ResponseEntity.badRequest().body("An unexpected error occurred while getting invoice");
            } else if (invoices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Invoice Found For Given Status");
            }

            List<InvoiceResponseDTO> dtos = new ArrayList<>();

            for (Invoice invoiceBill : invoices) {
                List<Deposit> allDeposit = new ArrayList<>();

                // Get the Deposit by appointment Id
                if (invoiceBill.getHospitalizationInfo() != null) {
                    allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                    // Handle cases where the invoice is null.
                    if (allDeposit == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                    } else if (allDeposit.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deposits are empty");
                    }
                }
                dtos.add(this.convertToInvoiceDTO(invoiceBill, allDeposit));
            }

            return ResponseEntity.ok().body(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Invoice By Date Range : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }

    }


    @PostMapping("/sendInvoiceById/{invoiceId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public synchronized ResponseEntity<?> sendInvoiceByEmail(@PathVariable long invoiceId, @RequestParam(defaultValue = "false") boolean isReminder) {
        User loginUser = null;
        try {
            Invoice invoice = null;
            // check if this is a reminder invoice or a regular invoice.
            if (isReminder) {
                invoice = invoiceService.getInvoiceById(invoiceId);
            } else {
                loginUser = userService.getAuthenticateUser();

                if (loginUser == null || loginUser.getHospital() == null) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                }

                List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                        .map(String::trim)
                        .toList();

                for (String branch : branchList) {
                    // Get the invoice within the specified date range.
                    invoice = invoiceService.getInvoiceById(loginUser.getHospital().getId(), branch.getBytes(), invoiceId);
                    if (invoice != null) {
                        break;
                    }
                }
            }

            // If the invoice is not found, return.
            if (invoice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invoice Not Found For Id ");
            }

            // Get the patient's email address.
//            String recipientEmail = Optional.ofNullable(invoice.getAppointment())
//                    .map(Appointment::getPatient)
//                    .map(Patient::getEmailId)
//                    .map(util::decrypt)
//                    .orElse(null);

            String recipientEmail = util.decrypt(new String(invoice.getAppointment().getPatient().getEmailId()));




            // If no email address was found, return.
            if (recipientEmail == null || recipientEmail.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No valid email found for the patient.");
            }

            // Generate the invoice as a PDF.
            byte[] pdfBytes = pdfTemplate.createInvoiceAsBytes(invoice, depositService.getAllDepositByAppointmentId(loginUser, invoice.getAppointment().getId()));

            // If PDF generation failed, return.
            if (pdfBytes == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pdf Generation Failed");
            }


            // Send the email asynchronously.
            Invoice finalInvoice = invoice;
            CompletableFuture.runAsync(() -> {
                try {
                    // Validate the input data.
                    if (finalInvoice == null || recipientEmail == null || recipientEmail.isBlank() || pdfBytes == null || pdfBytes.length == 0) {
                        log.error("Invalid input: Invoice details, recipient email, or PDF content is missing.");
                        return;
                    }

//                    log.info("Preparing to send email to: {}", recipientEmail);

                    // Set the subject and content of the email.
                    String subject = isReminder ? "Your Due Invoice" : "Your Hospital Invoice";

                    String htmlContent = isReminder ? getInvoiceReminderEmailTemplate(
                            finalInvoice.getAppointment().getHospital().getName(),
                            util.decrypt(new String(finalInvoice.getAppointment().getPatient().getName())),
                            String.valueOf(finalInvoice.getId()),
                            finalInvoice.getDueDate(),
                            finalInvoice.getTotalAmount(),
                            finalInvoice.getAppointment().getHospital().getEmailId(),
                            finalInvoice.getAppointment().getHospital().getContactNumber()) :
                            getInvoiceEmailTemplate(
                                    finalInvoice.getAppointment().getHospital().getName(),
                                    util.decrypt(new String(finalInvoice.getAppointment().getPatient().getName())),
                                    String.valueOf(finalInvoice.getId()),
                                    finalInvoice.getDueDate(),
                                    finalInvoice.getTotalAmount(),
                                    finalInvoice.getAppointment().getHospital().getEmailId(),
                                    finalInvoice.getAppointment().getHospital().getContactNumber());

//                    log.info("Sending email for invoice ID: {} to: {} (Subject: {})", invoice.getId(), recipientEmail, subject);

                    // Call email service asynchronously.
                    emailService.sendEmailWithAttachmentForInvoice(recipientEmail, subject, htmlContent, pdfBytes, "Invoice.pdf")
                            .thenAccept(result -> {
                                if ("Success".equalsIgnoreCase(result)) {
//                                    log.info("Email sent successfully for invoice ID: {}", invoice.getId());
                                    System.out.println("Email sent successfully for invoice ID: {}" + finalInvoice.getId());
                                } else {
                                    log.error("Failed to send email for invoice ID: {}. Result: {}" + finalInvoice.getId() + result);
                                }
                            }).exceptionally(ex -> {
                                log.error("Unexpected error while sending email for invoice ID: {}. Error: {}" + finalInvoice.getId() + ex.getMessage(), ex);
                                return null;
                            });

                } catch (Exception e) {
                    log.error("Email sending process failed for invoice ID: {}. Error: {}" + finalInvoice != null ? finalInvoice.getId() : "Unknown" + e.getMessage(), e);
                }
            });


            // Return the PDF as a response.
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice.pdf")
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Sending Mail : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        }
    }


    @GetMapping("/hospitalTurnover")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getHospitalTurnoverByHospitalId() {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            // Get the hospital associated with the logged-in user.
            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getId());

            // Calculate the total revenue and expenses.
//            Double totalRevenue = invoiceService.getTotalRevenueByHospitalId(loginUser);
            double totalRevenue = 0;
            double totalExpenses = staffService.getTotalSalaryExpenses(loginUser);
            for (String branch : branchList) {
                double getRevenue = invoiceService.getTotalRevenueByHospitalId(loginUser.getHospital().getId(), branch.getBytes());
                totalRevenue += getRevenue;
            }
            double netTurnover = totalRevenue - totalExpenses;

            // Create a map to store the turnover data.
            Map<String, Object> turnoverData = new HashMap<>();
            turnoverData.put("totalRevenue", totalRevenue);
            turnoverData.put("totalExpenses", totalExpenses);
            turnoverData.put("netTurnover", netTurnover);
            turnoverData.put("hospital", hospital);

            // Return the turnover data.
            return ResponseEntity.ok(turnoverData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Getting Hospital Turnover By Hospital Id : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }


    @GetMapping("/getHospitalReport")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getHospitalReport() {
        User loginUser = null;
        try {
            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            // Calculate various hospital statistics.
            long staffCount = staffService.getCountOfStaff(loginUser);
            List<RoleSalaryAverageDTO> roleSalaryAverageDTO = staffService.getAverageSalaryByRole(loginUser);
//            double totalRevenue = invoiceService.getTotalRevenueByHospitalId(loginUser);
            double totalRevenue = 0;
            for (String branch : branchList) {
                double getRevenue = invoiceService.getTotalRevenueByHospitalId(loginUser.getHospital().getId(), branch.getBytes());
                totalRevenue += getRevenue;
            }
            double totalExpenses = staffService.getTotalSalaryExpenses(loginUser);

            double netProfit = totalRevenue - totalExpenses;

            // Create a map to store the hospital report data.
            Map<String, Object> report = new HashMap<>();
            report.put("totalStaff", staffCount);
            report.put("roles", roleSalaryAverageDTO);
            report.put("totalExpenses", totalExpenses);
            report.put("totalRevenue", totalRevenue);
            report.put("totalProfit", netProfit);

            // Return the hospital report.
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Fetch Hospital Report : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }


    @GetMapping("/getHospitalReport/monthYear")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getRevenue(
            @RequestParam(value = "month", required = false, defaultValue = "0") int month,
            @RequestParam(value = "year", required = false, defaultValue = "0") int year) {
        User loginUser = null;
        try {
            // Check if both month and year are missing.
            if (month == 0 && year == 0) {
                return ResponseEntity.badRequest().body(ExceptionMessages.MONTH_YEAR_NOT_FOUND);
            }

            // Validate the month parameter.
            if (month != 0 && (month < 1 || month > 12)) {
                return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_MONTH);
            }

            // Validate the year parameter.
            int currentYear = LocalDate.now().getYear();
            if (year != 0 && (year < 2020 || year > currentYear)) {
                return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_YEAR);
            }


            // Retrieve the logged-in user.
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            // Calculate hospital statistics for the given month and year.
            long staffCount = staffService.getNumberOfStaffByHospitalIdAndDate(loginUser, month, year);
            List<RoleSalaryAverageDTO> roleSalaryAverageDTO = staffService.findAverageSalaryByRoleAndDate(loginUser, month, year);
//            Double totalRevenue = invoiceService.getTotalRevenueByHospitalIdAndDate(loginUser, month, year);
//            Double totalRevenue = invoiceService.getTotalRevenueByMonthAndYear(loginUser, month, year);
            double totalRevenue = 0;
            for (String branch : branchList) {
                double getRevenue = invoiceService.getTotalRevenueByMonthAndYear(loginUser.getHospital().getId(), branch.getBytes(), month, year);
                totalRevenue += getRevenue;
            }
            Double totalExpenses = staffService.getTotalSalaryExpensesByHospitalIdAndDate(loginUser, month, year);

            // Calculate net profit.
            Double netProfit = totalRevenue - totalExpenses;

            System.out.println("totalRevenue" + totalRevenue);
            System.out.println("totalExpences" + totalExpenses);
            System.out.println("netprofit" + netProfit);
            // Create a map to store the report data.
            Map<String, Object> report = new HashMap<>();
            report.put("totalStaff", staffCount);
            report.put("roles", roleSalaryAverageDTO);
            report.put("totalExpenses", totalExpenses);
            report.put("totalRevenue", totalRevenue);
            report.put("totalProfit", netProfit);

            // Return the report data.
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while Get Revenue: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }


    //    @Scheduled(fixedRate = 10000000)
    @Scheduled(cron = "0 0 9 * * ?")
//    @Scheduled(fixedRate = 20000)
    public synchronized void getDueInvoice() {
        System.out.println("running");

//        User loginUser = userService.getAuthenticateUser();
        // Retrieve all due invoice.
        List<Invoice> invoice = invoiceService.getDueInvoice();

        // Check if there are any due invoice.
        if (invoice == null || invoice.isEmpty()) {
//            log.warn("No Invoice Found for scheduling");
            System.out.println("No due invoice found.");
            return;
        }

        // Iterate through the due invoice and send email reminders.
        invoice.stream()
                .filter(invoiceBill -> invoiceBill != null)   // Filter out null invoice.
                .forEach(invoiceBill -> {
                    try {
                        // Send an email reminder for the invoice.
                        this.sendInvoiceByEmail(invoiceBill.getId(), true);
                        System.out.println("Email sent to " + invoiceBill.getAppointment().getPatient().getName());
                    } catch (Exception e) {
                        // Log the error if email sending fails.
                        System.err.println("Failed to send email for Invoice ID: " + invoiceBill.getId());
                        e.printStackTrace();
                        log.error("An unexpected error occurred while Fetch Due Invoice : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n "/*+loginUser.getId()*/);
                    }
                });
    }

    private ResponseEntity<?> buildInvoiceDtoForBranch(List<Invoice> invoice, User loginUser, boolean isSingleData) {
        List<InvoiceResponseDTO> dtos = new ArrayList<>();

        for (Invoice invoiceBill : invoice) {
            List<Deposit> allDeposit = new ArrayList<>();

            List<String> loginUserBranch = Arrays.asList(new String(loginUser.getBranch()).trim().split("\\s*,\\s*"));
            String invoiceCreatedUserBranch = new String(invoiceBill.getCreatedUser().getBranch());

            if (!loginUserBranch.contains(invoiceCreatedUserBranch)) {
                continue;
            }

            // Get the Deposit by appointment Id
            if (invoiceBill.getHospitalizationInfo() != null) {
                allDeposit = depositService.getAllDepositByAppointmentId(loginUser, invoiceBill.getAppointment().getId());

                // Handle cases where the deposit is null.
                if (allDeposit == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("allDeposit Not Found For Provided id");
                }
            }
            dtos.add(this.convertToInvoiceDTO(invoiceBill, allDeposit));
        }
        if (isSingleData) {
            return ResponseEntity.ok(dtos.getFirst());
        }
        // Return the invoice.
        return ResponseEntity.ok(dtos);
    }

    public InvoiceResponseDTO convertToInvoiceDTO(Invoice invoice, List<Deposit> allDeposit) {
        if (invoice == null) return null;

        UserController userController = new UserController();

        InvoiceResponseDTO dto = new InvoiceResponseDTO();
        dto.setId(invoice.getId());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setBalanceAmount(invoice.getPaidAmount());
        dto.setPaidAmount(invoice.getPaidAmount());
        dto.setDueDate(invoice.getDueDate());
        dto.setDoctorFee(invoice.getDoctorFee());
        dto.setPaymentMode(invoice.getPaymentMode());
        dto.setCreatedDate(invoice.getCreatedDate());
        dto.setModifiedDate(invoice.getModifiedDate());
        dto.setAppointmentDTO(appointmentService.appointmentToAppointmentDto(invoice.getAppointment()));
        dto.setHospitalizationInfo(invoice.getHospitalizationInfo());
        dto.setStatus(invoice.getStatus());
        dto.setPolicyAmount(invoice.getPolicyAmount());
        dto.setPolicyNumber(invoice.getPolicyNumber());
        dto.setPolicyStatus(invoice.getPolicyStatus());
        dto.setPolicyCompanyName(invoice.getPolicyCompanyName());
        dto.setCreatedUser(userController.convertToDTO(invoice.getCreatedUser(), new HashSet<>()));
        dto.setCreatedUser(userController.convertToDTO(invoice.getModifiedUser(), new HashSet<>()));
        if (allDeposit != null || !allDeposit.isEmpty()) {
            dto.setDeposit(allDeposit);
        } else {
            dto.setQuickCareCharges(invoice.getQuickCareCharges());
        }
        return dto;
    }

    public String getInvoiceEmailTemplate(
            String hospitalName,
            String patientName,
            String invoiceId,
            LocalDate invoiceDate,
            double totalAmount,
            String contactEmail,
            String contactNumber) {

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #e6f7ff; color: #333; line-height: 1.6; }" +
                ".container { width: 100%; max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 15px; box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12); overflow: hidden; }" +
                ".header { background: linear-gradient(135deg, #47a3da, #2a73ad); color: #ffffff; text-align: center; padding: 30px; font-size: 24px; font-weight: 600; letter-spacing: 0.5px; }" +
                ".body { padding: 30px; font-size: 16px; color: #444; }" +
                ".invoice-details { background: #f0f8ff; padding: 20px; border-radius: 10px; margin: 25px 0; font-size: 15px; border: 1px solid #d1e5f0; }" +
                ".invoice-details strong { display: block; margin-bottom: 10px; color: #2a73ad; }" +
                ".invoice-details ul { list-style: none; padding: 0; }" +
                ".invoice-details li { padding: 10px 0; border-bottom: 1px solid #e1e1e1; }" +
                ".invoice-details li:last-child { border-bottom: none; }" +
                ".footer { background-color: #f8f8f8; color: #666; text-align: center; padding: 20px; font-size: 14px; }" +
                ".footer a { color: #2a73ad; text-decoration: none; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">Your Hospital Invoice from " + hospitalName + "</div>" +
                "<div class=\"body\">" +
                "<p>Dear <strong>" + patientName + "</strong>,</p>" +
                "<p>Thank you for choosing <strong>" + hospitalName + "</strong> for your healthcare needs. We've attached your hospital invoice for your recent visit. Please review the details at your convenience.</p>" +
                "<div class=\"invoice-details\">" +
                "<strong>Invoice Information:</strong>" +
                "<ul>" +
                "<li><strong>Invoice Number:</strong> " + invoiceId + "</li>" +
                "<li><strong>Invoice Date:</strong> " + invoiceDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + "</li>" +
                "<li><strong>Total Amount Due:</strong> " + NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(totalAmount) + "</li>" +
                "</ul>" +
                "</div>" +
                "<p>If you have any questions or need assistance with your invoice, please contact our invoice department.</p>" +
                "<p>You can reach us at <strong>" + contactEmail + "</strong> or call <strong>" + contactNumber + "</strong>.</p>" +
                "<p>We appreciate your prompt attention to this matter and wish you continued health and well-being.</p>" +
                "<p>Sincerely,<br>The Invoice Department<br><strong>" + hospitalName + "</strong></p>" +
                "</div>" +
                "<div class=\"footer\">&copy; " + Year.now() + " " + hospitalName + ". All Rights Reserved. <br>" +
//                "<a href=\"" + website + "\">Visit Our Website</a>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }


    public String getInvoiceReminderEmailTemplate(
            String hospitalName,
            String patientName,
            String invoiceId,
            LocalDate dueDate,
            double totalAmount,
            String contactEmail,
            String contactNumber) {

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #fff3cd; color: #333; line-height: 1.6; }" +
                ".container { width: 100%; max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 15px; box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12); overflow: hidden; }" +
                ".header { background: linear-gradient(135deg, #ffb347, #ff7730); color: #ffffff; text-align: center; padding: 30px; font-size: 24px; font-weight: 600; letter-spacing: 0.5px; }" +
                ".body { padding: 30px; font-size: 16px; color: #444; }" +
                ".invoice-details { background: #fff8e1; padding: 20px; border-radius: 10px; margin: 25px 0; font-size: 15px; border: 1px solid #ffd966; }" +
                ".invoice-details strong { display: block; margin-bottom: 10px; color: #ff7730; }" +
                ".invoice-details ul { list-style: none; padding: 0; }" +
                ".invoice-details li { padding: 10px 0; border-bottom: 1px solid #e1e1e1; }" +
                ".invoice-details li:last-child { border-bottom: none; }" +
                ".footer { background-color: #f8f8f8; color: #666; text-align: center; padding: 20px; font-size: 14px; }" +
                ".footer a { color: #ff7730; text-decoration: none; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">Invoice Reminder from " + hospitalName + "</div>" +
                "<div class=\"body\">" +
                "<p>Dear <strong>" + patientName + "</strong>,</p>" +
                "<p>This is a kind reminder regarding your outstanding balance with <strong>" + hospitalName + "</strong>. We understand that managing healthcare costs can be challenging, and we are here to assist you.</p>" +
                "<div class=\"invoice-details\">" +
                "<strong>Invoice Details:</strong>" +
                "<ul>" +
                "<li><strong>Invoice Number:</strong> " + invoiceId + "</li>" +
                "<li><strong>Due Date:</strong> " + dueDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + "</li>" +
                "<li><strong>Total Amount Due:</strong> " + NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(totalAmount) + "</li>" +
                "</ul>" +
                "</div>" +
                "<p>If you have any questions or require further assistance, please contact our invoice department.</p>" +
                "<p>You can reach us at <strong>" + contactEmail + "</strong> or call <strong>" + contactNumber + "</strong>.</p>" +
                "<p>We appreciate your prompt attention to this matter and look forward to resolving it as soon as possible.</p>" +
                "<p>Sincerely,<br>The Invoice Department<br><strong>" + hospitalName + "</strong></p>" +
                "</div>" +
                "<div class=\"footer\">&copy; " + Year.now() + " " + hospitalName + ". All Rights Reserved. <br>" +
//                    "<a href=\"" + website + "\">Visit Our Website</a>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

}






