package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.HospitalRepository;
import com.fspl.medica_healthcare.repositories.UserRepository;
import com.fspl.medica_healthcare.templets.EmailTemplets;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.transaction.Transactional;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    public List<User> getAllUsersByHospital(User loginUser, byte[] branch) {
        try {
            List<User> userList = userRepository.findAllUsersByHospitalId(loginUser.getHospital().getId(), branch);
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

    public boolean saveUserByOAuth(User user) {
        try {
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while saveUser(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"User email:\n "+user.getUsername());
            return false;
        }
    }

//    public List<User> getAllActiveUsersOfHospital(User loginUser) {
//        try {
//            List<User> userList = userRepository.findAllActiveUsersByHospitalId(loginUser.getHospital().getId());
//            if (userList.isEmpty()){
//                return null;
//            }
//            return userList;
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("An unexpected error occurred while getAllActiveUsersOfHospital(): \n"+ExceptionUtils.getStackTrace(e) +"\n"+"Logged User Id:\n "+loginUser.getId());            return null;
//        }
//    }

    //add new service layer..D
    public List<User> getAllActiveUsersByHospitalAndBranch(User loginUser, byte[] branch) {
        return userRepository.findByHospitalIdAndBranchAndStatus(
                loginUser.getHospital().getId(), branch, 1
        );
    }



    //
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

    //new method..
    public List<User> getAllUsers() {
        try {
            List<User> users = userRepository.findAll(); // Fetches all users
            return users.isEmpty() ? Collections.emptyList() : users; // Never returns null
        } catch (Exception e) {
            log.error("Error in getAllUsers(): \n" + ExceptionUtils.getStackTrace(e));
            return Collections.emptyList(); // Fail-safe: returns empty list instead of null
        }
    }

    public User userDtoToUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setName(userDTO.getName());
        user.setContactNumber(userDTO.getContactNumber());
        user.setDateOfBirth(userDTO.getDateOfBirth());
        if (userDTO.getRoles().equals("ADMIN")) {
            user.setBranch(null);
        } else {
            user.setBranch(userDTO.getBranch().getBytes());
        }
        user.setRoles(userDTO.getRoles().toUpperCase());
        return user;
    }
}

