package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.PatientDTO;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.EmailService;
import com.fspl.medica_healthcare.services.HospitalService;
import com.fspl.medica_healthcare.services.PatientService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.annotation.PostConstruct;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private static final Logger logger = Logger.getLogger(PatientController.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private PatientService patientService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Autowired
    private EncryptionUtil util;

    @Autowired
    private HospitalService hospitalService;

    public static String getPatientBirthdayTemplete(String patientName, String hospitalName) {
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
                "<h1>🎉 Happy Birthday, " + patientName + "! 🎂</h1>" +
                "</div>" +
                "<div class=\"body\">" +
                "<p>We at <strong>" + hospitalName + " 🎂</strong> wish you a day filled with joy, laughter, and good health.</p>" +
                "<p>May this year bring you happiness, success, and wellness.</p>" +
                "<p>Stay healthy and enjoy your special day! 🎊</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    //========================================= RETRIEVES THE LIST OF ACTIVE PATIENT ===================================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/getActivePatientsList")
    public synchronized ResponseEntity<?> getActivePatients() {
        try {
            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

            List<Patient> activePatients = patientService.getPatientsByStatus(1);  // Fetch the list of active patients (status = 1)

            // If no active patients are found, return a response with  message
            if (activePatients == null || activePatients.isEmpty()) {
                return ResponseEntity.status(400).body("No active patients found.");
            }

            List<PatientDTO> patientDTOList = new ArrayList<>();   // Create a list to store patient DTOs for response

            // Iterate through the list of active patients and map their details to the DTO
            for (Patient patient : activePatients) {
                PatientDTO patientDTO = new PatientDTO();

                // Mapping basic patient information from patient DTO
                patientDTO.setId(patient.getId());
                patientDTO.setName(Arrays.toString(patient.getName()));
                patientDTO.setContactNumber(patient.getContactNumber());
                patientDTO.setEmailId(Arrays.toString(patient.getEmailId()));
                patientDTO.setCurrentDoctor(patient.getCurrentDoctor());
                patientDTO.setAge(Integer.parseInt(util.decrypt(patient.getAge())));
                patientDTO.setDiet(patient.getDiet());
                patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));
                patientDTO.setCreatedUser(patient.getCreatedUser());
                patientDTO.setModifiedUser(patient.getModifiedUser());
                patientDTO.setHospital(patient.getHospital());
                patientDTO.setStatus(patient.getStatus());
                patientDTO.setCurrentStatus(patient.getCurrentStatus());

                // Decrypt sensitive patient information before sending it in response
                patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");

                patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));


                //new terms for decrypt
                patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");
                patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");

                // Set created and modified dates if they exist
                if (patient.getCreatedDate() != null) {
                    patientDTO.setCreatedDate(patient.getCreatedDate());
                }
                if (patient.getModifiedDate() != null) {
                    patientDTO.setModifiedDate(patient.getModifiedDate());
                }

                // Add the DTO to the response list
                patientDTOList.add(patientDTO);
            }

            return ResponseEntity.ok(patientDTOList);  // Return the list of active patients as a response

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error details along with the authenticated user ID
            logger.error("Error in getActivePatients(): " + ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User ID: " + userService.getAuthenticateUser().getId());

            return ResponseEntity.status(500).body("Error fetching active patients: " + e.getMessage());// Return a response in case of an error
        }
    }

    //========================================== RETRIEVES THE LIST OF INACTIVE PATIENTS ================================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/getInActivePatientsList")
    public synchronized ResponseEntity<?> getInActivePatients() {
        try {
            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

            List<Patient> inactivePatients = patientService.getPatientsByStatus(0);  // Fetch the list of active patients (status = 1)

            // If no active patients are found, return a response with  message
            if (inactivePatients == null || inactivePatients.isEmpty()) {
                return ResponseEntity.status(400).body("No active patients found.");
            }

            List<PatientDTO> patientDTOList = new ArrayList<>();   // Create a list to store patient DTOs for response

            // Iterate through the list of active patients and map their details to the DTO
            for (Patient patient : inactivePatients) {
                PatientDTO patientDTO = new PatientDTO();

                // Mapping basic patient information from patient DTO
                patientDTO.setId(patient.getId());
                patientDTO.setName(Arrays.toString(patient.getName()));
                patientDTO.setContactNumber(patient.getContactNumber());
                patientDTO.setEmailId(Arrays.toString(patient.getEmailId()));
                patientDTO.setCurrentDoctor(patient.getCurrentDoctor());
                patientDTO.setAge(Integer.parseInt(util.decrypt(patient.getAge())));
                patientDTO.setDiet(patient.getDiet());
                patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));
                patientDTO.setCreatedUser(patient.getCreatedUser());
                patientDTO.setModifiedUser(patient.getModifiedUser());
                patientDTO.setHospital(patient.getHospital());
                patientDTO.setStatus(patient.getStatus());
                patientDTO.setCurrentStatus(patient.getCurrentStatus());

                // Decrypt sensitive patient information before sending it in response
                patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");

                patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));


                //new terms for decrypt
                patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");
                patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");

                // Set created and modified dates if they exist
                if (patient.getCreatedDate() != null) {
                    patientDTO.setCreatedDate(patient.getCreatedDate());
                }
                if (patient.getModifiedDate() != null) {
                    patientDTO.setModifiedDate(patient.getModifiedDate());
                }

                // Add the DTO to the response list
                patientDTOList.add(patientDTO);
            }

            return ResponseEntity.ok(patientDTOList);  // Return the list of active patients as a response

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error details along with the authenticated user ID
            logger.error("Error in getActivePatients(): " + ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User ID: " + userService.getAuthenticateUser().getId());

            return ResponseEntity.status(500).body("Error fetching active patients: " + e.getMessage());// Return a response in case of an error
        }
    }

    //======================================= RETRIEVES THE LIST OF PATIENTS WITH ACTIVE CURRENT STATUS ===================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/getCurrentActivePatientsList")
    public synchronized ResponseEntity<?> getActiveCurrentStatusPatients() {
        try {
            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

            List<Patient> currentActivePatients = patientService.getPatientsByCurrentStatus(1);// Fetch the list of patients who are active based on their current status (currentStatus = 1)

            // If no active patients are found, return a response with an appropriate message
            if (currentActivePatients == null || currentActivePatients.isEmpty()) {
                return ResponseEntity.status(400).body("No active patients found with current status.");
            }

            List<PatientDTO> patientDTOList = new ArrayList<>(); // Create a list to store patient DTOs for response

            // Iterate through the list of active patients and map their details to the DTO
            for (Patient patient : currentActivePatients) {
                PatientDTO patientDTO = new PatientDTO();

                // Mapping basic patient information
                patientDTO.setId(patient.getId());
                patientDTO.setName(Arrays.toString(patient.getName()));
                patientDTO.setContactNumber(patient.getContactNumber());
                patientDTO.setEmailId(Arrays.toString(patient.getEmailId()));
                patientDTO.setCreatedUser(patient.getCreatedUser());
                patientDTO.setModifiedUser(patient.getModifiedUser());
                patientDTO.setHospital(patient.getHospital());
                patientDTO.setAge(Integer.parseInt(util.decrypt(patient.getAge())));

                // Decrypt sensitive patient information before sending it in response
                patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");
                patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));

                patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));
                patientDTO.setDiet(patient.getDiet());


                // Set patient's current status and doctor details
                patientDTO.setCurrentStatus(patient.getCurrentStatus());
                patientDTO.setCurrentDoctor(patient.getCurrentDoctor());
                patientDTO.setStatus(patient.getStatus());

                patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");
                patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");

                // Set created and modified dates if they exist
                patientDTO.setCreatedDate(patient.getCreatedDate());
                patientDTO.setModifiedDate(patient.getModifiedDate());

                patientDTOList.add(patientDTO);  // Add the populated DTO to the response list
            }

            return ResponseEntity.ok(patientDTOList); // Return the list of currently active patients as a response

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error details along with the authenticated user ID
            logger.error("Error in getActiveCurrentStatusPatients(): " + ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User ID: " + userService.getAuthenticateUser().getId());

            return ResponseEntity.status(500).body("Error fetching active patients: " + e.getMessage());  // Return a response in case of an error
        }
    }

    //=================================== RETRIEVES THE LIST OF PATIENTS WITH INACTIVE CURRENT STATUS ================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/getCurrentInactivePatientsList")
    public synchronized ResponseEntity<?> getInactiveCurrentStatusPatients() {
        try {
            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

            List<Patient> currentInactivePatients = patientService.getPatientsByCurrentStatus(0); // Fetch the list of patients who are inactive based on their current status (currentStatus = 0)

            // If no inactive patients are found, return a response with message
            if (currentInactivePatients == null || currentInactivePatients.isEmpty()) {
                return ResponseEntity.status(400).body("No inactive patients found with current status.");
            }

            List<PatientDTO> patientDTOList = new ArrayList<>();// Create a list to store patient DTOs for response

            // Iterate through the list of inactive patients and map their details to the DTO
            for (Patient patient : currentInactivePatients) {
                PatientDTO patientDTO = new PatientDTO();

                // Mapping basic patient information
                patientDTO.setId(patient.getId());
                patientDTO.setName(Arrays.toString(patient.getName()));
                patientDTO.setContactNumber(patient.getContactNumber());
                patientDTO.setEmailId(Arrays.toString(patient.getEmailId()));
                patientDTO.setCreatedUser(patient.getCreatedUser());
                patientDTO.setModifiedUser(patient.getModifiedUser());
                patientDTO.setHospital(patient.getHospital());
                patientDTO.setCurrentDoctor(patient.getCurrentDoctor());
                patientDTO.setAge(Integer.parseInt(util.decrypt(patient.getAge())));


                // Decrypt sensitive patient information before sending it in response
                patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");
                patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));
                patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));
                patientDTO.setDiet(patient.getDiet());


                patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");
                patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");

                // Set patient's current status and doctor details
                patientDTO.setCurrentStatus(patient.getCurrentStatus());
                patientDTO.setCurrentDoctor(patient.getCurrentDoctor());
                patientDTO.setStatus(patient.getStatus());

                // Set created and modified dates if they exist
                patientDTO.setCreatedDate(patient.getCreatedDate());
                patientDTO.setModifiedDate(patient.getModifiedDate());

                patientDTOList.add(patientDTO);  // Add the DTO to the response list
            }

            return ResponseEntity.ok(patientDTOList);// Return the list of currently inactive patients as a response

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error details along with the authenticated user ID
            logger.error("Error in getInactiveCurrentStatusPatients(): " + ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User ID: " + userService.getAuthenticateUser().getId());

            return ResponseEntity.status(500).body("Error fetching inactive patients: " + e.getMessage()); // Return a response in case of an error
        }
    }

    //============================================== RETRIEVES A PATIENT BY PATIENT ID =========================================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/GetPatientByPatientId/{id}")
    public synchronized ResponseEntity<?> getPatientById(@PathVariable long id) {
        try {
            // Validate the patient ID; it must be a positive number.
            if (id < 0) {
                return ResponseEntity.badRequest().body("Error: Patient ID must be a valid positive number.");
            }

            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

            Patient patient = patientService.getPatientById(id);  // Fetch patient details by ID from the database

            // If no patient is found with the given ID, return a response
            if (patient == null) {
                return ResponseEntity.status(200).body("Error: No patient found with ID: " + id);
            }

            // Create a PatientDTO object to transfer the patient data
            PatientDTO patientDTO = new PatientDTO();

            // Mapping basic patient information
            patientDTO.setId(patient.getId());
            patientDTO.setName(Arrays.toString(patient.getName()));
            patientDTO.setContactNumber(patient.getContactNumber());

            // Decrypt sensitive patient information before sending it in response
            patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
            patientDTO.setEmailId(Arrays.toString(patient.getEmailId()));
            patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
            patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");

            patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));
            patientDTO.setDiet(patient.getDiet());
            patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));

            // Setting user details and hospital information
            patientDTO.setCreatedUser(patient.getCreatedUser());
            patientDTO.setModifiedUser(patient.getModifiedUser());
            patientDTO.setHospital(patient.getHospital());

            // Setting patient's status and current doctor
            patientDTO.setStatus(patient.getStatus());
            patientDTO.setCurrentStatus(patient.getCurrentStatus());
            patientDTO.setCurrentDoctor(patient.getCurrentDoctor());

            patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");
            patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
            patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");

            // Convert date values properly to prevent potential errors
            if (patient.getCreatedDate() != null) {
                patientDTO.setCreatedDate(LocalDate.from(LocalDate.from(patient.getCreatedDate()).atStartOfDay()));
            }

            if (patient.getModifiedDate() != null) {
                patientDTO.setModifiedDate(LocalDate.from(LocalDate.from(patient.getModifiedDate()).atStartOfDay()));
            }

            return ResponseEntity.ok(patientDTO);  // Return the patient's details as a response

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error details along with the authenticated user ID for debugging
            logger.error("Error in getPatientById(): " + ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User ID: " + userService.getAuthenticateUser().getId());

            return ResponseEntity.status(500).body("An error occurred while fetching patient data: " + e.getMessage());// Return a response in case of an error
        }
    }

    //=========================================== RETRIEVES PATIENTS BY CONTACT NUMBER ================================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/getPatientsByContactNumber")
    public ResponseEntity<?> getPatientsByContactNumber(
            @RequestParam(value = "contactNumber", required = false) String contactNumber) {
        try {
            // Validate that the contact number is not null or empty
            if (contactNumber == null || contactNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: Contact number is required.");
            }

            // Validate the contact number length (should be exactly 10 digits)
            if (contactNumber.length() > 10) {
                return ResponseEntity.badRequest().body("Error: Contact number must contain exactly 10 digits.");
            }

            // Ensure the contact number contains only numeric digits
            if (!contactNumber.matches("^\\d+$")) {
                return ResponseEntity.badRequest().body("Error: Contact number should contain only numeric digits.");
            }

            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

//            String encContactNumber = util.encrypt(contactNumber);

            List<Patient> patients = patientService.getPatientsByContactNumber(contactNumber);  // Fetch the list of patients associated with the given contact number

            List<Patient> allPatients = patientService.getAllPatientsByHospital(user.getHospital().getId());


            List<PatientDTO> matchedPatients = new ArrayList<>(); // Create a list patient details in DTO format

            // Iterate over each patient and map their details to a DTO
            for (Patient patient : allPatients) {
                String decContactNumber = patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "";
                if (decContactNumber.contains(contactNumber)) {
                    PatientDTO patientDTO = new PatientDTO();

                    // Map basic patient information
                    patientDTO.setId(patient.getId());
                    patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));
                    patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");
                    patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                    patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));

                    // Decrypt sensitive patient information before sending it in response
                    patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                    patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");
                    patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                    patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");
                    patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));

                    patientDTO.setDiet(patient.getDiet());
                    patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));

                    // Map additional patient details
                    patientDTO.setCreatedUser(patient.getCreatedUser());
                    patientDTO.setModifiedUser(patient.getModifiedUser());
                    patientDTO.setHospital(patient.getHospital());
                    patientDTO.setStatus(patient.getStatus());
                    patientDTO.setCurrentStatus(patient.getCurrentStatus());
                    patientDTO.setCurrentDoctor(patient.getCurrentDoctor());

                    //new encrypt
                    //  patientDTO.setName(patient.getName() != null ? util.decrypt(Arrays.toString(patient.getName())) : "");
                    patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");
                    patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");

                    // Set created and modified dates if available
                    if (patient.getCreatedDate() != null) {
                        patientDTO.setCreatedDate(patient.getCreatedDate());
                    }

                    if (patient.getModifiedDate() != null) {
                        patientDTO.setModifiedDate(patient.getModifiedDate());
                    }

                    matchedPatients.add(patientDTO); // Add the mapped patientDTO to the list
                }
            }

            if (matchedPatients.isEmpty()) {
                return ResponseEntity.ok("No patient Found with Contact Number : " + contactNumber);
            }

            return ResponseEntity.ok(matchedPatients); // Return the list of patients as a response

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error details along with the authenticated user ID for debugging
            logger.error("Error in getPatientsByContactNumber(): " + ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User ID: " + userService.getAuthenticateUser().getId());

            return ResponseEntity.status(500).body("An error occurred while fetching patient data: " + e.getMessage());// Return a response in case of an error
        }
    }

    //=================================================== RETRIEVES PATIENTS BY NAME ====================================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/getPatientsByPatientName/{name}")
    public synchronized ResponseEntity<?> getPatientsByName(@PathVariable String name) {
        try {
            // Validate that the name is not null or empty
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Error: Name is required.");
            }

            // Validate that the name contains only alphabets and spaces
            if (!name.matches("^[a-zA-Z ]+$")) {
                return ResponseEntity.badRequest().body("Error: Name should contain only alphabets and spaces.");
            }

            name = name.trim().toUpperCase();  // Normalize the name for case-insensitive search (convert to Uppercase and trim spaces)

//            String encName = util.encrypt(name);

            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

            List<Patient> patients = patientService.getPatientsByName(name);  // Fetch the list of patients matching the given name

            List<Patient> allPatients = patientService.getAllPatientsByHospital(user.getHospital().getId());


            List<PatientDTO> matchedPatients = new ArrayList<>(); // Create a list patient details in DTO format

            // Iterate over each patient and map their details to a DTO
            for (Patient patient : allPatients) {
                String decName = patient.getName() != null ? util.decrypt(new String(patient.getName())).toUpperCase() : "";

                if (decName.contains(name)) {
                    PatientDTO patientDTO = new PatientDTO();

                    // Map basic patient information
                    patientDTO.setId(patient.getId());
                    patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");

                    // Decrypt sensitive patient information before sending it in response
                    patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                    patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");
                    patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));
                    patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                    patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");
                    patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));
                    patientDTO.setStatus(patient.getStatus());
                    patientDTO.setCurrentStatus(patient.getCurrentStatus());
                    patientDTO.setDiet(patient.getDiet());
                    patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");

                    // Map additional patient details
                    patientDTO.setCreatedUser(patient.getCreatedUser());
                    patientDTO.setModifiedUser(patient.getModifiedUser());
                    patientDTO.setHospital(patient.getHospital());
                    patientDTO.setCurrentDoctor(patient.getCurrentDoctor());

