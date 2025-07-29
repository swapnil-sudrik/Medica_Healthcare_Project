package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.HospitalResponseDTO;
import com.fspl.medica_healthcare.dtos.StaffDTO;
import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.dtos.UserResponseDTO;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import com.fspl.medica_healthcare.utils.RoleConstants;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    @Autowired
    private OtpService otpService;

    private static final Logger log = Logger.getLogger(UserController.class);

    //add new field..arshad
    private final Set<Long> usersWhoNeedReminders = ConcurrentHashMap.newKeySet();

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

            Otp otp = otpService.getOtpByEmail(userDTO.getUsername());
            if (otp == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.OTP_NOT_VERIFIED);
            }
            if (!otp.isVerified()) {
                return ResponseEntity.badRequest().body(ExceptionMessages.OTP_NOT_VERIFIED);
            }
            // Define allowed roles for User table
            List<String> userRoles = RoleConstants.USER_ROLES;

            for (String r : userRoles) {
                if (role.equals(r)) {
                    break;
                } else if (role.contains(r)) {
                return ResponseEntity.badRequest().body(ExceptionMessages.ROLE_IS_NOT_CORRECT + userRoles);
                }
            }

            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getId());
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.HOSPITAL_NOT_FOUND);
            }

            User user = userService.userDtoToUser(userDTO);

            List<String> branchList = Arrays.stream(new String(hospital.getBranch()).split(","))
                    .map(String::trim)
                    .toList();
//NEW DHIRAJ UPDATED
            if (!branchList.contains(userDTO.getBranch().trim()) && loginUser.getCreatedUser().getRoles().equals("SUPER_ADMIN")) {
                return ResponseEntity.badRequest().body(ExceptionMessages.BRANCH_NOT_FOUND + branchList);
            }

            if (staffService.findByEmail(user.getUsername() , loginUser) != null){
                return ResponseEntity.badRequest().body("username/email already exist in staff enter other email address.");
            }
            // Check if the username already exists...
            if (userService.findByUsername(user.getUsername()) != null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.USERNAME_ALREADY_EXIST);
            }

            Staff staff = staffService.userDtoTOStaff(userDTO);

            if ("DOCTOR".equals(staff.getRoles()) && userDTO.getType() != null) {
                String type = userDTO.getType();
                if ("Regular".equals(type) || "Freelancer".equals(type)) {
                    staff.setType(type);
                } else {

                    return ResponseEntity.badRequest().body("Invalid type. Only 'Regular' or 'Freelancer' are allowed.");

                }
            }
            //add staff changes..dhiraj

            if (role.equals("DOCTOR")) {
                staff.setDoctorFee(userDTO.getDoctorFee());
            } else {
//                staff.setDoctorFee(BigDecimal.valueOf(0));
                staff.setDoctorFee(0.0);
            }
