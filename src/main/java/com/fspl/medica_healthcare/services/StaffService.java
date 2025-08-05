package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.dtos.RoleSalaryAverageDTO;
import com.fspl.medica_healthcare.dtos.UserDTO;
import com.fspl.medica_healthcare.models.Staff;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.StaffRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;

    private static final Logger log = Logger.getLogger(StaffService.class);

    public boolean saveStaff(Staff staff , User loginUser) {
        try {
         staffRepository.save(staff);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while saveStaff(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return false;
        }
    }
    //ORIGINAL API
    /*
    public Staff findById(long id, User loginUser) {
        try {
            Optional<Staff> staff = staffRepository.findById(id);
            return staff.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findById(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }
     */

    //Add new branchwise logic...
    public Staff findById(Long id, User loginUser) {
        Optional<Staff> optionalStaff = staffRepository.findById(id);

        if (optionalStaff.isEmpty()) {
            return null;
        }

        Staff staff = optionalStaff.get();

        // Null check
        if (staff.getHospital() == null || staff.getBranch() == null || loginUser.getHospital() == null) {
            return null;
        }

        //  Use == for primitive long comparison
        boolean isSameHospital = loginUser.getHospital().getId() == staff.getHospital().getId();

        // Get staff branch
        String staffBranch = new String(staff.getBranch());

        // Get logged-in admin's branches
        List<String> adminBranches = Arrays.stream(new String(loginUser.getBranch()).split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        // Check if branch allowed
        boolean isBranchAllowed = adminBranches.contains(staffBranch);

        //  Admin or Master Admin
        if (isSameHospital && isBranchAllowed) {
            return staff;
        }

        //  Master Admin can access any branch
        if (isSameHospital && adminBranches.size() > 1) {
            return staff;
        }

        return null; // access denied
    }

    //

    public Staff findByEmail(String email , User loginUser) {
        try {
            Optional<Staff> staff = staffRepository.findByEmail(email);
//           if (optionalStaff.isEmpty()){
//               return null;
//           }
//           return optionalStaff.get();
            return staff.orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while saveStaff(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);

            return null;
        }

    }

    public List<Staff> getAllStaffByHospitalId(User loginUser) {
        try {
            List<Staff> staffList = staffRepository.findAllUsersByHospitalId(loginUser.getHospital().getId());
            if (staffList.isEmpty()){
                return null;
            }
            return staffList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllStaffByHospitalId(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }

    public List<Staff> getAllActiveStaffByHospitalId(User loginUser) {
        try {
            List<Staff> staffList = staffRepository.findAllActiveStaffByHospitalId(loginUser.getHospital().getId());
            if (staffList.isEmpty()){
                return null;
            }
            return staffList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllActiveStaffByHospitalId(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }

    //Add branch wise logic...
    public List<Staff> getAllActiveStaffByHospitalAndBranch(Long hospitalId, byte[] branch) {
        return staffRepository.findByHospitalIdAndBranchAndStatus(hospitalId, branch, 1);
    }


    //Add branch wise logic
    public List<Staff> getAllStaffByHospitalAndBranch(Long hospitalId, byte[] branch) {
        return staffRepository.findAllByHospitalIdAndBranch(hospitalId, branch);
    }

    public List<RoleSalaryAverageDTO> getAverageSalaryByRole(User loginUser) {
        try {
            List<RoleSalaryAverageDTO> roleSalaryAverageDTOList =staffRepository.findAverageSalaryByRole(loginUser.getHospital().getId());

            if (roleSalaryAverageDTOList.isEmpty()){
                return null;
            }
            return roleSalaryAverageDTOList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAverageSalaryByRole(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }

    public List<RoleSalaryAverageDTO> findAverageSalaryByRoleAndDate(User loginUser, int month, int year) {
        try {
            List<RoleSalaryAverageDTO> roleSalaryAverageDTOList = staffRepository.findAverageSalaryByRoleAndDate(loginUser.getHospital().getId(), month, year);
           if (roleSalaryAverageDTOList.isEmpty()){
               return null;
           }
            return roleSalaryAverageDTOList;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findAverageSalaryByRoleAndDate(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }

    public Double getTotalSalaryExpenses(User loginUser) {
        try {
            return staffRepository.getTotalSalaryExpensesByHospitalId(loginUser.getHospital().getId());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getTotalSalaryExpensesByHospitalId(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }

    public Double getTotalSalaryExpensesByHospitalIdAndDate(User loginUser, int month, int year) {
        try {
            return staffRepository.getTotalSalaryExpensesByHospitalIdAndDate(loginUser.getHospital().getId(), month, year);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getTotalSalaryExpensesByHospitalIdAndDate(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }

    public Long getCountOfStaff(User loginUser) {
        try {
            return staffRepository.getNumberOfStaffByHospitalId(loginUser.getHospital().getId());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getCountOfStaff(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);

            return null;
        }
    }

    public Long getNumberOfStaffByHospitalIdAndDate(User loginUser, int month, int year) {
        try {
            return staffRepository.getNumberOfStaffByHospitalIdAndDate(loginUser.getHospital().getId(), month, year);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getNumberOfStaffByHospitalIdAndDate(): \n"+e.getMessage() +"\n"+"Logged User :\n "+loginUser);
            return null;
        }
    }

    public Staff userDtoTOStaff(UserDTO userDTO) {
        Staff staff = new Staff();
        staff.setEmail(userDTO.getUsername());
        staff.setName(userDTO.getName());
        staff.setAddress(userDTO.getAddress().getBytes());
        staff.setCareTaker(userDTO.isCareTaker());
        staff.setSpecalization(userDTO.getSpecalization());
        staff.setBookingStatus(userDTO.getBookingStatus());
        staff.setBookingCharge(userDTO.getBookingCharge());
        if (userDTO.getRoles().equals("ADMIN")) {
            staff.setBranch(null);
        } else {
            staff.setBranch(userDTO.getBranch().getBytes());
        }
        staff.setRoles(userDTO.getRoles().toUpperCase());
        staff.setDoctorFee(userDTO.getDoctorFee());
//        if (userDTO.getSalary() == null) {
//            staff.setSalary(BigDecimal.ZERO);
//        } else {
//            staff.setSalary(userDTO.getSalary());
//        }
        if (userDTO.getSalary() == 0.0) {
            staff.setSalary(0.0);
        } else {
            staff.setSalary(userDTO.getSalary());
        }
        return staff;
    }



}
