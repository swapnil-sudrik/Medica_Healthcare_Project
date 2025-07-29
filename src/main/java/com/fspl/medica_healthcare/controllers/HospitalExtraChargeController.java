package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.HospitalExtraCharge;
import com.fspl.medica_healthcare.models.Settings;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.HospitalExtraChargeService;
import com.fspl.medica_healthcare.services.HospitalService;
import com.fspl.medica_healthcare.services.UserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/hospitalCharges")
public class HospitalExtraChargeController {

    @Autowired
    private HospitalExtraChargeService hospitalExtraChargeService;

    @Autowired
    private UserService userService;

    @Autowired
    private HospitalService hospitalService;


    User loginUser = null;

    private static final Logger logger = Logger.getLogger(HospitalExtraChargeController.class);

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/add/{hospitalId}")
    public ResponseEntity<String> addCharge(@PathVariable long hospitalId, @RequestBody HospitalExtraCharge charge) {
        try {
            // Get authenticated user
            User loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("Unauthorized: No authenticated user found", HttpStatus.UNAUTHORIZED);
            }

            // Check if hospital exists
            Optional<Hospital> hospitalOptional = Optional.ofNullable(hospitalService.getHospitalById(hospitalId));
            if (!hospitalOptional.isPresent()) {
                return new ResponseEntity<>("Hospital with ID " + hospitalId + " not found", HttpStatus.NOT_FOUND);
            }

            if(charge.getChargeType() == null){
                return  new ResponseEntity<>("charge type cant be null",HttpStatus.BAD_REQUEST);
            }
            // Set user and timestamps
            charge.setCreatedUser(loginUser);
            charge.setModifiedUser(loginUser);
            charge.setPaymentDate(LocalDate.now());
            charge.setModifyPaymentDate(LocalDate.now());

            // Save charge
            hospitalExtraChargeService.saveCharge(hospitalId, charge);
            return ResponseEntity.ok("Charge added successfully!");

        } catch (RuntimeException e) {
            logger.error("Error adding charge: " + e.getMessage(), e);
            return new ResponseEntity<>("Error adding charge: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Get total extra charges for a specific hospital and Year
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/yearly/{hospitalId}/{year}")
    public ResponseEntity<?> getHospitalYearlyExpenses(@PathVariable long hospitalId, @PathVariable int year) {
        try {

            // Get current year
            int currentYear = LocalDate.now().getYear();

            // Check if the requested year is in the future
            if (year > currentYear) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Future year not allowed. Please provide a year up to " + currentYear);
            }

            //Check if hospital exists (handling case where HospitalService does not return Optional)
            Hospital hospital = hospitalService.getHospitalById(hospitalId);
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Hospital with ID " + hospitalId + " not found");
            }

            // Calculate total yearly expenses
            double totalYearlyExpenses = hospitalExtraChargeService.getTotalChargesByYear(hospitalId, year);
            return ResponseEntity.ok(totalYearlyExpenses);
        } catch (Exception e) {
            logger.error("Error occurred while fetching yearly expenses for hospital ID " + hospitalId +
                    " and year " + year + ": " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while processing the request.");
        }

    }

    // Get total extra charges for a specific hospital and month
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("monthly/{hospitalId}/{year}/{month}")
    public ResponseEntity<?> getTotalChargesForHospital(
            @PathVariable long hospitalId,
            @PathVariable int year,
            @PathVariable int month) {
        try{

            // Get current year and month
            int currentYear = LocalDate.now().getYear();
            int currentMonth = LocalDate.now().getMonthValue();

            // Validate year (future years not allowed)
            if (year > currentYear) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Future year not allowed. Please provide a year up to " + currentYear);
            }

            // Validate month (must be between 1 and 12)
            if (month < 1 || month > 12) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid month: " + month + ". Please provide a value between 1 and 12.");
            }

            // If year is current year, the month should not be in the future
            if (year == currentYear && month > currentMonth) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Future month not allowed. Please provide a month up to " + currentMonth + " for the year " + currentYear);
            }

            //Check if hospital exists (handling case where HospitalService does not return Optional)
            Hospital hospital = hospitalService.getHospitalById(hospitalId);
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Hospital with ID " + hospitalId + " not found");
            }

            // Calculate total monthly expenses
            Double hospitalExtraCharge= hospitalExtraChargeService.calculateTotalChargesForHospital(hospitalId, year, month);
            return new ResponseEntity<>(hospitalExtraCharge,HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error occurred while fetching yearly expenses for hospital ID " + hospitalId +
                    " and year " + year + ": " + e.getMessage(), e);
            return new ResponseEntity("Bad request" ,HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/update/{chargeId}")
    public ResponseEntity<String> updateCharge(
            @PathVariable long chargeId,
            @RequestBody HospitalExtraCharge updatedCharge) {
        try {
            // Get authenticated user
            User loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("Unauthorized: No authenticated user found", HttpStatus.UNAUTHORIZED);
            }

            // Call service method
            hospitalExtraChargeService.updateCharge(chargeId, updatedCharge, loginUser.getId());

            return ResponseEntity.ok("Charge updated successfully!");
        } catch (Exception e) {
            logger.error("Error updating charge: " + e.getMessage(), e);
            return new ResponseEntity<>("Error updating charge: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



}