//NEW Dhiraj Updated
            if (loginUser.getCreatedUser().getRoles().equals("SUPER_ADMIN")) {
                staff.setBranch(userDTO.getBranch().getBytes());
            } else {
                staff.setBranch(loginUser.getBranch());
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

                    String namePart = userDTO.getName().substring(0, Math.min(userDTO.getName().length(), 3));
                    String contactPart = userDTO.getContactNumber().substring(0, 4);
                    String birthDatePart = userDTO.getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyy"));
                    String autoGeneratedPassword = namePart + contactPart + "@" + birthDatePart;


                    String maskedPassword = maskPassword(autoGeneratedPassword);
                    //if staff saved and password missing then again send with password then showing error username already exists.

                    user.setHospital(hospital);
                    user.setStaff(savedStaff);
                    // NEW DHIRAJ Updated
//                    if (role.equals("ADMIN")) {
//                        user.setBranch(hospital.getBranch());
//                    } else {
                        user.setBranch(userDTO.getBranch().getBytes());
//                    }
                    user.setRoles(role);
                    user.setCreatedUser(loginUser);
                    user.setModifiedUser(loginUser);
                    user.setCreatedDate(LocalDate.now());
                    user.setModifiedDate(LocalDate.now());
                    user.setStatus(1);
                    user.setPassword(encoder.encode(autoGeneratedPassword));
                    boolean isSavedUser = userService.saveUser(user , loginUser);
                    if (isSavedUser) {
                        User savedUser = userService.findByUsername(user.getUsername());
                        otpService.delete(otp);
                        //email user
                        emailService.sendEmail(savedUser.getUsername(), "Welcome to "+hospital.getName() +" - Your Registration is Complate", getUserRegistrationSuccessTemplate(hospital.getName(), savedUser.getUsername(),maskedPassword, savedUser.getRoles(),savedStaff.getId(), savedStaff.getSalary()));

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
           e.printStackTrace();
            log.error("An unexpected error occurred while createUser(): \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }

    private String maskPassword(String password) {
        if (password == null || password.length() < 4) {
            return "XXXX";
        }
        return password.substring(0, 2) + "X".repeat(password.length() - 4) + password.substring(password.length() - 2);
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

            if (user==null || (user.getHospital() !=null && user.getHospital().getId()!=(loginUser.getHospital().getId()))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USER_NOT_FOUND);
            }
            if (user.getRoles().equals("SUPER_ADMIN")){
                return ResponseEntity.badRequest().body(ExceptionMessages.USER_NOT_FOUND);
            }

            //..GET branch data

//            String loginBranch = new String(loginUser.getBranch()).trim();
//            String userBranch = new String(user.getBranch()).trim();
//                if(loginBranch.equalsIgnoreCase(userBranch)){
//                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                            .body("You are not unauthorized to access users from other branchs.");
//                }
            String loginBranch = loginUser.getBranch() != null ? new String(loginUser.getBranch()).trim() : null;
            String targetUserBranch = user.getBranch() != null ? new String(user.getBranch()).trim() : null;

            if (loginBranch != null && targetUserBranch != null && !loginBranch.equalsIgnoreCase(targetUserBranch)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not authorized to access users from other branches.");
            }

            ////////////////////////////////////////
            UserResponseDTO response = convertToDTO(user, new HashSet<>());
            if (response==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            return ResponseEntity.ok(response);
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

            List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                    .map(String::trim)
                    .toList();

            List<User> users = new ArrayList<>();
            for (String branch : branchList){
                List<User> branchUsers = userService.getAllUsersByHospital(loginUser, branch.getBytes());
                if (branchUsers !=null){
                    users.addAll(branchUsers);
                }
//                for (User user : branchUsers){
//                    users.add(user);
//                }
            }

            if (users == null){
                return ResponseEntity.ok(ExceptionMessages.USERS_NOT_FOUND);
            }

            // Get the logged-in user's branch as string..
//            String loginBranch = new String(loginUser.getBranch()).trim();
            //////////////////////////////////

            List<UserResponseDTO> responseDTOS = new ArrayList<>();

            for (User user : users) {
                // Check for same branch and not SUPER_ADMIN...
//                if (user.getBranch() != null &&
//                        loginBranch.equals(new String(user.getBranch()).trim()) &&
//                        !user.getRoles().equalsIgnoreCase("SUPER_ADMIN")) {
//
//                }
                responseDTOS.add(convertToDTO(user, new HashSet<>()));

            }

            if (responseDTOS.isEmpty()) {
                return ResponseEntity.ok(ExceptionMessages.USERS_NOT_FOUND);
            }

            return ResponseEntity.ok(responseDTOS);

        } catch (Exception e) {
            e.printStackTrace();
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
//            List<User> activeUsers = userService.getAllActiveUsersOfHospital(loginUser);
//            if (activeUsers == null){
//                return ResponseEntity.ok(ExceptionMessages.USERS_NOT_FOUND);
//            }
            List<UserResponseDTO> responseDTOS = new ArrayList<>();

//            for (User user : activeUsers){
//                responseDTOS.add(convertToDTO(user, new HashSet<>()));
//            }
            //ADD Branch data get.....
//            for (User user : activeUsers) {
//                // Only include users from same hospital and same branch
//                boolean isSameHospital = user.getHospital() != null &&
//                        loginUser.getHospital() != null &&
//                        user.getHospital().getId() == loginUser.getHospital().getId();
//
//                boolean isSameBranch = Arrays.equals(user.getBranch(), loginUser.getBranch());
//
//                if (isSameHospital && isSameBranch) {
//                    responseDTOS.add(convertToDTO(user, new HashSet<>()));
//                }
//            }

            // Get logged-in user's branch list (could be multiple if master admin)
                    List<String> branchList = Arrays.stream(new String(loginUser.getBranch()).split(","))
                                                    .map(String::trim)
                                                    .toList();

           // Loop through branches and get active users for each
            for (String branch : branchList) {
                List<User> activeUsers = userService.getAllActiveUsersByHospitalAndBranch(loginUser, branch.getBytes());

                if (activeUsers != null) {
                    for (User user : activeUsers) {
                        //   Avoid adding SUPER_ADMIN users
                        if (!user.getRoles().equalsIgnoreCase("SUPER_ADMIN")) {
                            responseDTOS.add(convertToDTO(user, new HashSet<>()));
                        }
                    }
                }
            }

                if (responseDTOS.isEmpty()) {
                return ResponseEntity.ok("No active users found in your branch.");
            }
            return ResponseEntity.ok(responseDTOS);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllActiveUsersOfHospital(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User :\n "+loginUser);
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
            User user = userService.getUserById(id, loginUser);

            if (user == null || user.getHospital().getId()!=(loginUser.getHospital().getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.STAFF_NOT_FOUND);
            }

            // reactive branch wise
            boolean isSameHospital = user.getHospital().getId() == loginUser.getHospital().getId();
            boolean isSameBranch = Arrays.equals(user.getBranch(), loginUser.getBranch());

            boolean isCreatedByLoginUser = user.getCreatedUser() != null &&
                    user.getCreatedUser().getId() == loginUser.getId();

            if (!isSameHospital || (!isSameBranch && !isCreatedByLoginUser)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("You are not authorized to reactivate users from other branches.");
            }
            //////////

            // Check if the user is already active
            if (user.getStatus() == 1) {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_IS_ACTIVE);
            }
            user.setStatus(1);
            user.setModifiedUser(loginUser);
            user.setModifiedDate(LocalDate.now());
            boolean isUserSaved = userService.saveUser(user , loginUser);
            Staff staff = staffService.findByEmail(user.getUsername(), loginUser);
            if (isUserSaved && staff != null) {
                staff.setStatus(1);
                staff.setModifiedUser(loginUser);
                staff.setModifiedDate(LocalDate.now());
                boolean isStaffSaved = staffService.saveStaff(staff, loginUser);
                if (isStaffSaved) {
                    //email
                    emailService.sendEmail(user.getUsername(), "Your " + user.getHospital().getName() + " Account Has Been Activated", getUserReactivationTemplate(user.getName(), user.getHospital().getName()));
                    return ResponseEntity.ok("User"+ ExceptionMessages.ACCOUNT_ACTIVATED);
                }
            }
            return ResponseEntity.ok("User"+ ExceptionMessages.SOMETHING_WENT_WRONG);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while reactivateUser(): \n"+ExceptionUtils.getStackTrace(e)+"\n"+" Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    //update for self
    @PutMapping("/update")
    @PreAuthorize("hasAnyAuthority('DOCTOR','SUPER_ADMIN', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<String> updateUser(@RequestBody @Valid UserDTO updatedUserDTO) {
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

//           Staff checkUsername = staffService.findByEmail(updatedUserDTO.getUsername(), loginUser);
//
//           if (!checkUsername.getEmail().equals(loginUser.getUsername())){
//               return ResponseEntity.badRequest().body("This Username Already taken. use another username");
//           }

            Hospital hospital = hospitalService.getHospitalById(loginUser.getHospital().getId());
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.HOSPITAL_NOT_FOUND);
            }

            if (!loginUser.getRoles().equals(role) && updatedUserDTO.getRoles() !=null){
                return ResponseEntity.badRequest().body("You don't have access to change your role [ "+loginUser.getRoles()+" ] to [ "+updatedUserDTO.getRoles()+" ].");
            }

//            if (!loginUser.getStaff().getSalary().equals(updatedUserDTO.getSalary()) && updatedUserDTO.getSalary() != null){
//                return ResponseEntity.badRequest().body("You cant edit your salary");
//            }


            if (updatedUserDTO.getSalary() != 0.0 && loginUser.getStaff().getSalary() != updatedUserDTO.getSalary()){
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

                    // comment out due to error solve after some time
//                    if (updatedUserDTO.getPassword() != null && !user.getPassword().equals(encoder.encode(updatedUserDTO.getPassword()))){
//                        user.setPassword(encoder.encode(updatedUserDTO.getPassword()));
//                    }else {
//                        user.setPassword(user.getPassword());
//                    }


                    user.setName(updatedUserDTO.getName());
                    user.setHospital(hospital);
                    user.setStaff(savedStaff);
                    user.setModifiedUser(loginUser);
                    user.setModifiedDate(LocalDate.now());
                    user.setLastUpdatedPasswordDate(LocalDate.now());
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
    }


    @GetMapping("/getByToken")
    @PreAuthorize("hasAnyAuthority('DOCTOR','SUPER_ADMIN', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<?> getUserByToken(){
        User loginUser = null;
        try{
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USER_NOT_FOUND);
            }
            UserResponseDTO userResponseDTO = convertToDTO(loginUser , new HashSet<>());

            return ResponseEntity.ok(userResponseDTO);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getUserByToken(): \n"+ExceptionUtils.getStackTrace(e)+"\n"+" Logged User :\n "+loginUser);

            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USERNAME_NOT_FOUND);
            }

            Otp otp = otpService.getOtpByEmail(request.getUsername());
            if (otp == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.OTP_NOT_VERIFIED);
            }
            if (!otp.isVerified()) {
                return ResponseEntity.badRequest().body(ExceptionMessages.OTP_NOT_VERIFIED);
            }

            // Role-based restrictions
            if (loginUser.getRoles().equals("SUPER_ADMIN")) {
                // SUPER_ADMIN can reset passwords only for ADMINs
                if (!user.getRoles().equals("ADMIN")) {
                    return ResponseEntity.badRequest().body("SUPER_ADMIN can reset passwords only for ADMIN users.");
                }
            } else if (loginUser.getRoles().equals("ADMIN")) {
                // ADMIN can reset passwords only for DOCTOR, RECEPTIONIST or other role
                if (user.getRoles().equals("SUPER_ADMIN")) {
                    return ResponseEntity.badRequest().body("You do not have access to reset passwords for this role.");
                }
                // Ensure the user belongs to the same hospital as the authenticated user
                if (user.getHospital().getId()!=(loginUser.getHospital().getId())) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.USER_NOT_FOUND);
                }

            } else {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            // Update password
            user.setPassword(encoder.encode(request.getPassword()));
            user.setModifiedUser(user);
            user.setModifiedDate(LocalDate.now());
            user.setLastUpdatedPasswordDate(LocalDate.now());
            boolean isSaved = userService.saveUser(user , user);
            if (isSaved) {
               otpService.delete(otp);
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


            User user = userService.getUserById(id, loginUser);
//            Staff staff = staffService.findById(id , loginUser);

            if (user == null || user.getHospital().getId() != (loginUser.getHospital().getId())) {
                return ResponseEntity.badRequest().body(ExceptionMessages.STAFF_NOT_FOUND);
            }

            // Ensure same hospital....
            if (user.getHospital() == null || loginUser.getHospital() == null ||
                    user.getHospital().getId() != loginUser.getHospital().getId()) {
                return ResponseEntity.badRequest().body(ExceptionMessages.UNAUTHORIZED_HOSPITAL_ACCESS);
            }

           // Add branch comparison logic (only this part is new)....
            if (user.getCreatedUser() != null && loginUser.getId() != user.getCreatedUser().getId()) {
                if (!Arrays.equals(user.getBranch(), loginUser.getBranch())) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.UNAUTHORIZED_BRANCH_ACCESS);
                }
            }

            user.setStatus(0);
            user.setModifiedUser(loginUser);
            user.setModifiedDate(LocalDate.now());
            boolean isUserSaved = userService.saveUser(user, loginUser);
            Staff staff = staffService.findByEmail(user.getUsername(), loginUser);
            if (isUserSaved && staff!=null){
                staff.setStatus(0); // Soft delete
                staff.setModifiedUser(loginUser);
                staff.setModifiedDate(LocalDate.now());
                   boolean isStaffSaved = staffService.saveStaff(staff, loginUser);
                   if (isStaffSaved){
                       emailService.sendEmail(user.getUsername(), "Your " + user.getHospital().getName() + " Account Has Been Deactivated", getUserDeactivationTemplate(user.getUsername(), user.getHospital().getName()));
                       return ResponseEntity.ok("USER" +ExceptionMessages.ACCOUNT_DEACTIVATED);
                   }else {
                       return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                   }
            }else {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while deleteUser() : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+" Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }


    @PostMapping("/unlockUser/{emailId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> unlockUser(@PathVariable String emailId){
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            User lockedUser = userService.findByUsername(emailId);
            if (lockedUser==null || lockedUser.getRoles().equals("SUPER_ADMIN")){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USER_NOT_FOUND);
            }

            if (loginUser.getHospital().getId() != lockedUser.getHospital().getId()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.USER_NOT_FOUND);
            }

            lockedUser.setAccountLocked(false);
            lockedUser.setLockTime(null);
            lockedUser.setFailedLoginAttempts(3);
            userService.saveUser(lockedUser, loginUser);
            return ResponseEntity.ok("User has been Unlocked.");

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while unlockUser() : \n"+ ExceptionUtils.getStackTrace(e) +"\n"+" Logged User :\n "+loginUser);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    public UserResponseDTO convertToDTO(User user, Set<Long> visited) {
        try{
            if (user == null) return null;
            UserResponseDTO dto = new UserResponseDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setName(user.getName());
            dto.setRoles(user.getRoles());
            dto.setCreatedDate(user.getCreatedDate());
            dto.setModifiedDate(user.getModifiedDate());
            dto.setStatus(user.getStatus());
            dto.setBranch(new String(user.getBranch()));
            dto.setContactNumber(user.getContactNumber());
            dto.setDateOfBirth(user.getDateOfBirth());
            if (user.getStaff() !=null) {
                dto.setAddress(new String(user.getStaff().getAddress()));
            }
            // If already visited, avoid infinite recursion but still include shallow object
            if (user.getCreatedUser() != null) {

                if (visited.contains(user.getCreatedUser().getId())) {
                    dto.setCreatedUser(shallowUser(user.getCreatedUser()));
                    System.out.println("shallow created");
                } else {
                    visited.add(user.getCreatedUser().getId());
                    dto.setCreatedUser(convertToDTO(user.getCreatedUser(), visited));
                    System.out.println("convert created");

                }
            }

            if (user.getModifiedUser() != null) {
                if (visited.contains(user.getModifiedUser().getId())) {
                    dto.setModifiedUser(shallowUser(user.getModifiedUser()));
                } else {
                    visited.add(user.getModifiedUser().getId());
                    dto.setModifiedUser(convertToDTO(user.getModifiedUser(), visited));
                }
            }

            if (user.getHospital() != null) {
                HospitalResponseDTO hospitalDTO = new HospitalResponseDTO();
                hospitalDTO.setId(user.getHospital().getId());
                hospitalDTO.setName(user.getHospital().getName());
                hospitalDTO.setAddress(new String(user.getHospital().getAddress()));
                hospitalDTO.setContactNumber(user.getHospital().getContactNumber());
                hospitalDTO.setEmailId(user.getHospital().getEmailId());
                hospitalDTO.setDepartments(new String(user.getHospital().getDepartments()));
                hospitalDTO.setBranch(new String(user.getHospital().getBranch()));
                hospitalDTO.setNumberOfUsers(user.getHospital().getNumberOfUsers());
                hospitalDTO.setStatus(user.getHospital().getStatus());
                dto.setHospital(hospitalDTO);
            }

            if (user.getStaff() != null) {
                StaffDTO staffDTO = new StaffDTO();
                staffDTO.setId(user.getStaff().getId());
                staffDTO.setEmail(user.getStaff().getEmail());
                staffDTO.setName(user.getStaff().getName());
                staffDTO.setAddress(new String(user.getStaff().getAddress()));
                staffDTO.setBranch(new String(user.getStaff().getBranch()));
                staffDTO.setRoles(user.getStaff().getRoles());
                staffDTO.setDoctorFee(user.getStaff().getDoctorFee());
                staffDTO.setSalary(user.getStaff().getSalary());
                staffDTO.setStatus(user.getStaff().getStatus());
                staffDTO.setModifiedDate(user.getStaff().getModifiedDate());
                staffDTO.setCreatedDate(user.getStaff().getCreatedDate());
                dto.setStaff(staffDTO);
            }
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private UserResponseDTO shallowUser(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRoles(user.getRoles());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setModifiedDate(user.getModifiedDate());
        dto.setStatus(user.getStatus());
        dto.setBranch(new String(user.getBranch()));
        if (user.getStaff() !=null){
            dto.setAddress(new String(user.getStaff().getAddress()));
        }
        return dto;
    }

//    public static String getUserRegistrationSuccessTemplate(String hospitalName, String altUsername, String altPassword, String role, Long id, double salary) {
//        return "<!DOCTYPE html>" +
//                "<html>" +
//                "<head>" +
//                "<style>" +
//                "body {" +
//                "    font-family: Arial, sans-serif;" +
//                "    margin: 0;" +
//                "    padding: 0;" +
//                "    background-color: #f4f4f9;" +
//                "}" +
//                ".container {" +
//                "    width: 100%;" +
//                "    max-width: 600px;" +
//                "    margin: 20px auto;" +
//                "    background: #ffffff;" +
//                "    border-radius: 10px;" +
//                "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
//                "    overflow: hidden;" +
//                "}" +
//                ".header {" +
//                "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
//                "    color: #ffffff;" +
//                "    text-align: center;" +
//                "    padding: 20px;" +
//                "    font-size: 24px;" +
//                "}" +
//                ".body {" +
//                "    padding: 20px;" +
//                "    line-height: 1.6;" +
//                "    color: #333333;" +
//                "}" +
//                ".footer {" +
//                "    background-color: #f4f4f9;" +
//                "    color: #888888;" +
//                "    text-align: center;" +
//                "    padding: 10px;" +
//                "    font-size: 12px;" +
//                "}" +
//                "</style>" +
//                "</head>" +
//                "<body>" +
//                "<div class=\"container\">" +
//                "<div class=\"header\">" +
//                "Welcome to " + hospitalName +
//                "</div>" +
//                "<div class=\"body\">" +
//                "<p>Dear User,</p>" +
//                "<p>Your registration for the " + hospitalName + " platform has been successfully completed!</p>" +
//                "<p>Here are your secure login details:</p>" +
//                "<p><strong>Login ID:</strong> " + altUsername + "</p>" +
//                "<p><strong>Access Key:</strong> " + altPassword + "</p>" +
//                "<p><strong>Role:</strong> " + role + "</p>" +
//                "<p><strong>Salary:</strong> $" + salary + "</p>" +
//                "<p><strong>Associate ID:</strong> " + id + "</p>" +
//                "<p>Please use these details to log in and change your password as soon as possible for added security.</p>" +
//                "</div>" +
//                "<div class=\"footer\">" +
//                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
//                "</div>" +
//                "</div>" +
//                "</body>" +
//                "</html>";
//    }


    public static String getUserRegistrationSuccessTemplate(String hospitalName, String altUsername, String altPassword, String role, Long id, double salary) {
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
                "<p><strong><passwordhint:</strong> Password Hint:First 3 Letter of your name followed by first 4 digits of your mobile number then press @ and your birth year</p>"+
                "<p><strong>Sample Password:</strong> ana7798@2001</p>"+

                "<p>Please use these details to log in and change your password as soon as possible for added security.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    public String getStaffRegistrationSuccessTemplate(String hospitalName, String altUsername, String role, Long id, double salary) {
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

    //new added..arshad

    @Scheduled(cron = "0 */5 * * * ?")
    //@Scheduled(cron = "0 0 0 * * ?",zone = "Asia/Kolkata") // Run once a day at midnight
    public void sendPasswordUpdateReminder() {
        try {
            LocalDate now = LocalDate.now();
            List<User> users = userService.getAllUsers();

            if (users.isEmpty()) {
                return;
            }
            // Remove users who have updated their password since the last reminder
            removeUsersWhoUpdatedPassword(users, now);
            // Send reminders to users who haven't updated their password in the last 3 months
            for (User user : users) {
                processUser(user, now);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error("Unexpected error in password reminder scheduler: \n" + ExceptionUtils.getStackTrace(e));
        }
    }

    private void processUser(User user, LocalDate now) {
        if (user == null || user.getLastUpdatedPasswordDate() == null) {
            return;
        }
        // Check if the user hasn't updated their password in the last 3 months
        // if (user.getLastUpdatedPassword().plusMonths(3).isBefore(now)){
        if (user.getLastUpdatedPasswordDate().plusMonths(3).isBefore(now)) {
            if (usersWhoNeedReminders.contains(user.getId())) {
                sendReminderEmail(user);
            } else {
                // Send the first reminder
                sendReminderEmail(user);
                usersWhoNeedReminders.add(user.getId());
            }
        }
    }

    private void removeUsersWhoUpdatedPassword(List<User> users, LocalDate now) {
        // Collect all user IDs that have updated their password recently
        Set<Long> usersToRemove = users.stream()
                .filter(user -> user != null && user.getLastUpdatedPasswordDate() != null)
                .filter(user -> user.getLastUpdatedPasswordDate().isAfter(now.minusMonths(3)))
                .map(User::getId)
                .collect(Collectors.toSet());

        // Remove those users from the reminder list
        usersWhoNeedReminders.removeIf(userId -> usersToRemove.contains(Long.valueOf(userId)));
    }

    private void sendReminderEmail(User user) {
        try {
            if (!isUserDataValid(user)) {
                return;
            }
            String emailContent = generateEmailContent(user.getName());
            emailService.sendEmail(user.getUsername(), "Password Change Alert", emailContent);
        } catch (RuntimeException e) {
            e.printStackTrace();
            log.error("Error sending email to user " + ExceptionUtils.getStackTrace(e));
        }
    }

    private boolean isUserDataValid(User user) {
        return user != null && user.getUsername() != null && !user.getUsername().isBlank()
                && user.getName() != null && !user.getName().isBlank();
    }

    private String generateEmailContent(String name) {
        return """
                <html>
                    <head>
                        <style>
                            body {
                                font-family: Arial, sans-serif;
                                margin: 0;
                                padding: 0;
                                background-color: #f4f4f9;
                            }
                            .email-container {
                                width: 100%%;
                                max-width: 600px;
                                margin: 20px auto;
                                background: #ffffff;
                                border-radius: 10px;
                                box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                                overflow: hidden;
                            }
                            .header {
                                background: linear-gradient(135deg, #42a5f5, #0d47a1);
                                color: #ffffff;
                                text-align: center;
                                padding: 20px;
                                font-size: 24px;
                            }
                            .content {
                                padding: 20px;
                                line-height: 1.6;
                                color: #333333;
                            }
                            .footer {
                                background-color: #f4f4f9;
                                color: #888888;
                                text-align: center;
                                padding: 10px;
                                font-size: 12px;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="email-container">
                            <div class="header">
                                Medica Healthcare
                            </div>
                            <div class="content">
                                <p>Dear %s,</p>
                                <p>We noticed that you haven’t changed your password in the last 3 months. For your safety, we recommend updating it now to keep your account secure.</p>
                                <p>To update your password, please log in to your account and follow the instructions in the <strong>Security Settings</strong> section.</p>
                                <p><strong>Note:</strong> This is an automated email. Please do not reply to this message.</p>
                            </div>
                            <div class="footer">
                                &copy; 2025 Medica Healthcare. All rights reserved.
                            </div>
                        </div>
                    </body>
                </html>
                """.formatted(name);
    }



}

