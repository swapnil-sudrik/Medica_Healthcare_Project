package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.LoginResponse;
import com.fspl.medica_healthcare.dtos.Notification;
import com.fspl.medica_healthcare.models.AuthRequest;
import com.fspl.medica_healthcare.models.LoginDetails;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.JwtService;
import com.fspl.medica_healthcare.services.LoginDetailService;
import com.fspl.medica_healthcare.services.NotificationService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.Synchronized;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LoginDetailService loginDetailService;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    private static final Logger log = Logger.getLogger(AuthController.class);


    @GetMapping("/get")
    public String getInfo(HttpServletRequest request){
        String userAgentString = request.getHeader("User-Agent");

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);

        String browser = userAgent.getBrowser().getName();
        String browserVersion = userAgent.getBrowserVersion() != null
                ? userAgent.getBrowserVersion().getVersion()
                : "Unknown";

        String os = userAgent.getOperatingSystem().getName();
        String deviceType = userAgent.getOperatingSystem().getDeviceType().getName();

       return String.format("Browser: %s %s, OS: %s, Device: %s",
                browser, browserVersion, os, deviceType);
    }


    @Synchronized
    @PostMapping("/login")
    public ResponseEntity<?> authenticateAndGetToken(
            @Valid @RequestBody AuthRequest authRequest,
            HttpServletRequest request) {
        User user = null;
        String clientIp = getClientIp(request, authRequest);

        try {
            String userAgent1 = request.getHeader("User-Agent");
            System.out.println("Device/User-Agent: " + userAgent1);



            /// Retrieve the user by username
            user = userService.findByUsername(authRequest.getUsername());

            if (user == null) {
                log.error("Failed login attempt - Username: " + authRequest.getUsername() + ", IP: " + clientIp + ", Time: " + LocalDateTime.now());
                return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_USERNAME_PASSWORD);
            }
            if (!user.getRoles().equals("SUPER_ADMIN")){
                if (user.getHospital().getStatus()==0){
                    return ResponseEntity.badRequest().body(ExceptionMessages.HOSPITAL_IS_INACTIVE);
                }
                // Check if the user is active or not
                if (user.getStatus() == 0) {
                    return ResponseEntity.badRequest().body(ExceptionMessages.USER_IS_NOT_ACTIVE);
                }
            }

            /// deepali - lock user when enter 3 time wrong password and again unlock when 24 hours pass from current time
            if (user.isAccountLocked() && user.getLockTime().plusHours(24).isBefore(LocalDateTime.now())){
                user.setFailedLoginAttempts(3);
                user.setLockTime(null);
                user.setAccountLocked(false);
                userService.saveUser(user, user);
            }

            if (user.isAccountLocked()){
                return ResponseEntity.badRequest().body(ExceptionMessages.ACCOUNT_LOCKED);
            }


//            1 . new UsernamePasswordAuthenticationToken() = This creates an instance of UsernamePasswordAuthenticationToken, which is a standard Spring Security class representing a login attempt with a username and password.
//            2 . authenticationManager.authenticate(...) = 1) Delegates more AuthenticationProviders (like DaoAuthenticationProvider).
//                                                          2) The provider attempts to load the user from the database (via UserDetailsService) using the username.
//                                                          3) If a user is found, the provider then checks the password using the configured PasswordEncoder.
//                                                          4) If everything matches, it returns a fully authenticated Authentication object (with roles, authorities, etc.).

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
            if (authentication.isAuthenticated()) {
                // Create session manually if not exists
                HttpSession session = request.getSession(true); //Ensures session is created
                LoginDetails loginDetails = loginDetailService.findByUsername(user.getUsername());

                // Send notification to the user who just logged in
                String username = authRequest.getUsername();
                Notification loginNotification = new Notification(
                        UUID.randomUUID().toString(),
                        "Welcome back, " + username + "! 🎉",
                        Instant.now()
                );
                // Save in memory
                notificationService.addNotification(username, loginNotification);
                // Push to frontend via WebSocket
                messagingTemplate.convertAndSendToUser(username, "/user/notifications", loginNotification);

                user.setFailedLoginAttempts(3);
                user.setLockTime(null);
                user.setAccountLocked(false);
              boolean isUserSaved=  userService.saveUser(user, user);

                if (!isUserSaved){
                    return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                }

                if (loginDetails == null) {
                    LoginDetails newLoginDetails = new LoginDetails();
                    boolean isSavedLoginDetails = loginDetailService.saveLoginDetails(setLoginDetails(newLoginDetails, user, request, clientIp));
                    if (!isSavedLoginDetails) {
                        return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                    }
                }else {
                    boolean isLoginDetailSaved = loginDetailService.saveLoginDetails(setLoginDetails(loginDetails, user, request, clientIp));
                    if (!isLoginDetailSaved){
                        return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                    }
                }
                String token = encryptionUtil.encrypt(jwtService.generateToken(authRequest.getUsername(), user));
                LoginResponse response = new LoginResponse();
                response.setId(user.getId());
                response.setRoles(user.getRoles());
                response.setToken(token);
                session.setAttribute("jwtToken", token);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(ExceptionMessages.AUTHENTICATION_FAILED);
        } catch (BadCredentialsException e) {
            e.printStackTrace();
            if (!user.getRoles().equals("SUPER_ADMIN")){
                user.setFailedLoginAttempts(user.getFailedLoginAttempts()-1);
                if (user.getFailedLoginAttempts()<=0){
                    user.setAccountLocked(true);
                    user.setLockTime(LocalDateTime.now());
                }
                userService.saveUser(user, user);
            }
            log.error("Error: \n " + ExceptionUtils.getStackTrace(e));
            log.error("Failed login attempt - Username: " + authRequest.getUsername() + ", IP: " + clientIp + ", Time: " + LocalDateTime.now());
            return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_USERNAME_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while login: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User Id:\n " + user.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    private LoginDetails setLoginDetails(LoginDetails loginDetails,User user, HttpServletRequest request, String clientIp){

        String userAgentString = request.getHeader("User-Agent");

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);

        String browser = userAgent.getBrowser().getName();
        String browserVersion = userAgent.getBrowserVersion() != null
                ? userAgent.getBrowserVersion().getVersion()
                : "Unknown";

        String os = userAgent.getOperatingSystem().getName();
        String deviceType = userAgent.getOperatingSystem().getDeviceType().getName();

        System.out.println(String.format("Browser: %s %s, OS: %s, Device: %s", browser, browserVersion, os, deviceType));
        loginDetails.setIpAddress(clientIp);
        loginDetails.setCreated(user);
        loginDetails.setUsername(user.getUsername());
        loginDetails.setLoginAt(LocalDateTime.now());
        loginDetails.setBrowser(browser);
        loginDetails.setBrowserVersion(browserVersion);
        loginDetails.setOperatingSystem(os);
        loginDetails.setDeviceType(deviceType);
        return loginDetails;
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        User user = null;
        try {

            HttpSession session = request.getSession(false); // Don't create a new session if it doesn't exist
          String encryptedToken = null;
            if (session != null) {
                encryptedToken = session.getAttribute("jwtToken").toString();
                session.removeAttribute("jwtToken");
                session.invalidate();

            }

            // 2. Check if JWT token is present in the Authorization header
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (encryptedToken==null &&  authHeader != null && authHeader.startsWith("Bearer ")) {
                encryptedToken = authHeader.substring(7);
            }

            if (encryptedToken==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Token Not Found in Request. please login.");
            }

            String decryptedToken = encryptionUtil.decrypt(encryptedToken);

            String username = jwtService.extractUsername(decryptedToken);
            LoginDetails loginDetails = loginDetailService.findByUsername(username);

            if (loginDetails == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.LOGIN_DETAILS_NOT_FOUND);
            }
            // Retrieve the user by username
            user = userService.findByUsername(username);

            if (user == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            loginDetailService.delete(loginDetails);

            // Send notification to the user who just logged in
            Notification loginNotification = new Notification(
                    UUID.randomUUID().toString(),
                    "Logged Out, " + username + "! 😮",
                    Instant.now()
            );
            // Save in memory
            notificationService.addNotification(username, loginNotification);
            // Push to frontend via WebSocket
            messagingTemplate.convertAndSendToUser(username, "/user/notifications", loginNotification);


            return ResponseEntity.ok(ExceptionMessages.LOGGED_OUT_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while login : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User Id:\n " + user.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }

    private String getClientIp(HttpServletRequest request, AuthRequest authRequest) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            log.error("An unexpected error occurred while getClientIp() : \n" + ExceptionUtils.getStackTrace(e) + "\n" + " Username : \n " + authRequest.getUsername());
        return "";
        }

    }

}
