package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.models.AuthRequest;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Staff;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import com.fspl.medica_healthcare.utils.RoleConstants;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LoginDetailService loginDetailService;

    private static final Logger log = Logger.getLogger(UserController.class);

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> createUser(@Valid @RequestBody UserDTO userDTO) {
        User loginUser =null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            if (userDTO.getRoles() == null) {
                return ResponseEntity.badRequest().body("Role is required");
            }
            String role = userDTO.getRoles().toUpperCase(); // Ensure case consistency

            if (role.equals("SUPER_ADMIN")){
                return ResponseEntity.badRequest().body("You do not have permission to add SUPER_ADMIN");
            }
            // Define allowed roles for User table
            List<String> userRoles = RoleConstants.USER_ROLES;



            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getId());
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.HOSPITAL_NOT_FOUND);
            }

            User user = userService.userDtoToUser(userDTO);

            List<String> branchList = Arrays.stream(new String(hospital.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            if (!branchList.contains(userDTO.getBranch().trim()) && !role.equals("ADMIN")) {
                return ResponseEntity.badRequest().body(ExceptionMessages.BRANCH_NOT_FOUND + branchList);
            }

            // Check if the username already exists
            if (userService.findByUsername(user.getUsername()) != null || staffService.findByEmail(user.getUsername() , loginUser) != null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.USERNAME_ALREADY_EXIST);
            }

            Staff staff = staffService.userDtoTOStaff(userDTO);

            if (role.equals("DOCTOR")) {
                staff.setDoctorFee(userDTO.getDoctorFee());
            } else {
                staff.setDoctorFee(BigDecimal.valueOf(0));
            }
            if (role.equals("ADMIN")) {
                staff.setBranch(hospital.getBranch());
            } else {
                staff.setBranch(userDTO.getBranch().getBytes());
            }
            staff.setHospital(hospital);
            staff.setStatus(1);
            staff.setCreatedUser(loginUser);
            staff.setModifiedUser(loginUser);
            staff.setCreatedDate(LocalDate.now());
            staff.setModifiedDate(LocalDate.now());
            boolean isSaved = staffService.saveStaff(staff , loginUser);

            if (isSaved) {
                Staff savedStaff = staffService.findByEmail(staff.getEmail() , loginUser);

                if (userRoles.contains(role)) {
                    if (userDTO.getPassword() == null) {
                        return ResponseEntity.badRequest().body("Password is required");
                    }
                    String password = userDTO.getPassword();
                    user.setHospital(hospital);
                    user.setStaff(savedStaff);
                    if (role.equals("ADMIN")) {
                        user.setBranch(hospital.getBranch());
                    } else {
                        user.setBranch(userDTO.getBranch().getBytes());
                    }
                    user.setRoles(role);
                    user.setCreatedUser(loginUser);
                    user.setModifiedUser(loginUser);
                    user.setCreatedDate(LocalDate.now());
                    user.setModifiedDate(LocalDate.now());
                    user.setStatus(1);
                    user.setPassword(encoder.encode(userDTO.getPassword()));
                    boolean isSavedUser = userService.saveUser(user , loginUser);
                    if (isSavedUser) {
                        User savedUser = userService.findByUsername(user.getUsername());
                        //email
                        emailService.sendEmail(savedUser.getUsername(), "Welcome to "+hospital.getName() +" - Your Registration is Complate", getUserRegistrationSuccessTemplate(hospital.getName(), savedUser.getUsername(),password, savedUser.getRoles(),savedStaff.getId(), savedStaff.getSalary()));

                        return ResponseEntity.ok(ExceptionMessages.USER_SAVED);
                    }else {
                        return ResponseEntity.ok(ExceptionMessages.USER_NOT_SAVED);
                    }
                } else {
                    //emailstaff
                    emailService.sendEmail(savedStaff.getEmail(), "Welcome to "+hospital.getName() +" - Your Registration is Complete", getStaffRegistrationSuccessTemplate(hospital.getName(), savedStaff.getEmail(), savedStaff.getRoles(),savedStaff.getId(), savedStaff.getSalary()));
                    return ResponseEntity.ok(ExceptionMessages.STAFF_SAVED);
                }

            } else {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_NOT_SAVED);
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred while createUser(): \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            User user = userService.getUserById(id , loginUser);
            if (user.getRoles().equals("SUPER_ADMIN")){
                return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_REQUEST);
            }

            if (user==null || user.getHospital().getId()!=(loginUser.getHospital().getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USER_NOT_FOUND);
            }
