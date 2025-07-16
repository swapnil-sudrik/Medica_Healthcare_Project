package com.fspl.medica_healthcare.controllers;


import com.fspl.medica_healthcare.dtos.HospitalResponseDTO;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.HospitalService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hospital")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private UserService userService;

    private static final Logger log = Logger.getLogger(HospitalController.class);

    @PostMapping("/addHospital")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> createHospital(@ModelAttribute @Valid HospitalResponseDTO hospitalDto,
                                                         @RequestParam("letterHead") MultipartFile letterHead
    ) {
        User loginUser = null;

        try {
            loginUser = userService.getAuthenticateUser();

//            if (hospitalService.existsByName(hospitalDto.getName())) {
//                return new ResponseEntity<>("Hospital already exits for this name", HttpStatus.BAD_REQUEST);
//            }

            if (hospitalDto.getAddress() == null || hospitalDto.getAddress().trim().isEmpty()) {
                return new ResponseEntity<>("Hospital address is required", HttpStatus.BAD_REQUEST);
            }
            if (hospitalService.existsByEmailId(hospitalDto.getEmailId())) {
                return new ResponseEntity<>("Hospital already exits for this email id", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getName() == null || !hospitalDto.getName().matches("^[A-Za-z ]+$")) {
                return new ResponseEntity<>("Hospital name must contain only letters", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getContactNumber() == null || hospitalDto.getContactNumber().matches("0000000000") || !hospitalDto.getContactNumber().matches("^\\d{10}$")) {
                return new ResponseEntity<>("Contact number must be exactly 10 digits or please put the correct number", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getEmailId() == null || !hospitalDto.getEmailId().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getOpeningTime() == null || hospitalDto.getClosingTime() == null) {
                return new ResponseEntity<>("Opening and closing times are required", HttpStatus.BAD_REQUEST);
            }
            if (!hospitalDto.getOpeningTime().matches("^(0[1-9]|1[0-2]):[0-5][0-9] [APap][Mm]$")) {
                return new ResponseEntity<>("Please enter opening time in hh:mm AM/PM format", HttpStatus.BAD_REQUEST);
            }
            if (!hospitalDto.getClosingTime().matches("^(0[1-9]|1[0-2]):[0-5][0-9] [APap][Mm]$")) {
                return new ResponseEntity<>("Please enter closing time in hh:mm AM/PM format", HttpStatus.BAD_REQUEST);
            }
            if (letterHead != null && !letterHead.isEmpty()) {
                if (!Objects.requireNonNull(letterHead.getContentType()).startsWith("image/")) {
                    return new ResponseEntity<>("Invalid file type. Only image files are allowed", HttpStatus.BAD_REQUEST);
                }
                if (letterHead.getSize() > 5 * 1024 * 1024) { // 5MB limit
                    return new ResponseEntity<>("File size must be less than 5MB", HttpStatus.BAD_REQUEST);
                }
            }
            if (hospitalDto.getBranch() == null || !hospitalDto.getBranch().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Branch must contain only letters and commas", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getWorkingDays() == null || !hospitalDto.getWorkingDays().matches("^[a-zA-Z\\- ]*$")) {
                return new ResponseEntity<>("Hospital working days must contain only letters and - characters like Monday - Friday", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getOffDays() == null || !hospitalDto.getOffDays().matches("^[A-Za-z,]+$")) {
                return new ResponseEntity<>("Hospital off days must contain only letters", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getNumberOfUsers() == null || !hospitalDto.getNumberOfUsers().matches("^\\d+$")) {
                return new ResponseEntity<>("Number of users must be a numeric value", HttpStatus.BAD_REQUEST);
            }
            if (hospitalDto.getDepartments() == null || !hospitalDto.getDepartments().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Department must contain only letters and commas", HttpStatus.BAD_REQUEST);
            }

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

            Hospital hospital = new Hospital();

            hospital.setName(hospitalDto.getName().trim().replaceAll("\\s+", " "));
            hospital.setAddress(hospitalDto.getAddress().getBytes());
            hospital.setContactNumber(hospitalDto.getContactNumber());
            hospital.setEmailId(hospitalDto.getEmailId());
            hospital.setOpeningTime(LocalTime.parse(hospitalDto.getOpeningTime(), timeFormatter));
            hospital.setClosingTime(LocalTime.parse(hospitalDto.getClosingTime(), timeFormatter));
            hospital.setWorkingDays(hospitalDto.getWorkingDays());
            hospital.setOffDays(hospitalDto.getOffDays());
            hospital.setNumberOfUsers(hospitalDto.getNumberOfUsers());
            hospital.setLetterHead(letterHead.getBytes());
            hospital.setBranch(hospitalDto.getBranch().getBytes());
            hospital.setDepartments(hospitalDto.getDepartments().getBytes());
            hospital.setStatus(hospitalDto.getStatus());
            hospital.setCreatedUser(loginUser);
            hospital.setModifiedUser(loginUser);
            hospital.setCreatedDate(LocalDate.now());
            hospital.setModifiedDate(LocalDate.now());
            hospital.setStatus(1);
            Hospital savedHospital = hospitalService.saveHospital(hospital);

            return new ResponseEntity<>(savedHospital, HttpStatus.OK);
        } catch (Exception e) {
            log.error("An unexpected error while createHospital" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("An unexpected error occurred: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }
    //------------------------------------  Get All Hospital -----------------------------------------

    @GetMapping("/getAllHospital")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> getAllHospitals() {
        User loginUser = null;

        try {
            loginUser = userService.getAuthenticateUser();

            List<Hospital> hospitals = hospitalService.getAllHospitals();
            List<HospitalResponseDTO> hospitalDTOs = hospitals.stream().map(hs -> {
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                hsDto.setId((hs.getId()));
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setOpeningTime(String.valueOf(hs.getOpeningTime()));
                hsDto.setClosingTime(String.valueOf(hs.getClosingTime()));
                hsDto.setLetterhead(hs.getLetterHead());
                hsDto.setWorkingDays(hs.getWorkingDays());
                hsDto.setOffDays(hs.getOffDays());
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());
                return hsDto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(hospitalDTOs);

        } catch (Exception e) {
            log.error("An Unexpected error while getAllHospitals" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("An unexpected error occurred:" + e.getMessage(), HttpStatus.BAD_REQUEST);

        }

    }
    //------------------------------------ Get Hospital by id -----------------------------------------

    @GetMapping("/getHospitalById/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> getHospitalById(@PathVariable long id) {

        User loginUser = null;

        try {
            loginUser = userService.getAuthenticateUser();
            Hospital hospital = hospitalService.getHospitalById(id);

            if (hospital != null) {
                Hospital hs = hospital;
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                hsDto.setId(hs.getId());
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setOpeningTime(String.valueOf(hs.getOpeningTime()));
                hsDto.setClosingTime(String.valueOf(hs.getClosingTime()));
                hsDto.setLetterhead(hs.getLetterHead());
                hsDto.setWorkingDays(hs.getWorkingDays());
                hsDto.setOffDays(hs.getOffDays());
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());

                return ResponseEntity.ok(hsDto);

            }
        } catch (Exception e) {
            log.error("An unexpected error occurred while getHospitalById" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("An unexpected error occurred:" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.status(404).body("error : Hospital with ID " + id + "not found");
    }

    //------------------------------------  Get by Hospital name -----------------------------------------

    @GetMapping("/getHospitalByName/{name}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> getHospitalByName(@PathVariable String name) {

        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            List<Hospital> hospitals = hospitalService.findHospitalByName(name);
            if (hospitals.isEmpty()) {
                return new ResponseEntity<>("Hospital not found with name: " + name, HttpStatus.BAD_REQUEST);
            }
            List<HospitalResponseDTO> hospitalDTOs = hospitals.stream().map(hs -> {
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                hsDto.setId(hs.getId());
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setOpeningTime(String.valueOf(hs.getOpeningTime()));
                hsDto.setClosingTime(String.valueOf(hs.getClosingTime()));
                hsDto.setLetterhead(hs.getLetterHead());
                hsDto.setWorkingDays(hs.getWorkingDays());
                hsDto.setOffDays(hs.getOffDays());
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());

                return hsDto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(hospitalDTOs);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getHospitalByName" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("An unexpected error occurred:" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    //------------------------------------------Update Hospital------------------------------------------

    @PutMapping(value = "/updateHospital/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> updateHospital(
            @PathVariable long id,
            @RequestParam(value = "letterhead", required = false) MultipartFile letterhead,
            @Valid @ModelAttribute HospitalResponseDTO hospitalResponseDTO,
            BindingResult bindingResult) {

        User loginUser = null;

        try {

            loginUser = userService.getAuthenticateUser();
            Hospital hs = hospitalService.getHospitalById(id);
            if (hs == null) {
                return new ResponseEntity<>("Hospital not found with id " + id, HttpStatus.BAD_REQUEST);
            }

            if (hospitalResponseDTO.getOpeningTime() == null || hospitalResponseDTO.getClosingTime() == null) {
                return new ResponseEntity<>("Opening and closing times are required", HttpStatus.BAD_REQUEST);
            }
            if (!hospitalResponseDTO.getOpeningTime().matches("^(0[1-9]|1[0-2]):[0-5][0-9] [APap][Mm]$")) {
                return new ResponseEntity<>("Please enter opening time in hh:mm AM/PM format", HttpStatus.BAD_REQUEST);
            }
            if (!hospitalResponseDTO.getClosingTime().matches("^(0[1-9]|1[0-2]):[0-5][0-9] [APap][Mm]$")) {
                return new ResponseEntity<>("Please enter closing time in hh:mm AM/PM format", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getName() == null || !hospitalResponseDTO.getName().matches("^[A-Za-z ]+$")) {
                return new ResponseEntity<>("Hospital name must contain only letters", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getAddress() == null || hospitalResponseDTO.getAddress().trim().isEmpty()) {
                return new ResponseEntity<>("Hospital address is required", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getContactNumber() == null || hospitalResponseDTO.getContactNumber().matches("0000000000") || !hospitalResponseDTO.getContactNumber().matches("^\\d{10}$")) {
                return new ResponseEntity<>("Contact number must be exactly 10 digits or please put the correct number", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getEmailId() == null || !hospitalResponseDTO.getEmailId().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getWorkingDays() == null || !hospitalResponseDTO.getWorkingDays().matches("^[a-zA-Z\\- ]*$")) {
                return new ResponseEntity<>("Hospital working days must contain only letters and - characters like Monday - Friday", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getOffDays() == null || !hospitalResponseDTO.getOffDays().matches("^[A-Za-z,]+$")) {
                return new ResponseEntity<>("Hospital off days must contain only letters", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getNumberOfUsers() == null || !hospitalResponseDTO.getNumberOfUsers().toString().matches("^\\d+$")) {
                return new ResponseEntity<>("Number of users must be a numeric value", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getBranch() == null || !hospitalResponseDTO.getBranch().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Branch must contain only letters and commas", HttpStatus.BAD_REQUEST);
            }
            if (hospitalResponseDTO.getDepartments() == null || !hospitalResponseDTO.getDepartments().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Department must contain only letters and commas", HttpStatus.BAD_REQUEST);
            }
            if (letterhead != null && !letterhead.isEmpty()) {
                if (!Objects.requireNonNull(letterhead.getContentType()).startsWith("image/")) {
                    return new ResponseEntity<>("Invalid file type. Only image files are allowed", HttpStatus.BAD_REQUEST);
                }
                if (letterhead.getSize() > 5 * 1024 * 1024) { // 5MB limit
                    return new ResponseEntity<>("File size must be less than 5MB", HttpStatus.BAD_REQUEST);
                }
            }

            Hospital hospital = hs;

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
            hospital.setOpeningTime(LocalTime.parse(hospitalResponseDTO.getOpeningTime().trim(), dateTimeFormatter));
            hospital.setClosingTime(LocalTime.parse(hospitalResponseDTO.getClosingTime().trim(), dateTimeFormatter));
            hospital.setName(hospitalResponseDTO.getName().trim().replaceAll("\\s+", " "));
            hospital.setAddress(hospitalResponseDTO.getAddress().getBytes());
            hospital.setContactNumber(hospitalResponseDTO.getContactNumber());
            hospital.setEmailId(hospitalResponseDTO.getEmailId());
            hospital.setWorkingDays(hospitalResponseDTO.getWorkingDays().trim().replaceAll("\\s+", " "));
            hospital.setOffDays(hospitalResponseDTO.getOffDays().trim().replaceAll("\\s+", " "));
            hospital.setNumberOfUsers(hospitalResponseDTO.getNumberOfUsers());
            hospital.setBranch(hospitalResponseDTO.getBranch().trim().getBytes());
            hospital.setDepartments(hospitalResponseDTO.getDepartments().trim().getBytes());
            hospital.setLetterHead(letterhead.getBytes());

            Hospital updatedHospital = hospitalService.saveHospital(hospital);
            return new ResponseEntity<>(updatedHospital, HttpStatus.OK);

        } catch (Exception e) {
            log.error("An unexpected error occurred while updateHospital" + ExceptionUtils.getStackTrace(e) + "Logged User: " + loginUser.getId());
            return new ResponseEntity<>("An unexpected error occurred:" + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


    //------------------------------------Delete or Deactivate Hospital -----------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> deleteHospital(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            Hospital hospital = hospitalService.getHospitalById(id);
            if (hospital == null) {
                return new ResponseEntity<>("Hospital not found.", HttpStatus.BAD_REQUEST);
            }

            if (hospital.getStatus() == 0) {
                return new ResponseEntity<>("Hospital is already deactivate.", HttpStatus.BAD_REQUEST);
            }
            hospital.setStatus(0);
            hospital.setModifiedDate(LocalDate.now());
            hospitalService.saveHospital(hospital);
            return new ResponseEntity<>("Hospital with ID " + id + " has been deactivated successfully.", HttpStatus.OK);
        } catch (Exception e) {
            log.error("An unexpected error occurred while deleteHospital" + ExceptionUtils.getStackTrace(e) + "Lgged User:" + loginUser.getId());
            return new ResponseEntity<>("Error deleting hospital: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    //------------------------------------  Reactive Hospital -----------------------------------------

    @PutMapping("/reactivateHospital/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<String> reactivateHospital(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            Hospital hospital = hospitalService.getHospitalById(id);

            if (hospital == null) {
                return new ResponseEntity<>("Hospital not found", HttpStatus.BAD_REQUEST);
            }
            if (hospital.getStatus() == 1) {
                return new ResponseEntity<>("Hospital is already active.", HttpStatus.BAD_REQUEST);
            }
            hospital.setStatus(1);
            hospital.setModifiedDate(LocalDate.now());
            hospitalService.saveHospital(hospital);

            return new ResponseEntity<>("Hospital with ID " + id + " has been reactivated successfully.", HttpStatus.OK);
        } catch (Exception e) {
            log.error("An unexpected error occurred while reactivateHospital" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return new ResponseEntity<>("Error while reactive the hospital: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    //------------------------------------ Get all Active Hospital -----------------------------------------

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/allActiveHospitals/{status}")
    public synchronized ResponseEntity<?> getAllActiveHospitals(@PathVariable int status) {

        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            List<Hospital> hospital = hospitalService.findActiveHospitals(status);
            //List<Hospital> hospitals = hospitalService.getAllHospitals();
            List<HospitalResponseDTO> hospitalDTOs = hospital.stream().map(hs -> {
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                hsDto.setId((hs.getId()));
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setOpeningTime(String.valueOf(hs.getOpeningTime()));
                hsDto.setClosingTime(String.valueOf(hs.getClosingTime()));
                hsDto.setLetterhead(hs.getLetterHead());
                hsDto.setWorkingDays(hs.getWorkingDays());
                hsDto.setOffDays(hs.getOffDays());
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());
                return hsDto;
            }).collect(Collectors.toList());

            if (hospital.isEmpty()) {
                return new ResponseEntity<>("Active hospitals are not available", HttpStatus.BAD_REQUEST);
            } else if (status != 1) {
                return new ResponseEntity<>("Invalid Status.Please Enter 1 for correct output value. .", HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok(hospitalDTOs);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllActiveHospitals" + ExceptionUtils.getStackTrace(e) + "Logged User : " + loginUser.getId());
            return new ResponseEntity<>("Error getting active hospital: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    //------------------------------------  Get all deactive Hospital -----------------------------------------

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/allDeactivatedHospitals/{status}")
    public synchronized ResponseEntity<?> getAllDeactivatedHospitals(@PathVariable int status) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            List<Hospital> hospital = hospitalService.findDeactivatedHospitals(status);
            List<HospitalResponseDTO> hospitalDTOs = hospital.stream().map(hs -> {
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                hsDto.setId((hs.getId()));
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setOpeningTime(String.valueOf(hs.getOpeningTime()));
                hsDto.setClosingTime(String.valueOf(hs.getClosingTime()));
                hsDto.setLetterhead(hs.getLetterHead());
                hsDto.setWorkingDays(hs.getWorkingDays());
                hsDto.setOffDays(hs.getOffDays());
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());
                return hsDto;
            }).collect(Collectors.toList());

            if (hospital.isEmpty()) {
                return new ResponseEntity<>("Deactivated hospitals are not available", HttpStatus.BAD_REQUEST);
            } else if (status != 0) {
                return new ResponseEntity<>("Invalid Status.Please Enter 0 for correct output value. .", HttpStatus.BAD_REQUEST);
            }
            return ResponseEntity.ok(hospitalDTOs);

        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllDeactivatedHospitals" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return new ResponseEntity<>("Error getting deactive hospital: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}