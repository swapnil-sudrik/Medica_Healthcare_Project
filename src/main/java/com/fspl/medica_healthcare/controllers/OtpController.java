package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.exceptions.RecordNotFoundException;
import com.fspl.medica_healthcare.models.Otp;
import com.fspl.medica_healthcare.models.OtpRequest;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.EmailService;
import com.fspl.medica_healthcare.services.OtpService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.templets.EmailTemplets;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;


    private static final Logger log = Logger.getLogger(OtpController.class);

    @PostMapping("/generate/{email}")
    public ResponseEntity<?> generateOtp(@PathVariable String email){
            try {
                if (email.isBlank()){
                    return ResponseEntity.badRequest().body("Email is required.");
                }

                if (!email.matches("^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$")){
                    return ResponseEntity.badRequest().body("Please enter a valid email");
                }

                Otp preOtp =  otpService.getOtpByEmail(email);
                if (preOtp !=null){
                    boolean isDeleted =  otpService.delete(preOtp);
                    if (!isDeleted){
                        return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                    }
                }
                Otp otp = new Otp();
                Random random = new Random();
                String generatedOtp = "";
                for (int i=1; i<=6;i++){
                    generatedOtp+=(random.nextInt(9));
                }

                otp.setEmail(email);
                otp.setOtp(generatedOtp);
                otp.setCreatedDateTime(LocalDateTime.now());
                boolean isOtpSaved = otpService.generateOtp(otp);
                if (isOtpSaved){
                    Otp newOtp =  otpService.getOtpByEmail(email);
                    emailService.sendEmail(otp.getEmail(),"OTP verification",getOtpVerificationTemplate(otp.getOtp()));
                    return ResponseEntity.ok(newOtp);
                }else {
                    return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
                }

            } catch (Exception e) {
                log.error("An unexpected error occurred during OTP creating for email: \n"+ ExceptionUtils.getStackTrace(e)+"\n"+"For email :\n "+email);
                return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
            }
    }

    @PostMapping("/verify/{email}")
    public ResponseEntity<String> verifyOtp(@PathVariable String email, @RequestBody OtpRequest otpRequest){
        try {
            if (email.isBlank()){
                throw new RecordNotFoundException("Email is required.");
            }
            if (!email.matches("^[a-zA-Z][a-zA-Z0-9_.-]*@[a-zA-Z]+\\.[a-zA-Z]{2,}$")){
                return ResponseEntity.badRequest().body("Please enter a valid email");
            }
            Otp otp = otpService.getOtpByEmail(email);

            if (otp==null){
                return ResponseEntity.badRequest().body("Invalid OTP. Enter Correct OTP");
            }
            if (!otp.getOtp().equals(otpRequest.getOtp())){
                return ResponseEntity.badRequest().body("OTP not match. Enter Correct OTP");
            }
            return ResponseEntity.ok("OTP Successfully Verified!");
        } catch (Exception e) {
            log.error("An unexpected error occurred while OTP verify: \n"+ExceptionUtils.getStackTrace(e)+"\n"+"For email :\n "+email);
            return ResponseEntity.badRequest().body(ExceptionMessages.SERVER_DOWN);
        }
    }


    public String getOtpVerificationTemplate(String otpCode) {
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
                "    text-align: center;" +
                "}" +
                ".otp {" +
                "    font-size: 24px;" +
                "    font-weight: bold;" +
                "    color: #0d47a1;" +
                "    background-color: #e3f2fd;" +
                "    padding: 10px;" +
                "    display: inline-block;" +
                "    border-radius: 5px;" +
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
                "OTP Verification - verify your OTP "+
                "</div>" +
                "<div class=\"body\">" +
                "<p>Dear User,</p>" +
                "<p>Use the following OTP to verify your identity:</p>" +
                "<p class=\"otp\">" + otpCode + "</p>" +
                "<p>This OTP is valid for a limited time. Do not share it with anyone.</p>" +
                "<p>If you did not request this OTP, please ignore this email or contact support.</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 Medica Healthcare. All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }



}
