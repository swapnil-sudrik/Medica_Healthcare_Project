package com.fspl.medica_healthcare.config;

// Application Runner for Default Data

import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.HospitalRepository;
import com.fspl.medica_healthcare.repositories.UserRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@org.springframework.context.annotation.Configuration
public class DefaultDataInitializer {

	   @Autowired
	    private PasswordEncoder encoder;

    private static final Logger log = Logger.getLogger(DefaultDataInitializer.class);

    @Bean
    public ApplicationRunner initializer(UserRepository userRepository, HospitalRepository hospitalRepository) {
        return args -> {
            String superUsername = "swapnilsudrik.s@gmail.com";
            String superPassword = "superadmin123";
            String superRole = "SUPER_ADMIN";

            userRepository.findByUsername(superUsername).ifPresentOrElse(admin -> {
                System.out.println("SUPER ADMIN ALREADY EXIST.");
                System.out.println("USERNAME: " + superUsername);
                System.out.println("PASSWORD: " + superPassword);
            }, () -> {
                try {
                    System.err.println("SUPER ADMIN NOT FOUND\n");
                    System.out.println("CREATING SUPER ADMIN FOR YOUR APPLICATION...");
                    User user = new User();
                    user.setUsername(superUsername);
                    user.setPassword(encoder.encode(superPassword));
                    user.setRoles(superRole);
                    user.setHospital(null);
                    user.setCreatedUser(user);
                    user.setModifiedUser(user);
                    user.setCreatedDate(LocalDate.now());
                    user.setModifiedDate(LocalDate.now());
                    user.setStatus(1);
                    user.setName("swapnil");
                    userRepository.save(user);


                    System.out.println("SUPER ADMIN CREATED SUCCESS..");
                    System.out.println("USERNAME: " + superUsername);
                    System.out.println("PASSWORD: " + superPassword);
                } catch (Exception e) {
                    log.error("An unexpected error occurred while creating a super admin :" + ExceptionUtils.getStackTrace(e));
                    return;
                }

            });
        };
    }
}
