package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Staff;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.EmailService;
import com.fspl.medica_healthcare.services.HospitalService;
import com.fspl.medica_healthcare.services.StaffService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import com.fspl.medica_healthcare.utils.RoleConstants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @Autowired
    private UserService userService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private EmailService emailService;

    private static final Logger log = Logger.getLogger(StaffController.class);


    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllActiveStaffByHospitalId() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<Staff> staffList = staffService.getAllActiveStaffByHospitalId(loginUser);
            if (staffList == null) {
                return ResponseEntity.ok(ExceptionMessages.STAFFS_NOT_FOUND);
            }
            return ResponseEntity.ok(staffList);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllActiveStaffByHospitalId(): \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllStaffByHospitalId() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<Staff> staffList = staffService.getAllStaffByHospitalId(loginUser);
            if (staffList == null) {
                return ResponseEntity.ok(ExceptionMessages.STAFFS_NOT_FOUND);
            }
            return ResponseEntity.ok(staffList);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllStaffByHospitalId(): \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> updateStaff(@PathVariable long id, @RequestBody UserDTO userDTO) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            Staff staff = staffService.findById(id, loginUser);
            if (staff == null) {
                return ResponseEntity.ok(ExceptionMessages.STAFF_NOT_FOUND);
            }

            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getId());
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.HOSPITAL_NOT_FOUND);
            }

            List<String> branchList = Arrays.stream(new String(hospital.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            if (!branchList.contains(userDTO.getBranch().trim())) {
                return ResponseEntity.badRequest().body("Branch Not Found. Available Branches: " + branchList);
            }

            List<String> userRoles = RoleConstants.USER_ROLES;

            if (userRoles.contains(staff.getRoles())) {
                if (!staff.getEmail().equals(userDTO.getUsername()) && userDTO.getUsername() != null) {
                    return ResponseEntity.badRequest().body("this staff is present in user so you can not change email");
                }
//                if (!staff.getName().equals(userDTO.getName()) && userDTO.getName()!=null){
//                    return ResponseEntity.badRequest().body("this staff is present in user so you can not change name");
//                }

                User user = userService.findByUsername(staff.getEmail());

                if (user == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USER_NOT_FOUND);
                }
                if (staff.getRoles().equals("ADMIN")) {
                    staff.setBranch(hospital.getBranch());
                    user.setBranch(hospital.getBranch());
                } else {
                    staff.setBranch(userDTO.getBranch().getBytes());
                    user.setBranch(userDTO.getBranch().getBytes());
                }
//                user.setUsername(userDTO.getUsername());
                user.setName(userDTO.getName());
                user.setRoles(userDTO.getRoles().toUpperCase());

            } else {
                staff.setEmail(userDTO.getUsername());
                staff.setBranch(userDTO.getBranch().getBytes());
            }

            staff.setName(userDTO.getName());
            staff.setRoles(userDTO.getRoles().toUpperCase());
            staff.setAddress(userDTO.getAddress().getBytes());
            if (userRoles.contains("DOCTOR")) {
                staff.setDoctorFee(userDTO.getDoctorFee());
            } else {
                staff.setDoctorFee(BigDecimal.ZERO);
            }
            staff.setSalary(userDTO.getSalary());
            staff.setModifiedUser(loginUser);
            staff.setModifiedDate(LocalDate.now());
            boolean isSaved = staffService.saveStaff(staff, loginUser);
            if (isSaved) {
                Staff savedStaff = staffService.findById(staff.getId(), loginUser);
                emailService.sendEmail(savedStaff.getEmail(), "Your Profile & Salary Details Have Been Updated in" + staff.getHospital().getName(), getStaffUpdateTemplate(savedStaff.getName(), savedStaff.getHospital().getName(), new String(savedStaff.getBranch()), savedStaff.getRoles(), savedStaff.getSalary()));
                return ResponseEntity.ok("Staff" + ExceptionMessages.UPDATE_SUCCESS);
            } else {
                return ResponseEntity.badRequest().body("Staff" + ExceptionMessages.UPDATE_FAILED);
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while updateStaff(): \n" + ExceptionUtils.getStackTrace(e) + "\n" + " Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getStaffById(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            Staff staff = staffService.findById(id, loginUser);
            if (staff == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_NOT_FOUND);
            }
            return ResponseEntity.ok(staff);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getStaffById(): \n" + ExceptionUtils.getStackTrace(e) + "\n" + " Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }


    public String getStaffUpdateTemplate(String staffName, String hospitalName, String branch, String role, BigDecimal salary) {
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
                "Staff Profile Updated" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear " + staffName + ",</p>" +
                "<p>Your profile information has been successfully updated in the " + hospitalName + " system.</p>" +
                "<p><strong>Updated Details:</strong></p>" +
                "<p><strong>Branch:</strong> " + branch + "</p>" +
                "<p><strong>Role:</strong> " + role + "</p>" +
                "<p><strong>Salary:</strong> $" + salary + "</p>" +
                "<p>If you did not request this change or notice any discrepancies, please contact the administration immediately.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

}