//            UserResponseDTO response = UserMapper.toUserResponseDTO(user);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getUserById(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAllHospitalUsers() {
        User loginUser=null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<User> users = userService.getAllUsersByHospital(loginUser);
            if (users == null){
                return ResponseEntity.ok(ExceptionMessages.USERS_NOT_FOUND);
            }
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllHospitalUsers(): \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @GetMapping("/getActive")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAllActiveUsersOfHospital() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<User> activeUsers = userService.getAllActiveUsersOfHospital(loginUser);
            if (activeUsers == null){
                return ResponseEntity.ok(ExceptionMessages.USERS_NOT_FOUND);
            }
            return ResponseEntity.ok(activeUsers);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllActiveUsersOfHospital(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @PutMapping("/reactive/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> reactivateUser(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            Staff staff = staffService.findById(id , loginUser);

            if (staff == null || staff.getHospital().getId()!=(loginUser.getHospital().getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.STAFF_NOT_FOUND);
            }

            // Check if the user is already active
            if (staff.getStatus() == 1) {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_IS_ACTIVE);
            }
            staff.setStatus(1);
            staff.setModifiedUser(loginUser);
            staff.setModifiedDate(LocalDate.now());
            boolean isSaved = staffService.saveStaff(staff , loginUser);
            User user = userService.findByUsername(staff.getEmail());

            if (isSaved && user != null) {
                // Check if the user is already active
                user.setStatus(1);
                user.setModifiedUser(loginUser);
                user.setModifiedDate(LocalDate.now());
                boolean isSavedUser = userService.saveUser(user ,loginUser);
                if (isSavedUser) {
                    //email
                    emailService.sendEmail(user.getUsername(), "Your " + user.getHospital().getName() + " Account Has Been Activated", getUserReactivationTemplate(user.getName(), user.getHospital().getName()));
                    return ResponseEntity.ok("User"+ ExceptionMessages.ACCOUNT_ACTIVATED);
                }
            }
            emailService.sendEmail(staff.getEmail(), "Your " + staff.getHospital().getName() + " Account Has Been Activated", getStaffReactivationTemplate(staff.getName(), staff.getHospital().getName()));
            return ResponseEntity.ok("Staff" + ExceptionMessages.ACCOUNT_ACTIVATED);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while reactivateUser(): \n"+ExceptionUtils.getStackTrace(e)+"\n"+" Logged User :\n "+loginUser);

            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }




    //update for self
    @PutMapping("/update")
    @PreAuthorize("hasAnyAuthority('DOCTOR','SUPER_ADMIN', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<?> updateUser(@RequestBody @Valid UserDTO updatedUserDTO) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            if (loginUser==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            if (updatedUserDTO.getRoles() == null){
                return ResponseEntity.badRequest().body("Role is required");
            }
            String role = updatedUserDTO.getRoles().toUpperCase(); // Ensure case consistency

            // Define allowed roles for User table
            List<String> userRoles = RoleConstants.USER_ROLES;



//           Staff checkUsername = staffService.findByEmail(updatedUserDTO.getUsername());
//
//           if (checkUsername !=null && !loginUser.getUsername().equals(updatedUserDTO.getUsername())){
//               return ResponseEntity.badRequest().body("This Username Already taken. use another username");
//           }

            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getId());
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.HOSPITAL_NOT_FOUND);
            }

            if (!loginUser.getRoles().equals(role) && updatedUserDTO.getRoles() !=null){
                return ResponseEntity.badRequest().body("You don't have access to change your role [ "+loginUser.getRoles()+" ] to [ "+updatedUserDTO.getRoles()+" ].");
            }

            if (!loginUser.getStaff().getSalary().equals(updatedUserDTO.getSalary()) && updatedUserDTO.getSalary() != null){
                return ResponseEntity.badRequest().body("You cant edit your salary");
            }

            //if the username is change then we need to login again because previous token having previous email and that not in database
            if (!loginUser.getUsername().equals(updatedUserDTO.getUsername())){
                loginDetailService.delete(loginDetailService.findByUsername(loginUser.getUsername()));
            }

            Staff staff = staffService.findByEmail(loginUser.getUsername() , loginUser);

            staff.setEmail(updatedUserDTO.getUsername());
            staff.setName(updatedUserDTO.getName());
            staff.setAddress(updatedUserDTO.getAddress().getBytes());
            staff.setRoles(role);
