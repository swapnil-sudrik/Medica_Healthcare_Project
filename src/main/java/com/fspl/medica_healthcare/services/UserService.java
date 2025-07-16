package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.HospitalRepository;
import com.fspl.medica_healthcare.repositories.UserRepository;
import com.fspl.medica_healthcare.templets.EmailTemplets;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailTemplets emailTemplets;

    @Autowired
    private HospitalRepository hospitalRepository;

//    Q- why use static keyword
//   1:- Single Logger Instance Per Class
//   - The logger is shared across all instances of UserService, reducing memory usage.
//   - You don’t need to create a new logger for every object of UserService.

//   2:- Faster and More Efficient Logging
//   - Since it’s static, the logger is initialized only once when the class is loaded.
//   - It avoids unnecessary object creation.

    private static final Logger log = Logger.getLogger(UserService.class);

    public List<User> getAllUsersByHospital(User loginUser) {
        try {
            List<User> userList = userRepository.findAllUsersByHospitalId(loginUser.getHospital().getId());
            if (userList.isEmpty()){
                return null;
            }
            return userList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllUsersByHospitalId(): \n"+ ExceptionUtils.getStackTrace(e) +"\n"+"Logged User Id:\n "+loginUser.getId());

            return null;
        }


    }

    public User getUserById(long id , User loginUser) {
        try {
            Optional<User> user = userRepository.findById(id);
            return user.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getUserById(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User Id:\n "+loginUser.getId());
            return null;
        }
    }

    public User findByUsername(String username) {

        try {
            Optional<User> user = userRepository.findByUsername(username);
            return user.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while user findByUsername(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Username : \n "+username);
            return null;
        }
    }

    public boolean saveUser(User user , User loginUser) {
        try {
           userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while saveUser(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User Id:\n "+loginUser.getId());
            return false;
        }
    }

    public List<User> getAllActiveUsersOfHospital(User loginUser) {
        try {
            List<User> userList = userRepository.findAllActiveUsersByHospitalId(loginUser.getHospital().getId());
            if (userList.isEmpty()){
                return null;
            }
            return userList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllActiveUsersOfHospital(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User Id:\n "+loginUser.getId());            return null;
        }
    }

    public User getAuthenticateUser() {
        Optional<User> user= Optional.empty();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            user = userRepository.findByUsername(authentication.getName());
            return user.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAuthenticateUser(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User Id:\n "+user.get().getId());
            return null;
        }

        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // return userRepository.findByUsername(authentication.getName()).orElseThrow(()->new UserNotFoundException("Token Not Found OR Authenticated User Not Found"));
    }

    public User userDtoToUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        user.setName(userDTO.getName());
        if (userDTO.getRoles().equals("ADMIN")) {
            user.setBranch(null);
        } else {
            user.setBranch(userDTO.getBranch().getBytes());
        }
        user.setRoles(userDTO.getRoles().toUpperCase());
        return user;
    }
}

