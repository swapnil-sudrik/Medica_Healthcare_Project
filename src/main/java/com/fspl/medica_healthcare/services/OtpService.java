package com.fspl.medica_healthcare.services;


import com.fspl.medica_healthcare.models.Otp;
import com.fspl.medica_healthcare.repositories.OtpRepository;
import com.fspl.medica_healthcare.templets.EmailTemplets;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OtpService {
    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailTemplets emailTemplets;

    private static final Logger log = Logger.getLogger(OtpService.class);

    public boolean generateOtp(Otp otp){
        try {
            otpRepository.save(otp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while generateOtp() : "+ e);
            return false;
        }
    }

    public Otp getOtpByEmail(String email){
        try {
            Optional<Otp> otp = otpRepository.findByEmail(email);
            return otp.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while getOtpByEmail() : "+ e);
            return null;
        }
    }

    public boolean delete(Otp otp){
        try {
          otpRepository.delete(otp);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while deleteOtp() : "+ e);
            return false;
        }
    }

    public void deleteExpiredOtp(LocalDateTime time){
        try{
            otpRepository.deleteByCreatedDateTimeBefore(time);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while deleteExpiredOtp() : "+ e);
        }
    }
}
