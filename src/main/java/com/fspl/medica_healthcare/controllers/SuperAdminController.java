package com.fspl.medica_healthcare.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.templets.EmailTemplets;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/superAdmin")
public class SuperAdminController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService otpService;

    private static final Logger log = Logger.getLogger(SuperAdminController.class);


    @GetMapping("/hospitalBills/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> getAllHospitalBillsByHospitalId(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
//            List<Billing> billingList = billingService.getAllHospitalBillsByHospitalId(loginUser);
            List<Invoice> billingList = invoiceService.getAllInvoiceByHospitalId(loginUser.getHospital().getId(),loginUser.getBranch());
            if (billingList == null) {
                return ResponseEntity.ok("No Bills Found.");
            }
            return ResponseEntity.ok(billingList);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllHospitalBillsByHospitalId(): \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User Id :\n "+loginUser.getId());

            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @PostMapping("/addAdmin/{hospitalId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(@PathVariable long hospitalId, @Valid @RequestBody UserDTO userDTO) {
        User loginUser = null;
        try {

            if (userDTO.getRoles() == null) {
                return ResponseEntity.badRequest().body("Role is required");
            }
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            Hospital hospital = hospitalService.getHospitalById(hospitalId);

            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.HOSPITAL_NOT_FOUND);
            }

            Otp otp = otpService.getOtpByEmail(userDTO.getUsername());
            if (otp == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.OTP_NOT_VERIFIED);
            }
            if (!otp.isVerified()) {
                return ResponseEntity.badRequest().body(ExceptionMessages.OTP_NOT_VERIFIED);
            }

            String role = userDTO.getRoles().toUpperCase(); // Ensure case consistency

            if (!role.equals("ADMIN")) {
                return ResponseEntity.badRequest().body("You have only access to add ADMIN");
            }

            User user = userService.userDtoToUser(userDTO);
            // Check if the username already exists
            if (userService.findByUsername(user.getUsername()) != null || staffService.findByEmail(user.getUsername() , loginUser) != null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.USERNAME_ALREADY_EXIST);
            }

            Staff staff = staffService.userDtoTOStaff(userDTO);
//            staff.setDoctorFee(BigDecimal.valueOf(0));
            staff.setDoctorFee(0.0);
            staff.setBranch(hospital.getBranch());
            staff.setHospital(hospital);
            staff.setStatus(1);
            staff.setCreatedUser(loginUser);
            staff.setModifiedUser(loginUser);
            staff.setCreatedDate(LocalDate.now());
            staff.setModifiedDate(LocalDate.now());
            boolean isSaved = staffService.saveStaff(staff , loginUser);

            if (isSaved) {

                Staff savedStaff = staffService.findByEmail(staff.getEmail() , loginUser);

                String namePart = userDTO.getName().substring(0, Math.min(userDTO.getName().length(), 3));
                String contactPart = userDTO.getContactNumber().substring(0, 4);
                String birthDatePart = userDTO.getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyy"));
                String autoGeneratedPassword = namePart + contactPart + "@" + birthDatePart;


                String maskedPassword = maskPassword(autoGeneratedPassword);

                user.setHospital(hospital);
                user.setStaff(savedStaff);
                user.setBranch(hospital.getBranch());
                user.setRoles(role);
                user.setCreatedUser(loginUser);
                user.setModifiedUser(loginUser);
                user.setCreatedDate(LocalDate.now());
                user.setModifiedDate(LocalDate.now());
                user.setStatus(1);
                user.setPassword(encoder.encode(autoGeneratedPassword));
                boolean isSavedUser = userService.saveUser(user, loginUser);
                if (isSavedUser) {
                    User savedUser = userService.findByUsername(user.getUsername());
                    emailService.sendEmail(savedUser.getUsername(), "Welcome to " + hospital.getName() + " - Your Registration is Complate", UserController.getUserRegistrationSuccessTemplate(hospital.getName(), savedUser.getUsername(),maskedPassword, savedUser.getRoles(), savedStaff.getId(), savedStaff.getSalary()));
                    return ResponseEntity.ok(ExceptionMessages.USER_SAVED);
                } else {
                    return ResponseEntity.ok(ExceptionMessages.USER_NOT_SAVED);
                }
            } else {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while createAdmin(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+" Logged User :\n "+loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    private String maskPassword(String password) {
        if (password == null || password.length() < 4) {
            return "XXXX";
        }
        return password.substring(0, 2) + "X".repeat(password.length() - 4) + password.substring(password.length() - 2);
    }



}
