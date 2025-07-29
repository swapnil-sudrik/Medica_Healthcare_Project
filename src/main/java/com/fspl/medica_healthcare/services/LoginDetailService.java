package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.LoginDetails;
import com.fspl.medica_healthcare.repositories.LoginDetailsRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginDetailService {

    @Autowired
    private LoginDetailsRepository loginDetailsRepository;

    private static final Logger log = Logger.getLogger(LoginDetailService.class);

    public boolean saveLoginDetails(LoginDetails loginDetails){
        try {
            loginDetailsRepository.save(loginDetails);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while saveLoginDetails() : "+ e);
            return false;
        }
    }

    public LoginDetails findByUsername(String username){
        try {
            Optional<LoginDetails> loginDetails = loginDetailsRepository.findByUsername(username);
            return loginDetails.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while findByUsername() : "+ e);

            return null;
        }
    }

    public void delete(LoginDetails loginDetails){
            loginDetailsRepository.delete(loginDetails);
    }
}
