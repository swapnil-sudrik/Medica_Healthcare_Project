package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.StaffDTO;
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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    @Autowired
    private UserController userController;

    private static final Logger log = Logger.getLogger(StaffController.class);

/*
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

            //Add branch wise logic ...DHIRAJ

            /////

            if (staffList == null) {
                return ResponseEntity.ok(ExceptionMessages.STAFFS_NOT_FOUND);
            }
            List<StaffDTO> staffDTOS = new ArrayList<>();
            for (Staff staff : staffList){
                StaffDTO staffDTO = staffDTOTOStaff(staff);
                staffDTOS.add(staffDTO);
            }

            return ResponseEntity.ok(staffDTOS);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllActiveStaffByHospitalId(): \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }
 */

    //Add branch wise logic ..

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllActiveStaffByHospitalId() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            List<StaffDTO> staffDTOS = new ArrayList<>();

            // Convert byte[] to List<String> for multiple branches
            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            for (String branch : branchList) {
                // This method should be implemented in your StaffService/Repo to filter by hospital and branch
                List<Staff> staffList = staffService.getAllActiveStaffByHospitalAndBranch(loginUser.getHospital().getId(), branch.getBytes());

                if (staffList != null) {
                    for (Staff staff : staffList) {
                        staffDTOS.add(staffDTOTOStaff(staff));
                    }
                }
            }

            if (staffDTOS.isEmpty()) {
                return ResponseEntity.ok("No active staff found in your branch.");
            }

            return ResponseEntity.ok(staffDTOS);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllActiveStaffByHospitalId(): \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User :\n " + (loginUser != null ? loginUser.getId() : "null"));
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
//            List<Staff> staffList = staffService.getAllStaffByHospitalId(loginUser);

            List<Staff> staffList = new ArrayList<>();

            // Get logged-in user's branch list (can be multiple if master admin
            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            for (String branch : branchList) {
                List<Staff> branchWiseStaff = staffService.getAllStaffByHospitalAndBranch(
                        loginUser.getHospital().getId(), branch.getBytes()
                );
                if (branchWiseStaff != null) {
                    staffList.addAll(branchWiseStaff);
                }
            }

                //

            if (staffList == null) {
                return ResponseEntity.ok(ExceptionMessages.STAFFS_NOT_FOUND);
            }
            List<StaffDTO> staffDTOS = new ArrayList<>();
            for (Staff staff : staffList){
                StaffDTO staffDTO = staffDTOTOStaff(staff);
                staffDTOS.add(staffDTO);
            }
            return ResponseEntity.ok(staffDTOS);
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

                //not verified
                if (!staff.getEmail().equals(userDTO.getUsername()) && userDTO.getUsername() != null) {
                    return ResponseEntity.badRequest().body("this staff is present in user so you can not change email");
                }

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

            if ("DOCTOR".equals(staff.getRoles()) && userDTO.getType() != null) {
                String type = userDTO.getType();
                if ("Regular".equals(type) || "Freelancer".equals(type)) {
                    staff.setType(type);
                } else {

                    return ResponseEntity.badRequest().body("Invalid type. Only 'Regular' or 'Freelancer' are allowed.");

                }
            }
            if (userRoles.contains("DOCTOR")) {
                staff.setDoctorFee(userDTO.getDoctorFee());
            } else {
//                staff.setDoctorFee(BigDecimal.ZERO);
                staff.setDoctorFee(0.0);
            }
            staff.setSalary(userDTO.getSalary());
            staff.setModifiedUser(loginUser);
            staff.setModifiedDate(LocalDate.now());

            //add staff changes..
            staff.setCareTaker(userDTO.isCareTaker());
            staff.setSpecalization(userDTO.getSpecalization());
            staff.setBookingStatus(userDTO.getBookingStatus());
            staff.setBookingCharge(userDTO.getBookingCharge());
            /////////////////////////////////


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
            return ResponseEntity.ok(staffDTOTOStaff(staff));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getStaffById(): \n" + ExceptionUtils.getStackTrace(e) + "\n" + " Logged User :\n " + loginUser.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @PutMapping("/reactive/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> reactivateUser(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            Staff staff = staffService.findById(id, loginUser);

//            if (staff == null || staff.getHospital().getId()!=(loginUser.getHospital().getId())) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.STAFF_NOT_FOUND);
//            }

            //Add branchwie logic..
            // Hospital check (primitive long comparison)
            if (staff == null || staff.getHospital() == null || loginUser.getHospital() == null ||
                    staff.getHospital().getId() != loginUser.getHospital().getId()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.STAFF_NOT_FOUND);
            }

            // Branch-level check for Normal Admin (created by Master Admin)
            if (staff.getCreatedUser() != null && staff.getCreatedUser().getId() != loginUser.getId()) {
                // compare branch (assuming it's a byte[] or same type)
                if (!Arrays.equals(staff.getBranch(), loginUser.getBranch())) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.UNAUTHORIZED_BRANCH_ACCESS);
                }
            }



            //

            // Check if the user is already active
            if (staff.getStatus() == 1) {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_IS_ACTIVE);
            }
            staff.setStatus(1);
            staff.setModifiedUser(loginUser);
            staff.setModifiedDate(LocalDate.now());
            boolean isStaffSaved = staffService.saveStaff(staff, loginUser);
            User user = userService.findByUsername(staff.getEmail());
            if (isStaffSaved && user != null) {
                user.setStatus(1);
                user.setModifiedUser(loginUser);
                user.setModifiedDate(LocalDate.now());
                boolean isUserSaved = userService.saveUser(user, loginUser);
                if (isUserSaved) {
                    //email
                    emailService.sendEmail(user.getUsername(), "Your " + user.getHospital().getName() + " Account Has Been Activated", userController.getUserReactivationTemplate(user.getName(), user.getHospital().getName()));
                    return ResponseEntity.ok("User and Staff"+ ExceptionMessages.ACCOUNT_ACTIVATED);
                }
            } else if (isStaffSaved && user == null) {
                //email
                emailService.sendEmail(staff.getEmail(), "Your " + staff.getHospital().getName() + " Account Has Been Activated", userController.getUserReactivationTemplate(staff.getName(), staff.getHospital().getName()));
                return ResponseEntity.ok("User and Staff"+ ExceptionMessages.ACCOUNT_ACTIVATED);
            }
            return ResponseEntity.ok("User"+ ExceptionMessages.SOMETHING_WENT_WRONG);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while reactivateUser(): \n"+ExceptionUtils.getStackTrace(e)+"\n"+" Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteStaff(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            Staff staff = staffService.findById(id , loginUser);
//            if (staff == null || staff.getHospital().getId() != (loginUser.getHospital().getId())) {
//                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_NOT_FOUND);
//            }
            //add branch wise logic..
            // Basic null checks + hospital id comparison (primitive long uses ==)
            if (staff == null || staff.getHospital() == null || loginUser.getHospital() == null ||
                    staff.getHospital().getId() != loginUser.getHospital().getId()) {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_NOT_FOUND);
            }

            //Add branch wise logic...
            // Branch-level access control logic similar to deleteUser()
            if (staff.getCreatedUser() != null && staff.getCreatedUser().getId() != loginUser.getId()) {
                // compare branches (assuming branch is a byte[] or similar)
                if (!Arrays.equals(staff.getBranch(), loginUser.getBranch())) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.UNAUTHORIZED_BRANCH_ACCESS);
                }
            }

            ////
            staff.setStatus(0);
            staff.setModifiedUser(loginUser);
            staff.setModifiedDate(LocalDate.now());
            boolean isStaffSaved = staffService.saveStaff(staff, loginUser);
            User user = userService.findByUsername(staff.getEmail());
            if (isStaffSaved && user!=null){
                user.setStatus(0); // Soft delete
                user.setModifiedUser(loginUser);
                user.setModifiedDate(LocalDate.now());
                boolean isUserSaved = userService.saveUser(user, loginUser);
                if (isUserSaved){
                    emailService.sendEmail(user.getUsername(), "Your " + user.getHospital().getName() + " Account Has Been Deactivated", userController.getUserDeactivationTemplate(user.getUsername(), user.getHospital().getName()));
                    return ResponseEntity.ok("USER and STAFF" +ExceptionMessages.ACCOUNT_DEACTIVATED);
                }else {
                    return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                }
            } else if (isStaffSaved && user == null) {
                emailService.sendEmail(staff.getEmail(), "Your " + staff.getHospital().getName() + " Account Has Been Deactivated", userController.getUserDeactivationTemplate(staff.getEmail(), staff.getHospital().getName()));
                return ResponseEntity.ok("STAFF" +ExceptionMessages.ACCOUNT_DEACTIVATED);
            }
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while deleteUser() : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+" Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    public StaffDTO staffDTOTOStaff(Staff staff){

        StaffDTO staffDTO = new StaffDTO();
        staffDTO.setId(staff.getId());
        staffDTO.setEmail(staff.getEmail());
        staffDTO.setName(staff.getName());
        staffDTO.setAddress(new String(staff.getAddress()));
        staffDTO.setBranch(new String(staff.getBranch()));
        staffDTO.setRoles(staff.getRoles());
        staffDTO.setDoctorFee(staff.getDoctorFee());
        staffDTO.setSalary(staff.getSalary());
        staffDTO.setStatus(staff.getStatus());
        staffDTO.setHospital(staff.getHospital());
        staffDTO.setCreatedUser(staff.getCreatedUser());
        staffDTO.setModifiedUser(staff.getModifiedUser());
        staffDTO.setCreatedDate(staff.getCreatedDate());
        staffDTO.setModifiedDate(staff.getModifiedDate());
        return staffDTO;
    }


    public String getStaffUpdateTemplate(String staffName, String hospitalName, String branch, String role, double salary) {
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
