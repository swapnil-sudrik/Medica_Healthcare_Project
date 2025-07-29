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

import java.time.LocalDate;
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

    //This method is used for add hospitals.
    @PostMapping("/addHospital")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> createHospital(@RequestBody @Valid HospitalResponseDTO hospitalDto
    ) {
        User loginUser = null;

        Hospital savedHospital = null;
        try {
            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            // Check if the address is provided and not empty
            if (hospitalDto.getAddress() == null) {
                return new ResponseEntity<>("Hospital address cannot be null", HttpStatus.BAD_REQUEST);
            } else if (hospitalDto.getAddress().trim().isEmpty()) {
                return new ResponseEntity<>("Hospital address is required", HttpStatus.BAD_REQUEST);
            }

            // Validate hospital name (only letters and spaces allowed)
            if (hospitalDto.getName() == null) {
                return new ResponseEntity<>("Hospital name cannot be null", HttpStatus.BAD_REQUEST);
            } else if ( !hospitalDto.getName().matches("^[A-Za-z ]+$")) {
                return new ResponseEntity<>("Hospital name must contain only letters", HttpStatus.BAD_REQUEST);
            }

            // Validate contact number (must be exactly 10 digits, no leading zeros)
            if (hospitalDto.getContactNumber() == null) {
                return new ResponseEntity<>("Contact number cannot be null", HttpStatus.BAD_REQUEST);
            } else if (hospitalDto.getContactNumber().matches("0000000000") || !hospitalDto.getContactNumber().matches("^\\d{10}$")) {
                return new ResponseEntity<>("Contact number must be exactly 10 digits or please put the correct number", HttpStatus.BAD_REQUEST);
            }

            // Validate email format (using regex)
            if (hospitalDto.getEmailId() == null){
                return new ResponseEntity<>("Email Id cannot be null", HttpStatus.BAD_REQUEST);
            }
            else if (!hospitalDto.getEmailId().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);
            }else if (hospitalService.existsByEmailId(hospitalDto.getEmailId())) {
                return new ResponseEntity<>("Hospital email-ID already exists.", HttpStatus.BAD_REQUEST);
            }

            // Validate the branch (only letters and commas allowed)
            if (hospitalDto.getBranch() == null) {
                return new ResponseEntity<>("Branch cannot be null", HttpStatus.BAD_REQUEST);
            } else if (!hospitalDto.getBranch().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Branch must contain only letters and commas",HttpStatus.BAD_REQUEST);
            }

            // Validate the number of users (must be numeric)
            if (hospitalDto.getNumberOfUsers() == null) {
                return new ResponseEntity<>("Numbers of users cannot be null",HttpStatus.BAD_REQUEST);
            } else if (!hospitalDto.getNumberOfUsers().matches("^\\d+$")) {
                return new ResponseEntity<>("Number of users must be a numeric value", HttpStatus.BAD_REQUEST);
            }

            // Validate the departments (only letters and commas allowed)
            if (hospitalDto.getDepartments() == null) {
                return new ResponseEntity<>("Department must contain only letters and commas", HttpStatus.BAD_REQUEST);
            } else if (!hospitalDto.getDepartments().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Department must contain only letters and commas", HttpStatus.BAD_REQUEST);
            }

            // Create a new hospital object
            Hospital hospital = new Hospital();

            // Setting hospital data from the DTO
            hospital.setName(hospitalDto.getName().trim().replaceAll("\\s+", " "));
            hospital.setAddress(hospitalDto.getAddress().getBytes());
            hospital.setContactNumber(hospitalDto.getContactNumber());
            hospital.setEmailId(hospitalDto.getEmailId());
            hospital.setNumberOfUsers(hospitalDto.getNumberOfUsers());
            hospital.setBranch(hospitalDto.getBranch().getBytes());
            hospital.setDepartments(hospitalDto.getDepartments().getBytes());
            hospital.setStatus(hospitalDto.getStatus());
            hospital.setCreatedUser(loginUser);
            hospital.setModifiedUser(loginUser);
            hospital.setCreatedDate(LocalDate.now());
            hospital.setModifiedDate(LocalDate.now());

            // Setting the hospital status as active (1)
            hospital.setStatus(1);

            // Save the hospital entity
            Boolean isSaved = hospitalService.saveHospital(hospital);
            if (isSaved) {
                return ResponseEntity.ok(hospital);
            } else {
                return new ResponseEntity<>("An error occurred while creating the hospital.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error while createHospital" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId()+ " Requested Data: "+hospitalDto);
            return new ResponseEntity<>("An error occurred while creating the hospital." + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }
    //------------------------------------  Get All Hospital -----------------------------------------

    //This method retrieves list of all hospitals.
    @GetMapping("/getAllHospital")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> getAllHospitals() {
        User loginUser = null;

        try {

            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            // Fetch all hospitals using the hospitalService
            List<Hospital> hospitals = hospitalService.getAllHospitals();

            // Transform each Hospital entity into a corresponding HospitalResponseDTO object
            List<HospitalResponseDTO> hospitalDTOs = hospitals.stream().map(hs -> {
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                // Set properties of the DTO from the Hospital entity
                hsDto.setId((hs.getId()));
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());
                return hsDto;
            }).collect(Collectors.toList()); // Collect the mapped DTOs into a list

            // Return a response with the list of mapped hospital DTOs
            return ResponseEntity.ok(hospitalDTOs);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An Unexpected error while getting all Hospitals" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("An error occurred while getting all the hospital." + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    //------------------------------------ Get Hospital by id -----------------------------------------

    //This method retries the hospital details of specific hospital by using its unique id.
    @GetMapping("/getHospitalbyId/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> getHospitalById(@PathVariable long id) {

        User loginUser = null;

        try {
            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }
            Hospital hospital = hospitalService.getHospitalById(id);

            // Ensure the hospital is active (status = 1), return error if it's inactive
            if (hospital.getStatus()!=1) {
                return new ResponseEntity<>("This Hospital is de-activated", HttpStatus.UNAUTHORIZED);
            }
            // Check if the hospital exists. If it does, transform it into a HospitalResponseDTO
            if (hospital != null) {

                Hospital hs = hospital;
                HospitalResponseDTO hsDto = new HospitalResponseDTO();
                // Map fields from the Hospital entity to the HospitalResponseDTO object
                hsDto.setId(hs.getId());
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());

                // Return the mapped hospital details as a successful response
                return ResponseEntity.ok(hsDto);

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getHospitalById" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("Hospital with ID " + id + " not found", HttpStatus.NOT_FOUND);

        }
        return ResponseEntity.status(404).body("Hospital with ID " + id + " not found");
    }

    //------------------------------------  Get by Hospital name -----------------------------------------

    // This method retries the details of hospital by using hospital name.
    @GetMapping("/getHospitalbyName/{name}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> getHospitalByName(@PathVariable String name) {

        User loginUser = null;
        try {

            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            // Fetch the list of hospitals matching the provided name from the hospital service
            List<Hospital> hospitals = hospitalService.findHospitalByName(name);

            //This condition checks the hospital is empty or not, if its empty then it will returns message.
            if (hospitals.isEmpty()) {
                return new ResponseEntity<>("Hospital not found with name: " + name, HttpStatus.BAD_REQUEST);
            }

            //Here transform each Hospital object in hospitals list into corresponding HospitalResponseDto object.
            List<HospitalResponseDTO> hospitalDTOs = hospitals.stream()
                    .filter(hs -> hs.getStatus() != 0).map(hs -> { // // Only active hospitals are processed
                        HospitalResponseDTO hsDto = new HospitalResponseDTO();

                        hsDto.setId(hs.getId());
                        hsDto.setName(hs.getName());
                        hsDto.setEmailId(hs.getEmailId());
                        hsDto.setContactNumber(hs.getContactNumber());
                        hsDto.setAddress(new String(hs.getAddress()));
                        hsDto.setBranch(new String(hs.getBranch()));
                        hsDto.setDepartments(new String(hs.getDepartments()));
                        hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                        hsDto.setStatus(hs.getStatus());

                        return hsDto;
                    }).collect(Collectors.toList()); // Collect the mapped DTOs into a list

            return ResponseEntity.ok(hospitalDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting Hospital by Name" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("something went wrong...",HttpStatus.NOT_FOUND);
        }
    }

    //------------------------------------------Update Hospital------------------------------------------

    //This method is used for updating hospital details.
    @PutMapping(value = "/updateHospitalbyId/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> updateHospital(
            @PathVariable long id,
            @Valid @RequestBody HospitalResponseDTO hospitalResponseDTO,
            BindingResult bindingResult) {

        User loginUser = null;

        try {

            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            // Retrieve the hospital by ID from the hospital service
            Hospital hs = hospitalService.getHospitalById(id);

            // Check if the hospital exists by its ID
            if (hs == null) {
                return new ResponseEntity<>("Hospital not found with id " + id, HttpStatus.BAD_REQUEST);
            }

            // Ensure the hospital is active (status = 1), return error if it's inactive
            if (hs.getStatus()!=1) {
                return new ResponseEntity<>("This Hospital is de-active! please active this hospital before update:", HttpStatus.UNAUTHORIZED);
            }
            // Check if the address is provided and not empty
            if (hospitalResponseDTO.getAddress() == null) {
                return new ResponseEntity<>("Hospital address cannot be null", HttpStatus.BAD_REQUEST);
            } else if (hospitalResponseDTO.getAddress().trim().isEmpty()) {
                return new ResponseEntity<>("Hospital address is required", HttpStatus.BAD_REQUEST);

            }

            // Validate hospital name (only letters and spaces allowed)
            if (hospitalResponseDTO.getName() == null) {
                return new ResponseEntity<>("Hospital name cannot be null", HttpStatus.BAD_REQUEST);
            } else if ( !hospitalResponseDTO.getName().matches("^[A-Za-z ]+$")) {
                return new ResponseEntity<>("Hospital name must contain only letters", HttpStatus.BAD_REQUEST);
            }

            // Validate contact number (must be exactly 10 digits, no leading zeros)
            if (hospitalResponseDTO.getContactNumber() == null) {
                return new ResponseEntity<>("Contact number cannot be null", HttpStatus.BAD_REQUEST);
            } else if (hospitalResponseDTO.getContactNumber().matches("0000000000") || !hospitalResponseDTO.getContactNumber().matches("^\\d{10}$")) {
                return new ResponseEntity<>("Contact number must be exactly 10 digits or please put the correct number", HttpStatus.BAD_REQUEST);
            }

            // Validate email format (using regex)
            if (hospitalResponseDTO.getEmailId() == null ) {
                return new ResponseEntity<>("Email Id cannot be null", HttpStatus.BAD_REQUEST);
            } else if (!hospitalResponseDTO.getEmailId().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);
            }

            // Validate the branch (only letters and commas allowed)
            if (hospitalResponseDTO.getBranch() == null) {
                return new ResponseEntity<>("Branch cannot be null", HttpStatus.BAD_REQUEST);
            } else if (!hospitalResponseDTO.getBranch().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Branch must contain only letters and commas",HttpStatus.BAD_REQUEST);
            }

            // Validate the number of users (must be numeric)
            if (hospitalResponseDTO.getNumberOfUsers() == null) {
                return new ResponseEntity<>("Numbers of users cannot be null",HttpStatus.BAD_REQUEST);
            } else if (!hospitalResponseDTO.getNumberOfUsers().matches("^\\d+$")) {
                return new ResponseEntity<>("Number of users must be a numeric value", HttpStatus.BAD_REQUEST);
            }

            // Validate the departments (only letters and commas allowed)
            if (hospitalResponseDTO.getDepartments() == null) {
                return new ResponseEntity<>("Department must contain only letters and commas", HttpStatus.BAD_REQUEST);
            } else if (!hospitalResponseDTO.getDepartments().matches("^[A-Za-z, ]+$")) {
                return new ResponseEntity<>("Department must contain only letters and commas", HttpStatus.BAD_REQUEST);
            }

            // Set the updated values for the hospital entity
            Hospital hospital = hs;

            // This formatter is used to format and parse time in 24 hour format.
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            hospital.setName(hospitalResponseDTO.getName().trim().replaceAll("\\s+", " "));
            hospital.setAddress(hospitalResponseDTO.getAddress().getBytes());
            hospital.setContactNumber(hospitalResponseDTO.getContactNumber());
            hospital.setEmailId(hospitalResponseDTO.getEmailId());
            hospital.setNumberOfUsers(hospitalResponseDTO.getNumberOfUsers());
            hospital.setBranch(hospitalResponseDTO.getBranch().trim().getBytes());
            hospital.setDepartments(hospitalResponseDTO.getDepartments().trim().getBytes());
            // Save the updated hospital object and return the response

            Boolean isUpdated = hospitalService.saveHospital(hospital);
            if (isUpdated) {
                return ResponseEntity.ok(hospital);
            } else {
                return new ResponseEntity<>("An error occurred while creating the hospital.", HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while updateHospital" + ExceptionUtils.getStackTrace(e) + "Logged User: " + loginUser.getId() + " Requested Data: "+hospitalResponseDTO);
            return new ResponseEntity<>("something went wrong..." + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


    //------------------------------------Active or Deactivate Hospital -----------------------------------------
    //This method is used for Active and deactive particular hospital by its id.
    @PutMapping("/activeAndDeactivateHospitalbyId/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> activeAndDeactivateHospital(@PathVariable long id) {

        // Fetch the authenticated user from the user service
        User loginUser = null;
        String status;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            // Retrieve the hospital by ID from the hospital service
            Hospital hospital = hospitalService.getHospitalById(id);

            // If the hospital is not found, return a BAD_REQUEST response
            if (hospital == null) {
                return new ResponseEntity<>("Hospital not found.", HttpStatus.BAD_REQUEST);
            }
            if (hospital.getStatus() == 0) {
                hospital.setStatus(1);
                status = "Activated";
            } else {
                hospital.setStatus(0);
                status = "Deactivated";
            }

            // Set the hospital's status to deactivated (0)
            hospital.setModifiedDate(LocalDate.now());

            // Save the updated hospital status in the database
            hospitalService.saveHospital(hospital);
            return new ResponseEntity<>("Hospital with ID " + id + " has been "+status+" successfully.", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId()+ " Requested Data: "+id);
            return new ResponseEntity<>("something went wrong..." + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    //This method is used for get the hospital list of active hospitals.
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/allActiveHospitals")
    public synchronized ResponseEntity<?> getAllActiveHospitals() {
        User loginUser = null;
        try {

            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            // Get the list of active hospitals from the hospital service
            List<Hospital> hospital = hospitalService.findActiveHospitals();

            // Map the list of Hospital entities to HospitalResponseDTO objects
            List<HospitalResponseDTO> hospitalDTOs = hospital.stream().map(hs -> {
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                hsDto.setId((hs.getId()));
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());
                return hsDto;
            }).collect(Collectors.toList()); // Collect the mapped DTOs into a list

            // If the list of active hospitals is empty, return a response indicating no active hospitals are available
            if (hospital.isEmpty()) {
                return new ResponseEntity<>("Active hospitals are not available", HttpStatus.BAD_REQUEST);
            }

            // Return the list of hospital DTOs if there are active hospitals
            return ResponseEntity.ok(hospitalDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting all Active Hospitals" + ExceptionUtils.getStackTrace(e) + "Logged User : " + loginUser.getId());
            return new ResponseEntity<>("something went wrong... " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    //------------------------------------  Get all de-active Hospital -----------------------------------------

    //This method is used for get the list of deleted or de-active hospitals.
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/allDeactivatedHospitals")
    public synchronized ResponseEntity<?> getAllDeactivatedHospitals() {

        User loginUser = null;
        try {

            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            // Get the list of deactivated hospitals from the hospital service
            List<Hospital> hospital = hospitalService.findDeactivatedHospitals();

            // Map the list of Hospital entities to HospitalResponseDTO objects
            List<HospitalResponseDTO> hospitalDTOs = hospital.stream().map(hs -> {
                HospitalResponseDTO hsDto = new HospitalResponseDTO();

                // Set various properties of the DTO from the Hospital entity
                hsDto.setId((hs.getId()));
                hsDto.setName(hs.getName());
                hsDto.setEmailId(hs.getEmailId());
                hsDto.setContactNumber(hs.getContactNumber());
                hsDto.setAddress(new String(hs.getAddress()));
                hsDto.setBranch(new String(hs.getBranch()));
                hsDto.setDepartments(new String(hs.getDepartments()));
                hsDto.setNumberOfUsers(hs.getNumberOfUsers());
                hsDto.setStatus(hs.getStatus());
                return hsDto;
            }).collect(Collectors.toList()); // Collect the mapped DTOs into a list

            // If the list of hospitals is empty, return a response indicating no deactivated hospitals are available
            if (hospital.isEmpty()) {
                return new ResponseEntity<>("Deactivated hospitals are not available", HttpStatus.BAD_REQUEST);
            }

            // Return the list of hospital DTOs if there are deactivated hospitals
            return ResponseEntity.ok(hospitalDTOs);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting all deactivated Hospitals" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return new ResponseEntity<>("something went wrong... " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/getBranchesbyHospitalId/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public synchronized ResponseEntity<?> getBranchesByHospitalId(@PathVariable long id) {

        User loginUser = null;

        try {
            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated",HttpStatus.UNAUTHORIZED);
            }

            Hospital hospital = hospitalService.getBranchesByHospitalId(id);

            // Check if the hospital exists by its ID
            if (hospital == null) {
                return new ResponseEntity<>("Hospital not found with id " + id, HttpStatus.BAD_REQUEST);
            }

            // Ensure the hospital is active (status = 1), return error if it's inactive
            if (hospital.getStatus()!=1) {
                return new ResponseEntity<>("This Hospital is de-active!", HttpStatus.UNAUTHORIZED);
            }

            byte[] byteData = hospital.getBranch();

            String data = new String(byteData);
            String[] branchesArray = data.split(",");

            ArrayList<String> branchesList = new ArrayList<>();
            for(String str : branchesArray)
            {
                branchesList.add(str.trim());
            }
            return ResponseEntity.status(200).body(branchesList);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting hospital branches." + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("Hospital with ID " + id + " not found", HttpStatus.NOT_FOUND);

        }
    }
}