//            if (role.equals("DOCTOR")) {
//                staff.setDoctorFee(updatedUserDTO.getDoctorFee());
//            } else {
//                staff.setDoctorFee(BigDecimal.valueOf(0));
//            }
//            if (role.equals("ADMIN")) {
//                staff.setBranch(hospital.getBranches());
//            } else {
//                staff.setBranch(updatedUserDTO.getBranch().getBytes());
//            }
            staff.setHospital(hospital);
            staff.setStatus(1);
            staff.setModifiedUser(loginUser);
            staff.setModifiedDate(LocalDate.now());
            boolean isSaved = staffService.saveStaff(staff , loginUser);

            if (isSaved) {
                Staff savedStaff = staffService.findByEmail(staff.getEmail() , loginUser);
                if (savedStaff == null) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_NOT_FOUND);
                }
                if (userRoles.contains(role)) {

                    User user = userService.findByUsername(loginUser.getUsername());
                    user.setUsername(updatedUserDTO.getUsername());
                    user.setRoles(role);
                    user.setPassword(encoder.encode(updatedUserDTO.getPassword()));
                    user.setName(updatedUserDTO.getName());
                    user.setHospital(hospital);
                    user.setStaff(savedStaff);
                    user.setModifiedUser(loginUser);
                    user.setModifiedDate(LocalDate.now());
                    boolean isSavedUser = userService.saveUser(user , loginUser);
                    if (isSavedUser) {
                           return ResponseEntity.ok(ExceptionMessages.UPDATE_SUCCESS);
                    } else {
                        return ResponseEntity.badRequest().body(ExceptionMessages.UPDATE_FAILED);
                    }
                }
                else {
                    return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                }
            } else {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while updateUser(): \n"+ExceptionUtils.getStackTrace(e)+"\n"+" Logged User :\n "+loginUser);

            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }


