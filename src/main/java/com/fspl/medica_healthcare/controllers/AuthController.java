package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.LoginResponse;
import com.fspl.medica_healthcare.models.AuthRequest;
import com.fspl.medica_healthcare.models.LoginDetails;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.JwtService;
import com.fspl.medica_healthcare.services.LoginDetailService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;


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

    private static final Logger log = Logger.getLogger(AuthController.class);

    @PostMapping("/login")
    public synchronized ResponseEntity<?> authenticateAndGetToken(
            @Valid @RequestBody AuthRequest authRequest,
            HttpServletRequest request,
            @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent) {
        System.out.println("===authcontroller==Call==");
        User user = null;
        String clientIp = getClientIp(request, authRequest);
        try {
            // Retrieve the user by username
            user = userService.findByUsername(authRequest.getUsername());

            if (user == null) {
                log.warn("Failed login attempt - Username: " + authRequest.getUsername() + ", IP: " + clientIp + ", User-Agent: " + userAgent + ", Time: " + LocalDateTime.now());
                return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_USERNAME_PASSWORD);
            }
            // Check if the user is active or not
            if (user.getStatus() == 0) {
                return ResponseEntity.badRequest().body(ExceptionMessages.USER_IS_NOT_ACTIVE);
            }

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
            if (authentication.isAuthenticated()) {
                // 🔹 Create session manually if not exists
                HttpSession session = request.getSession(true); // ✅ Ensures session is created
                LoginDetails loginDetails = loginDetailService.findByUsername(user.getUsername());
                LoginResponse response = new LoginResponse();
                response.setId(user.getId());
                response.setRoles(user.getRoles());

                if (loginDetails == null) {
                    LoginDetails newLoginDetails = new LoginDetails();
                    String token = jwtService.generateToken(authRequest.getUsername());
                    newLoginDetails.setToken(token);
                    newLoginDetails.setCreated(user);
                    newLoginDetails.setUsername(user.getUsername());
                    newLoginDetails.setIssuedAt(LocalDate.now());


                    session.setAttribute("JWT_TOKEN", token);
                    // Manually set JSESSIONID in response for debugging
//                    httpServletResponse.setHeader("Set-Cookie", "JSESSIONID=" + session.getId() + "; Path=/; HttpOnly; SameSite=None; Secure");

                    boolean isSavedLoginDetails = loginDetailService.saveLoginDetails(newLoginDetails);
                    if (isSavedLoginDetails) {
                        LoginDetails savedLoginDetails = loginDetailService.findByUsername(user.getUsername());
                        if (savedLoginDetails == null) {
                            return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                        }
                        response.setToken(savedLoginDetails.getToken());
                        return ResponseEntity.ok(response);
                    }
                } else {
                    response.setToken(loginDetails.getToken());
                    session.setAttribute("JWT_TOKEN", loginDetails.getToken());
//                    httpServletResponse.setHeader("Set-Cookie", "JSESSIONID=" + session.getId() + "; Path=/; HttpOnly; SameSite=None; Secure");
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.badRequest().body(ExceptionMessages.AUTHENTICATION_FAILED);
        } catch (BadCredentialsException e) {

            log.error("Error: \n " + ExceptionUtils.getStackTrace(e));
            log.error("Failed login attempt - Username: " + authRequest.getUsername() + ", IP: " + clientIp + ", User-Agent: " + userAgent + ", Time: " + LocalDateTime.now());
            return ResponseEntity.badRequest().body(ExceptionMessages.INVALID_USERNAME_PASSWORD);
        } catch (Exception e) {
            log.error("An unexpected error occurred while login: \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User Id:\n " + user.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        User user = null;
        try {
            HttpSession session = request.getSession(false); // Don't create a new session if it doesn't exist
            if (session != null) {
                String jwtToken = session.getAttribute("JWT_TOKEN").toString();
                String username = jwtService.extractUsername(jwtToken);
                LoginDetails loginDetails = loginDetailService.findByUsername(username);
                loginDetailService.delete(loginDetails);
                session.removeAttribute("JWT_TOKEN");
                session.invalidate();
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }

            String token = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
                System.out.println(token);
            } else {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            String username = jwtService.extractUsername(token);
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
            return ResponseEntity.ok(ExceptionMessages.LOGGED_OUT_SUCCESS);
        } catch (Exception e) {
            log.error("An unexpected error occurred while login : \n" + ExceptionUtils.getStackTrace(e) + "\n" + "Logged User Id:\n " + user.getId());
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }

    }


//
//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
//        User user = null;
//        try {
//            if (token.startsWith("Bearer ")) {
//                token = token.substring(7);
//                System.out.println(token);
//            }else {
//                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
//            }
//            String username = jwtService.extractUsername(token);
//            LoginDetails loginDetails =loginDetailService.findByUsername(username);
//            if (loginDetails == null){
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.LOGIN_DETAILS_NOT_FOUND);
//            }
//            // Retrieve the user by username
//            user = userService.findByUsername(username);
//
//            if (user==null) {
//                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
//            }
//
//            loginDetailService.delete(loginDetails);
//            return ResponseEntity.ok(ExceptionMessages.LOGGED_OUT_SUCCESS);
//        } catch (Exception e) {
//            log.error("An unexpected error occurred while login : \n"+ExceptionUtils.getStackTrace(e)+"\n"+"Logged User Id:\n "+user.getId());
//            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
//        }
//
//    }

    private String getClientIp(HttpServletRequest request, AuthRequest authRequest) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
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
