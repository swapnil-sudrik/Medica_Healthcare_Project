package com.fspl.medica_healthcare.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.models.Billing;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Staff;
import com.fspl.medica_healthcare.models.User;
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
    private BillingService billingService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private EmailService emailService;

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
            List<Billing> billingList = billingService.getAllHospitalBillsByHospitalId(loginUser);
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
//    @PostMapping("/addAdmin/{hospitalId}")
//    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
//    public ResponseEntity<Map<String, Object>> createAdmin(@PathVariable Long hospitalId, @Valid @RequestBody List<User> users){
//        User authenticateUser = userService.getAuthenticateUser();
//        Hospital hospital = hospitalService.getHospitalById(hospitalId);
//
//        List<User> savedAdmins = new ArrayList<>();
//        List<String> errorMessages = new ArrayList<>();
//
//        for (User user : users) {
//            // Check if the username already exists
//            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
//                errorMessages.add("User with username " + user.getUsername() + " already exists and was not added.");
//                continue;
//            }
//

    /// /            // Check if the role is restricted
//            if ("ADMIN".equals(user.getRoles())) {
//                String userPassword = user.getPassword();
//            user.setHospitalId(hospital);
//            user.setRoles(user.getRoles());
//            user.setCreated(authenticateUser);
//            user.setModified(authenticateUser);
//            user.setCreatedDate(LocalDate.now());
//            user.setModifiedDate(LocalDate.now());
//            user.setStatus(1);
//            user.setPassword(encoder.encode(user.getPassword()));
//            savedAdmins.add(userService.saveUser(user,userPassword));
//            }else{
//                errorMessages.add("User with username " + user.getUsername() + " was not added due to restricted role: " + user.getRoles());
//            }
//        }
//        // Prepare response
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", savedAdmins);
//        response.put("errors", errorMessages);
//        return ResponseEntity.ok(response);    }
//}
    @PostMapping("/addAdmin/{hospitalId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(@PathVariable long hospitalId, @Valid @RequestBody UserDTO userDTO) {
        User loginUser = null;
        try {
            if (userDTO.getPassword() == null) {
                return ResponseEntity.badRequest().body("Password is required");
            }
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
            staff.setDoctorFee(BigDecimal.valueOf(0));
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
                String password = userDTO.getPassword();
                user.setHospital(hospital);
                user.setStaff(savedStaff);
                user.setBranch(hospital.getBranch());
                user.setRoles(role);
                user.setCreatedUser(loginUser);
                user.setModifiedUser(loginUser);
                user.setCreatedDate(LocalDate.now());
                user.setModifiedDate(LocalDate.now());
                user.setStatus(1);
                user.setPassword(encoder.encode(user.getPassword()));
                boolean isSavedUser = userService.saveUser(user, loginUser);
                if (isSavedUser) {
                    User savedUser = userService.findByUsername(user.getUsername());
                    emailService.sendEmail(savedUser.getUsername(), "Welcome to " + hospital.getName() + " - Your Registration is Complate", UserController.getUserRegistrationSuccessTemplate(hospital.getName(), savedUser.getUsername(), password, savedUser.getRoles(), savedStaff.getId(), savedStaff.getSalary()));
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
}