//
//        try {
//            String role = updatedUserDTO.getRoles().toUpperCase(); // Ensure case consistency
//
//            // Define allowed roles for User table
//            List<String> userRoles = Arrays.asList("ADMIN", "DOCTOR", "RECEPTIONIST", "SUPER_ADMIN");
//
//            User loginUser = userService.getAuthenticateUser();
//
//            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getHospitalId());
//
//            List<String> branchList = Arrays.stream(new String(hospital.getBranches()).split(","))
//                    .map(String::trim)
//                    .toList();
//
//            if (!branchList.contains(updatedUserDTO.getBranch().trim())) {
//                throw new RecordNotFoundException("Branch Not Found. Available Branches: " + branchList);
//            }
//
//            if (userRoles.contains(role)){
//
//                Optional<User> optionalUser = userService.getOptionalUserById(id);
//                if (optionalUser.isEmpty()){
//                    throw new RecordNotFoundException("User not found with this id "+id);
//                }
//
//                User user = optionalUser.get();
//                if (!user.getId().equals(loginUser.getId())) {
//                    throw new RecordNotFoundException("You enter an wrong ID. You associate with this ID " + loginUser.getId());
//                }
//
//                String password = updatedUserDTO.getPassword();
//                user.setUsername(updatedUserDTO.getUsername());
//                user.setPassword(encoder.encode(updatedUserDTO.getPassword()));
//                user.setHospital(hospital);
//                user.setAddress(updatedUserDTO.getAddress());
//                user.setBranch(updatedUserDTO.getBranch().getBytes());
//                user.setName(updatedUserDTO.getName());
//                user.setRoles(role);
//                user.setModifiedUser(loginUser);
//                user.setModifiedDate(LocalDate.now());
//
//                Staff staff = user.getStaff();
//                staff.setDoctorFee(null);
//
//                staff.setEmail(updatedUserDTO.getUsername());
//                staff.setName(updatedUserDTO.getName());
//                staff.setAddress(updatedUserDTO.getAddress().getBytes());
//                staff.setBranch(updatedUserDTO.getBranch().getBytes());
//                staff.setRoles(role);
//                staff.setHospital(hospital);
//                if (role.equals("DOCTOR")){
//                    staff.setDoctorFee(updatedUserDTO.getDoctorFee());
//                }
//                staff.setSalary(updatedUserDTO.getSalary());
//                staff.setModifiedUser(loginUser);
//                staff.setModifiedDate(LocalDate.now());
//                boolean savedStaff= staffService.saveStaff(staff);
//                user.setStaff(savedStaff);
//                User updatedUser= userService.updateUser(user,password);
//                return ResponseEntity.ok(updatedUser);
//            } else {
//                Optional<Staff> staffOptional = staffService.findById(id);
//                if (staffOptional.isEmpty()){
//                    throw new RecordNotFoundException("Staff not found with this id "+id);
//                }
//                Staff staff = staffOptional.get();
//                staff.setEmail(updatedUserDTO.getUsername());
//                staff.setName(updatedUserDTO.getName());
//                staff.setAddress(updatedUserDTO.getAddress());
//                staff.setBranch(updatedUserDTO.getBranch().getBytes());
//                staff.setRoles(role);
//                staff.setHospital(hospital);
//                staff.setDoctorFee(null);
//                staff.setSalary(updatedUserDTO.getSalary());
//                staff.setModifiedUser(loginUser);
//                staff.setModifiedDate(LocalDate.now());
//                return ResponseEntity.ok(staffService.saveStaff(staff));
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }



    @PutMapping("/updatePassword")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<String> updatePassword(@Valid @RequestBody AuthRequest request) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            User user = userService.findByUsername(request.getUsername());
            if (user==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USER_NOT_FOUND);
            }

            // Role-based restrictions
            if (loginUser.getRoles().equals("SUPER_ADMIN")) {
                // SUPER_ADMIN can reset passwords only for ADMINs
                if (!user.getRoles().equals("ADMIN")) {
                    return ResponseEntity.badRequest().body("SUPER_ADMIN can reset passwords only for ADMIN users.");
                }
            } else if (loginUser.getRoles().equals("ADMIN")) {
                // ADMIN can reset passwords only for DOCTOR and RECEPTIONIST
                if (user.getRoles().equals("SUPER_ADMIN")) {
                    return ResponseEntity.badRequest().body("You do not have access to reset passwords for this role.");
                }
                // Ensure the user belongs to the same hospital as the authenticated user
                if (user.getHospital().getId()!=(loginUser.getHospital().getId())) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.USER_NOT_FOUND);
                }
                // Prevent ADMIN from resetting their own password
