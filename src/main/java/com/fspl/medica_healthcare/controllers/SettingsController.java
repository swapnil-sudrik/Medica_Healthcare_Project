package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Settings;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.UserRepository;
import com.fspl.medica_healthcare.services.HospitalService;
import com.fspl.medica_healthcare.services.SettingsService;
import com.fspl.medica_healthcare.services.UserService;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    User currentUser = null;

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

            currentUser = userService.getAuthenticateUser();

            Optional<Hospital> hospitalOptional = settingsService.findByHospital(setting.getHospital().getId());
            if (hospitalOptional.isEmpty()) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid hospitalId. Hospital not found."));
            }

            // Vaidating Hospital Opening Time and Closing Time

            String timeFormat = "HH:mm (e.g., 09:30 or 18:45)";
            if (parseTime(setting.getHospitalOpeningTime()) == null) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid hospitalOpeningTime. Use format " + timeFormat));
            }

            LocalTime openingTime = LocalTime.parse(setting.getHospitalOpeningTime());

            if (openingTime.isBefore(LocalTime.of(6, 0)) || openingTime.isAfter(LocalTime.of(10, 0))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Hospital opening time must be between 06:00 am and 10:00 am."));
            }

            if (parseTime(setting.getHospitalClosingTime()) == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid hospitalClosingTime. Use format " + timeFormat));
            }

            Hospital hospital = hospitalOptional.get();

            //Validating Logo And LetterHead

            if (logo != null && !logo.isEmpty()) {
                if (!isValidImageType(logo)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid hospitalLogo format. Only JPG, JPEG, or PNG files are allowed."));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Hospital logo is required."));
            }

            if (letterHead != null && !letterHead.isEmpty()) {
                if (!isValidImageType(letterHead)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid hospitalLetterHead format. Only JPG, JPEG, or PNG files are allowed."));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Hospital letterhead is required."));
            }

            //Validating Working Days Of Hospital

            List<String> validDays = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
            Set<String> validDaysSet = validDays.stream().map(String::toLowerCase).collect(Collectors.toSet());

            List<String> workingDaysList = Arrays.stream(setting.getHospitalWorkingDays().split(","))
                    .map(String::trim)
                    .filter(day -> !day.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            List<String> offDaysList = Arrays.stream(setting.getHospitalOffDays().split(","))
                    .map(String::trim)
                    .filter(day -> !day.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());


            Set<String> workingDaysSet = new HashSet<>(workingDaysList);
            Set<String> offDaysSet = new HashSet<>(offDaysList);

            Set<String> invalidWorkingDays = new HashSet<>(workingDaysSet);
            invalidWorkingDays.removeAll(validDaysSet);

            Set<String> invalidOffDays = new HashSet<>(offDaysSet);
            invalidOffDays.removeAll(validDaysSet);

            if (!invalidWorkingDays.isEmpty() || !invalidOffDays.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid days found. Allowed days are Monday to Sunday. Invalid entries: " +
                                String.join(", ", invalidWorkingDays) + " " + String.join(", ", invalidOffDays)));
            }

            Set<String> commonDays = new HashSet<>(workingDaysSet);
            commonDays.retainAll(offDaysSet);

            if (!commonDays.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "The following days cannot be both working and off days: " + String.join(", ", commonDays)));
            }

            if (workingDaysList.size() != workingDaysSet.size() || offDaysList.size() != offDaysSet.size()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Duplicate days found. Please remove duplicate days from your input."));
            }


            Optional<Settings> existingSettings = settingsService.findSettingsByHospital(hospital);

            Settings settings = existingSettings.orElse(new Settings());

            // Validating No Of Ambulance and Ambulance Charges

            String noOfAmbulancesStr = setting.getNoOfAmbulances();

            if (noOfAmbulancesStr == null || noOfAmbulancesStr.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Number of ambulances is required."));
            }

            noOfAmbulancesStr = noOfAmbulancesStr.trim();

            if (!noOfAmbulancesStr.matches("\\d+")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Number of ambulances must be a valid positive integer."));
            }

            // Validating No of Ambulance and Ambulance Charges

            Integer noOfAmbulances = Integer.valueOf(noOfAmbulancesStr);
            settings.setNoOfAmbulances(noOfAmbulancesStr);

            if (noOfAmbulances == 0) {

                settings.setAmbulanceCharges("0.0");

                if (setting.getAmbulanceCharges() != null &&
                        !setting.getAmbulanceCharges().trim().equals("0") &&
                        !setting.getAmbulanceCharges().trim().equals("0.0")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ambulance charges must be 0 when no ambulances are available."));
                }
            } else {
                String ambulanceChargesStr = setting.getAmbulanceCharges();

                if (ambulanceChargesStr == null || ambulanceChargesStr.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ambulance charges are required when ambulances are available."));
                }

                ambulanceChargesStr = ambulanceChargesStr.trim();

                if (!ambulanceChargesStr.matches("\\d+(\\.\\d+)?")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ambulance charges must be a valid positive number."));
                }

                Double ambulanceCharges = Double.parseDouble(ambulanceChargesStr);

                if (ambulanceCharges < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ambulance charges cannot be negative."));
                }

                settings.setAmbulanceCharges(ambulanceChargesStr);
            }

            // validate Ambulance contact number

            String ambulanceContactNumber = setting.getAmbulanceContactNumber();

            if (ambulanceContactNumber == null || ambulanceContactNumber.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Ambulance contact number is required."));
            }

            ambulanceContactNumber = ambulanceContactNumber.trim();

            if (!ambulanceContactNumber.matches("^\\d{10}$")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Ambulance contact number must be exactly 10 digits and only digits are allowed."));
            }

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

            if (existingSettings.isPresent()) {

                settings.setModifiedUser(currentUser);
                settings.setModifiedDate(new Date());
            } else {

                settings.setCreatedUser(currentUser);
                settings.setModifiedUser(currentUser);

                settings.setCreatedDate(new Date());
            }

            settingsService.saveSettings(settings, letterHead, logo);

            if (existingSettings.isPresent()) {
                return ResponseEntity.ok(settings);
            } else {
                return ResponseEntity.status(HttpStatus.CREATED).body(settings);
            }

        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error processing request: {}" + sw + "Logged User :" + currentUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while processing the request."));
        }
    }

   // __________ Get ALL Settings______________________________________________________________________________________________________________________________________________________________________

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/getSettings")
    public synchronized ResponseEntity<List<Settings>> getAllSettings() {
        try {
            List<Settings> settingsList = settingsService.getAllSettings();
            if (settingsList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return ResponseEntity.ok(settingsList);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching settings: {}" + sw + "Logged User :" + currentUser.getId());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // _________________Get Settings By ID_______________________________________________________________________________________________________________________________________________________________


    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}")
    public synchronized ResponseEntity<?> getSettingsById(@PathVariable long id) {
        try {
            Settings settings = settingsService.getSettingsById(id);
            if (settings == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Settings not found with ID: " + id));
            }
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching in get Setting by ID : {}" + sw + "Logged User :" + currentUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while fetching the settings: " + e.getMessage()));
        }
    }

    // _________________Get Logo and LetterHead Of Hospital_______________________________________________________________________________________________________________________________________________________________


    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/byType/{type}/{id}")
    public synchronized ResponseEntity<?> getImage(@PathVariable String type, @PathVariable long id) {
        try {
            Optional<byte[]> imageData;

            switch (type.toLowerCase()) {
                case "letterhead":
                    imageData = settingsService.getLetterHead(id);
                    break;
                case "logo":
                    imageData = settingsService.getLogo(id);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "Invalid type parameter. Use 'letterHead' or 'logo'."));
            }

            if (imageData.isPresent()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .body(imageData.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("error", type + " is not present for ID: " + id));
            }
        } catch (Exception e) {

            e.printStackTrace(pw);
            logger.error("Error fetching for hospital id : {}" + sw + "Logged User :" + currentUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "An error occurred while fetching the " + type + ": " + e.getMessage()));
        }
    }

    // _________Method to check image type_______________________________________________________________________________________________________________________________________________________________________

    private boolean isValidImageType(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return false;
            }

            String contentType = file.getContentType();

            return contentType != null &&
                    (contentType.equals("image/jpeg") ||
                            contentType.equals("image/png") ||
                            contentType.equals("image/jpg"));
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error Occurred in Checking image type : {}" + sw);
            return false;
        }


    }

    // ________Method to parse time________________________________________________________________________________________________________________________________________________________________________


    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            e.printStackTrace(pw);
            logger.error("Failed to parse time : {}" + sw + "Logged User :" + currentUser.getId());
            return null;
        }
    }
}