//
                    // Set created and modified dates if available
                    if (patient.getCreatedDate() != null) {
                        patientDTO.setCreatedDate(LocalDate.from(LocalDate.from(patient.getCreatedDate()).atStartOfDay()));
                    }

                    if (patient.getModifiedDate() != null) {
                        patientDTO.setModifiedDate(LocalDate.from(LocalDate.from(patient.getModifiedDate()).atStartOfDay()));
                    }

                    matchedPatients.add(patientDTO);  // Add the mapped patientDTO to the list
                }
            }

            if (matchedPatients.isEmpty()) {
                return ResponseEntity.ok("No Patients Found with the Name : " + name);
            }

            return ResponseEntity.ok(matchedPatients); // Return the list of patients as a response

        } catch (Exception e) {
            e.printStackTrace();
            // Log the error details along with the authenticated user ID for debugging
            logger.error("Error in getPatientsByName(): " + ExceptionUtils.getStackTrace(e) +
                    " LoggedIn User ID: " + userService.getAuthenticateUser().getId());

            return ResponseEntity.status(500).body("An error occurred while fetching patient data: " + e.getMessage());
        }
    }

    //=================================================== RETRIEVES PATIENTS BY Hospital Id ====================================================//
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    @GetMapping("/getAllPatientsByHospitalId/{hospital_id}")
    public ResponseEntity<?> getAllPatientByHospitalId(@PathVariable(required = false) long hospital_id) {
        try {
            // Retrieve the currently authenticated user
            User user = userService.getAuthenticateUser();

            // Validate the hospital_id; it must be a positive number greater than zero
            if (hospital_id <= 0) {
                return ResponseEntity.badRequest().body("Error: Hospital ID must be a positive number and Greater than Zero");
            }

            // Check if the hospital exists by its ID
            Hospital hospitalExists = hospitalService.getHospitalById(hospital_id);
            if (hospitalExists == null) {
                return ResponseEntity.badRequest().body("Error: No Hospital found with ID " + hospital_id);
            }

            // Retrieve all patients associated with the specified hospital
            List<Patient> allPatientsByHospital = patientService.getAllPatientsByHospital(hospital_id);

            // If no patients are found, return a message indicating that
            if (allPatientsByHospital == null || allPatientsByHospital.isEmpty()) {
                return ResponseEntity.status(200).body("No Patient Found By Hospital Id:- " + hospital_id);
            }

            List<PatientDTO> patientDTOList = new ArrayList<PatientDTO>();

            // Iterate through each patient and map relevant fields to the PatientDTO object
            for (Patient patient : allPatientsByHospital) {
                PatientDTO patientDTO = new PatientDTO();

                patientDTO.setId(patient.getId());
                patientDTO.setName(patient.getName() != null ? util.decrypt(new String(patient.getName())) : "");
                patientDTO.setContactNumber(patient.getContactNumber() != null ? util.decrypt(patient.getContactNumber()) : "");

                // Decrypt sensitive information like WhatsApp number and gender
                patientDTO.setWhatsAppNumber(patient.getWhatsAppNumber() != null ? util.decrypt(patient.getWhatsAppNumber()) : "");
                patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(new String(patient.getEmailId())) : "");
                patientDTO.setGender(patient.getGender() != null ? util.decrypt(patient.getGender()) : "");
                patientDTO.setBloodGroup(patient.getBloodGroup() != null ? util.decrypt(patient.getBloodGroup()) : "");

                // Map additional details like current doctor, age, diet, etc.
                patientDTO.setCurrentDoctor(patient.getCurrentDoctor());
                patientDTO.setAge(Integer.parseInt(patient.getAge() != null ? util.decrypt(patient.getAge()) : ""));
                patientDTO.setDiet(patient.getDiet());
                patientDTO.setDateOfBirth(util.decrypt(patient.getDateOfBirth()));
                patientDTO.setCreatedUser(patient.getCreatedUser());
                patientDTO.setModifiedUser(patient.getModifiedUser());
                patientDTO.setHospital(patient.getHospital());
                patientDTO.setStatus(patient.getStatus());
                patientDTO.setCurrentStatus(patient.getCurrentStatus());
                patientDTO.setCreatedDate(patient.getCreatedDate());
                patientDTO.setModifiedDate(patient.getModifiedDate());

                patientDTOList.add(patientDTO);
            }

            return ResponseEntity.ok(patientDTOList);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getAllPatientByHospitalId(): " + ExceptionUtils.getStackTrace(e) + " LoggedIn User ID: " + userService.getAuthenticateUser().getId());
            return ResponseEntity.status(500).body("Error fetching All Patients By Hospital ID: " + e.getMessage());
        }
    }

    //=============================================== SCHEDULES BIRTHDAY EMAILS DAILY ===================================================//
    @PostConstruct
    public void scheduleBirthdayEmails() {
        try {
            LocalDateTime now = LocalDateTime.now();   // Get the current time

            LocalDateTime nextRun = now.withHour(18).withMinute(6).withSecond(0); // Define the time for the birthday email job to run (5:06 PM)

            // If the current time is already past 5:06 PM, schedule for the next day
            if (now.isAfter(nextRun)) {
                nextRun = nextRun.plusDays(1);
            }

            long initialDelay = java.time.Duration.between(now, nextRun).toMinutes();   // Calculate the initial delay (in minutes) until the first run

            // Schedule the task to run daily at 5:06 PM
            scheduler.scheduleAtFixedRate(() -> {
                try {

                    // Get the current day and month
                    int birthdateMonth = LocalDateTime.now().getMonthValue();
                    int birthdateDay = LocalDateTime.now().getDayOfMonth();

                    List<Patient> patientBirthdayList = patientService.getPatientBirthdayWithHospital(birthdateMonth, birthdateDay);   // Fetch patients whose birthday is today

                    // If no patients have birthdays today, log a message and return
                    if (patientBirthdayList == null) {
                        System.out.println("No patients found with a birthday today.");
                        return;
                    }

                    List<PatientDTO> patientDTOList = new ArrayList<>();

//                    List<Patient> patientDTOList = patientBirthdayList;

                    for (Patient patient : patientBirthdayList) {
                        PatientDTO patientDTO = new PatientDTO();

                        patientDTO.setName(patient.getName() != null ? util.decrypt(Arrays.toString(patient.getName())) : "");
                        patientDTO.setEmailId(patient.getEmailId() != null ? util.decrypt(Arrays.toString(patient.getEmailId())) : "");
                        patientDTO.setHospital(patient.getHospital());

                        patientDTOList.add(patientDTO);
                    }

                    int birthdayCount = 0;

                    // Loop through each patient and send an email
                    while (birthdayCount < patientDTOList.size()) {
                        PatientDTO patientDTO = patientDTOList.get(birthdayCount);
                        try {
                            // Send a personalized birthday email

                            emailService.sendEmail(
                                    patientDTO.getEmailId(),
                                    "🎂 Happy Birthday " + patientDTO.getName() + "!",
                                    getPatientBirthdayTemplete(
                                            patientDTO.getName(),
                                            patientDTO.getHospital().getName()
                                    ));
                        } catch (Exception e) {
                            // Log error if email sending fails
                            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
                        }
                        birthdayCount++;
                    }
                } catch (Exception e) {
                    // Log any unexpected errors occurring during the scheduling process
                    logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
                }
            }, initialDelay, 60 * 24, TimeUnit.MINUTES); // Repeat every 24 hours (daily)

            // Log the scheduler initialization
        } catch (Exception e) {
            e.printStackTrace();
            // Log any errors occurring during the scheduler initialization
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
        }
    }
}

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