//                if (authenticateUser.getUserId().equals(user.getUserId())) {
//                    throw new RecordNotFoundException("You cannot reset your own password. Please contact SUPER_ADMIN.");
//                }
            } else {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            // Update the password
            user.setPassword(encoder.encode(request.getPassword()));
            user.setModifiedUser(loginUser);
            user.setModifiedDate(LocalDate.now());
            boolean isSaved = userService.saveUser(user , loginUser);
            if (isSaved) {
                String password = request.getPassword();
                emailService.sendEmail(user.getUsername(), "Your Password Has Been Reset – Action Required", getUserPasswordResetTemplate(user.getUsername(), password, user.getHospital().getName()));
                return ResponseEntity.ok(user.getName() + " password update success!");
            } else {
                return ResponseEntity.badRequest().body(user.getName() + " password update failed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while updatePassword() : \n"+ExceptionUtils.getStackTrace(e) +"\n"+" Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }


    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            Staff staff = staffService.findById(id , loginUser);
            if (staff == null || staff.getHospital().getId() != (loginUser.getHospital().getId())) {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_NOT_FOUND);
            }


            staff.setStatus(0);
            staff.setModifiedUser(loginUser);
            staff.setModifiedDate(LocalDate.now());
            boolean isStaffSaved = staffService.saveStaff(staff , loginUser);
            if (isStaffSaved){
                User user = userService.findByUsername(staff.getEmail());
                if (user !=null){
                    user.setStatus(0); // Soft delete
                    user.setModifiedUser(loginUser);
                    user.setModifiedDate(LocalDate.now());
                   boolean isSaved = userService.saveUser(user , loginUser);
                   if (isSaved){
                       emailService.sendEmail(user.getUsername(), "Your " + user.getHospital().getName() + " Account Has Been Deactivated", getUserDeactivationTemplate(user.getUsername(), user.getHospital().getName()));
                       return ResponseEntity.ok("USER" +ExceptionMessages.ACCOUNT_DEACTIVATED);
                   }else {
                       return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                   }

                }else {
                    emailService.sendEmail(staff.getEmail(), "Your " + user.getHospital().getName() + " Account Has Been Deactivated", getUserDeactivationTemplate(staff.getEmail(), staff.getHospital().getName()));
                    return ResponseEntity.ok("Staff has been deactivated.");
                }
            }else {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }



//
//            if (optionalUser.isPresent()){
//                User user = optionalUser.get();
//
//
//            }else {
//                Optional<Staff> optionalStaff = staffService.findbyEmail(email);
//
//                if (optionalStaff.isEmpty()){
//                    throw new RecordNotFoundException("User/Staff not found with this Email "+email);
//                }
//                Staff staff = optionalStaff.get();
//                staff.setStatus(0);
//                staff.setModifiedUser(authenticateUser);
//                staff.setModifiedDate(LocalDate.now());
//                staffService.saveStaff(staff);
//                return ResponseEntity.ok("Staff has been deactivated.");
//            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while deleteUser() : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+" Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }


    public static String getUserRegistrationSuccessTemplate(String hospitalName, String altUsername, String altPassword, String role, Long id, BigDecimal salary) {
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
                "Welcome to " + hospitalName +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear User,</p>" +
                "<p>Your registration for the " + hospitalName + " platform has been successfully completed!</p>" +
                "<p>Here are your secure login details:</p>" +
                "<p><strong>Login ID:</strong> " + altUsername + "</p>" +
                "<p><strong>Access Key:</strong> " + altPassword + "</p>" +
                "<p><strong>Role:</strong> " + role + "</p>" +
                "<p><strong>Salary:</strong> $" + salary + "</p>" +
                "<p><strong>Associate ID:</strong> " + id + "</p>" +
                "<p>Please use these details to log in and change your password as soon as possible for added security.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    public String getStaffRegistrationSuccessTemplate(String hospitalName, String altUsername, String role, Long id, BigDecimal salary) {
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
                "Welcome to " + hospitalName +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear Staff Member,</p>" +
                "<p>Your registration for the " + hospitalName + " platform has been successfully completed!</p>" +
                "<p>Here are your login details:</p>" +
                "<p><strong>Login ID:</strong> " + altUsername + "</p>" +
                "<p><strong>Role:</strong> " + role + "</p>" +
                "<p><strong>Salary:</strong> $" + salary + "</p>" +
                "<p><strong>Associate ID:</strong> " + id + "</p>" +
                "<p>Please use these details to log in.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    public String getStaffReactivationTemplate(String staffName, String hospitalName) {
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
                "Staff Account Reactivated" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear " + staffName + ",</p>" +
                "<p>Your staff account at <strong>" + hospitalName + "</strong> has been successfully reactivated.</p>" +
                "<p>If you need any assistance, please contact our support team.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    public String getUserReactivationTemplate(String name, String hospitalName) {
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
                "}" +	           "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Account Reactivated" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear " + name + ",</p>" +
                "<p>We are pleased to inform you that your account has been reactivated.</p>" +
                "<p>You can now log in and continue using the application.</p>" +
                "<p>If you have any questions, feel free to reach out to our support team.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }


    public String getUserPasswordResetTemplate(String username, String newPassword, String hospitalName) {
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
                "Password Reset Notification" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear " + username + ",</p>" +
                "<p>Your password has been successfully reset by the administrator.</p>" +
                "<p>Please use the following temporary password to log in:</p>" +
                "<p><strong>New Password:</strong> " + newPassword + "</p>" +
                "<p>For security reasons, we strongly recommend that you change your password after logging in.</p>" +
                "<p>If you did not request this change, please contact the administrator immediately.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    public String getUserDeactivationTemplate(String username, String hospitalName) {
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
                "}" +	           "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "Account Deactivated" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear " + username + ",</p>" +
                "<p>Your account has been marked as inactive.</p>" +
                "<p>This could be due to one of the following reasons:</p>" +
                "<ul>" +
                "<li>You chose to deactivate your account.</li>" +
                "<li>An administrator deactivated your account.</li>" +
                "</ul>" +
                "<p>If you believe this is an error, please contact support.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }



}

