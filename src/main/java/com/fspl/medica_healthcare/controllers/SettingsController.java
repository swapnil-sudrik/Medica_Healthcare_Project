package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Settings;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.HospitalService;
import com.fspl.medica_healthcare.services.SettingsService;
import com.fspl.medica_healthcare.services.UserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    User loginUser = null;

    private static final Logger logger = Logger.getLogger(SettingsController.class);
    @Autowired
    private SettingsService settingsService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private UserService userService;

    //Creating and Updating Settings for Hospital

    @PostMapping("/createOrUpdateSettings")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> createOrUpdateSettings(@ModelAttribute Settings setting,
                                                                 @RequestParam("letterHead") MultipartFile letterHead,
                                                                 @RequestParam("logo") MultipartFile logo) {

        try {
            // get authenticated user
            loginUser = userService.getAuthenticateUser();

            //Check if settings already exist for Hospital
            Optional<Hospital> hospitalOptional = settingsService.findHospitalByHospitalId(setting.getHospital().getId());

            if (hospitalOptional.isEmpty()) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Record Not Found");
            }

            // Validating Hospital Opening Time and Closing Time

            String TIME_FORMAT = "HH:mm (e.g., 09:30 or 18:45)";
            if (validateAndParseTime(setting.getHospitalOpeningTime()) == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid hospitalOpeningTime. Use format " + TIME_FORMAT));
            }

            //Parsing the HospitalOpeningTime into LocalTime
            LocalTime openingTime = LocalTime.parse(setting.getHospitalOpeningTime());

            // check HospitalOpeningTime is in between 06:00 AM to 10:00 AM
            if (openingTime.isBefore(LocalTime.of(6, 0)) || openingTime.isAfter(LocalTime.of(10, 0))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Hospital opening time must be between 06:00 am and 10:00 am."));
            }

            // If HospitalClosingTime is null then throw error
            if (validateAndParseTime(setting.getHospitalClosingTime()) == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid hospitalClosingTime. Use format " + TIME_FORMAT));
            }

            // Getting the Hospital object , If it is present
            Hospital hospital = hospitalOptional.get();

            //Validating Logo And LetterHead

            //Check if the logo is present and in the correct format
            if (logo != null && !logo.isEmpty()) {
                if (!isValidImageType(logo)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid hospitalLogo format. Only JPG,SVG, JPEG, or PNG files are allowed."));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Hospital logo is required."));
            }

            //Check if the LetterHead is present and in the correct format
            if (letterHead != null && !letterHead.isEmpty()) {
                if (!isValidImageType(letterHead)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid hospitalLetterHead format. Only JPG, SVG,JPEG, or PNG files are allowed."));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Hospital letterhead is required."));
            }

            //Validating Working Days Of Hospital

            //Creating a list of Weekdays
            List<String> validDays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

            // Convert the list of valid days to a set with lowercase values for case-insensitive comparison.
            Set<String> validDaysSet = validDays.stream().map(String::toLowerCase).collect(Collectors.toSet());

            // Process and normalize the hospital's working days into a lowercase list without empty values.
            List<String> workingDaysList = Arrays.stream(setting.getHospitalWorkingDays().split(","))
                    .map(String::trim)
                    .filter(day -> !day.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            // Process and normalize the hospital's off days into a lowercase list without empty values.
            List<String> offDaysList = Arrays.stream(setting.getHospitalOffDays().split(","))
                    .map(String::trim)
                    .filter(day -> !day.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            //Converting WorkingDays and OffDays into set to remove duplicates
            Set<String> workingDaysSet = new HashSet<>(workingDaysList);
            Set<String> offDaysSet = new HashSet<>(offDaysList);

            //Finding invalid days by removing valid days from set
            Set<String> invalidWorkingDays = new HashSet<>(workingDaysSet);
            invalidWorkingDays.removeAll(validDaysSet);

            Set<String> invalidOffDays = new HashSet<>(offDaysSet);
            invalidOffDays.removeAll(validDaysSet);

            // if invalid day found then throw error
            if (!invalidWorkingDays.isEmpty() || !invalidOffDays.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid days found. Allowed days are Monday to Sunday. Invalid entries: " +
                                String.join(", ", invalidWorkingDays) + " " + String.join(", ", invalidOffDays)));
            }

            // Find common days from working days and off days
            Set<String> commonDays = new HashSet<>(workingDaysSet);
            commonDays.retainAll(offDaysSet);

            // if common day found in working days and off days then throw error
            if (!commonDays.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "The following days cannot be both working and off days: " + String.join(", ", commonDays)));
            }

            if (workingDaysList.size() != workingDaysSet.size() || offDaysList.size() != offDaysSet.size()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Duplicate days found. Please remove duplicate days from your input."));
            }

            // Check if Setting already exist for Hospital
            Optional<Settings> existingSettings = settingsService.findSettingsByHospital(hospital);

            //If setting already exists get it otherwise create new
            Settings settings = existingSettings.orElse(new Settings());

            // Validating No Of Ambulance and Ambulance Charges

            String ambulanceCount = setting.getNoOfAmbulances();

            //if no of ambulance is null then throw error
            if (ambulanceCount == null || ambulanceCount.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Number of ambulances is required."));
            }

            //trim the input
            ambulanceCount = ambulanceCount.trim();

            // Check if no of ambulance is a positive integer
            if (!ambulanceCount.matches("\\d+")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Number of ambulances must be a valid positive integer."));
            }

            // Validating No of Ambulance and Ambulance Charges

            Integer noOfAmbulances = Integer.valueOf(ambulanceCount);
            settings.setNoOfAmbulances(ambulanceCount);

            // If no of ambulance is zero then ambulance charges will be automatically zero
            if (noOfAmbulances == 0) {

                settings.setAmbulanceCharges("0");

            } else {
                String ambulanceChargesStr = setting.getAmbulanceCharges();

                if (ambulanceChargesStr == null || ambulanceChargesStr.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ambulance charges are required when ambulances are available."));
                }

                // Ambulance charges must be valid positive integer
                ambulanceChargesStr = ambulanceChargesStr.trim();

                if (!ambulanceChargesStr.matches("\\d+(\\.\\d+)?")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ambulance charges must be a valid positive number."));
                }

                //Parsing ambulance charges into Double
                Double ambulanceCharges = Double.parseDouble(ambulanceChargesStr);

                // If ambulance charges are less than zero then throw error
                if (ambulanceCharges < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ambulance charges cannot be negative."));
                }

                settings.setAmbulanceCharges(ambulanceChargesStr);
            }

            // validate Ambulance contact number

            String ambulanceContactNumber = setting.getAmbulanceContactNumber();

            // If Ambulance Contact Number is null or empty then throw error
            if (ambulanceContactNumber == null || ambulanceContactNumber.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Ambulance contact number is required."));
            }

            //Check if Ambulance Contact number is valid positive integer
            ambulanceContactNumber = ambulanceContactNumber.trim();

            if (!ambulanceContactNumber.matches("^\\d{10}$")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Ambulance contact number must be exactly 10 digits and only digits are allowed."));
            }
            // Validating GST Number and applyGst field

            // if GstNumber is null or empty set ApplyGst as zero otherwise set s it is in request
            if(setting.getGstNumber()==null || setting.getGstNumber().isEmpty())
            {
                settings.setApplyGst("0");
            }
            else {
                settings.setApplyGst(setting.getApplyGst());
            }

            if (setting.getApplyGst() == null || setting.getApplyGst().isEmpty() ||
                    setting.getApplyGst().equals("0") || setting.getApplyGst().equals("1")) {
                // If applyGst is null, empty, "0", or "1", it's valid, do nothing
            } else {
                // If it's anything else, return error
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("applyGst is invalid.");
            }


            if (setting.getGstNumber() != null && !setting.getGstNumber().isEmpty()) {
                String gstNumber = setting.getGstNumber().trim();

                // Validate GST Number using regex pattern
                String gstPattern = "\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}";
                if (!gstNumber.matches(gstPattern)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid GST number format. The correct format is: \nXXAAAAA9999A1Z9"));
                }

            }


            // Adding all details
            settings.setHospital(hospital);
            settings.setHospitalOpeningTime(String.valueOf(openingTime));
            settings.setHospitalClosingTime(String.valueOf(LocalTime.parse(setting.getHospitalClosingTime())));
            settings.setHospitalWorkingDays(setting.getHospitalWorkingDays());
            settings.setHospitalOffDays(setting.getHospitalOffDays());
            settings.setAmbulanceCharges(String.valueOf(Double.valueOf(setting.getAmbulanceCharges())));
            settings.setAmbulanceBookingTime(setting.getAmbulanceBookingTime());
            settings.setAmbulanceContactNumber(setting.getAmbulanceContactNumber());
            settings.setNoOfAmbulances(String.valueOf(Integer.valueOf(setting.getNoOfAmbulances())));
            settings.setStatus(1);
            settings.setHospitalLogo(logo.getBytes());
            settings.setHospitalLetterHead(letterHead.getBytes());
            settings.setGstNumber(setting.getGstNumber());


            // Setting createdUser , ModifiedUser, createdDate and ModifiedDate
            if (existingSettings.isPresent()) {

                settings.setModifiedUser(loginUser);
                settings.setModifiedDate(LocalDate.now());
            } else {

                settings.setCreatedUser(loginUser);
                settings.setModifiedUser(loginUser);

                settings.setCreatedDate(LocalDate.now());
                settings.setModifiedDate(LocalDate.now());
            }

            // Save Settings
            settingsService.saveSettings(settings, letterHead, logo);

            if (existingSettings.isPresent()) {
                return ResponseEntity.ok(settings);
            } else {
                return ResponseEntity.status(HttpStatus.CREATED).body(settings);
            }

        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error processing request: {}" + stringWriter + "Logged User :" + loginUser.getId() +"Request Data:"+setting);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while processing the request."));
        }
    }


    // Get ALL Settings

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/getAllSettings")
    public synchronized ResponseEntity<List<Settings>> getAllSettings() {
        try {
            List<Settings> settingsList = settingsService.getAllSettings();
            if (settingsList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return ResponseEntity.ok(settingsList);
        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error fetching settings: {}" + stringWriter + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    //  Get Settings By ID

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/getSettingsByHospitalId/{hospitalId}")
    public synchronized ResponseEntity<?> getSettingsByHospitalId(@PathVariable long hospitalId) {
        try {

            // Check if hospitalId is a positive number
            if (hospitalId <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Record Not Found");
            }

            Optional<Settings> settings = settingsService.findSettingsByHospitalId(hospitalId);
            if (settings.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Record Not Found");
            }
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error fetching in get Setting by hospitalID : "+hospitalId+" | " + stringWriter + "Logged User :" + loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while fetching the settings: " + e.getMessage()));
        }
    }


    // Get Logo and LetterHead Of Hospital

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/byType/{type}/{settingId}")
    public synchronized ResponseEntity<?> getImage(@PathVariable String type, @PathVariable long settingId) {
        try {
            Optional<byte[]> imageData;

            switch (type.toLowerCase()) {
                case "letterhead":
                    imageData = settingsService.getLetterHead(settingId);
                    break;
                case "logo":
                    imageData = settingsService.getLogo(settingId);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "Invalid type parameter. Use 'letterHead' or 'logo'."));
            }

            // Return the image if present otherwise throw error
            if (imageData.isPresent()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .body(imageData.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", type + " is not present for ID: " + settingId));
            }
        } catch (Exception e) {

            e.printStackTrace(printWriter);
            logger.error("Error fetching for setting id :"+settingId+" | " + stringWriter + "Logged User :" + loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "An error occurred while fetching the " + type + ": " + e.getMessage()));
        }
    }



    // Method to check image type

    private boolean isValidImageType(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return false;
            }

            String contentType = file.getContentType();

            return contentType != null &&
                    (contentType.equals("image/jpeg") ||
                            contentType.equals("image/png") ||
                            contentType.equals("image/jpg") ||
                            contentType.equals("image/svg"));
        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error Occurred in Checking image type : {}" + stringWriter);
            return false;
        }


    }

    // Method to parse time

    private LocalTime validateAndParseTime(String time) {
        try {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            e.printStackTrace(printWriter);
            logger.error("Failed to parse time : {}" + stringWriter + "Logged User :" + loginUser.getId());
            return null;
        }
    }
}