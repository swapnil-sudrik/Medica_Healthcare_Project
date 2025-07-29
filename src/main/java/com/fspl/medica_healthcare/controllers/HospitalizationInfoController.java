package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.enums.PaymentMode;
import com.fspl.medica_healthcare.enums.PolicyStatus;
import com.fspl.medica_healthcare.enums.RoomType;
import com.fspl.medica_healthcare.exceptions.RecordNotFoundException;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.repositories.RoomChargeRepository;
import com.fspl.medica_healthcare.services.AppointmentService;
import com.fspl.medica_healthcare.services.CatalogService;
import com.fspl.medica_healthcare.services.HospitalizationService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/hospitalization")
public class HospitalizationInfoController {

    @Autowired
    private HospitalizationService hospitalizationService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private RoomChargeRepository roomChargeRepository;

    private static final Logger log = LogManager.getLogger(HospitalizationInfoController.class);

    @PostMapping("/admitPatient/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    @Transactional
    public ResponseEntity<?> admitPatient(
            @PathVariable Long appointmentId,
            @RequestParam String roomCategory,
            @RequestParam(required = false, defaultValue = "0.0") double depositAmount,
            @RequestParam(required = false, defaultValue = "UNDEFINED") PaymentMode paymentMode) {

        User loginUser = null;

        try {
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            Appointment appointment = appointmentService.findAppointmentById(appointmentId);

            if (appointment == null || appointment.getHospital() == null) {
                return ResponseEntity.badRequest().body("Appointment or Hospital Not Found!!");
            }

            if (appointment.getHospitalizationInfo() != null) {
                return ResponseEntity.badRequest().body("Hospitalization info already exists");
            }
//        RoomCharge roomCharge = roomChargeRepository.findByAppointment_AppointmentIdAndRoomType(appointmentId,roomType).orElseThrow(() -> new RoomChargeNofFoundException("Room Charge Not Found"));

//            RoomCharge roomCharge = roomChargeRepository.findByHospitalAndRoomType(appointment.getHospital(), roomType).orElseThrow(() -> new RecordNotFoundException("Room Charge Not Found"));

            List<Catalog> catalogByCategory = catalogService.findCatalogByCategory(appointment.getHospital().getId(), "ROOM");

            if (catalogByCategory == null || catalogByCategory.isEmpty()) {
                return ResponseEntity.badRequest().body("Catalog Not Found!!");
            }

            Catalog roomCatalog = catalogByCategory.stream()
                    .filter(catalog -> catalog != null && catalog.getName() != null && catalog.getName().equalsIgnoreCase(roomCategory))
                    .findFirst()
                    .orElse(null);

            if (roomCatalog == null) {
                return ResponseEntity.badRequest().body("Catalog Not Matched with existing one!!");
            }

            HospitalizationInfo hospitalizationInfo = new HospitalizationInfo();

            hospitalizationInfo.setHospitalized(true);
            hospitalizationInfo.setDateOfAdmission(LocalDate.now());
            hospitalizationInfo.setCatalog(roomCatalog);
            hospitalizationInfo.setDateOfDischarge(null);
            hospitalizationInfo.setAdditionalCharges(0.0);
            hospitalizationInfo.setCanteenCharges(0.0);
            hospitalizationInfo.setNursingCharges(0.0);
            hospitalizationInfo.setTotalDaysAdmitted(1);
            hospitalizationInfo.setCreatedUser(loginUser);
            hospitalizationInfo.setModifiedUser(loginUser);
            hospitalizationInfo.setCreatedDate(LocalDate.now());
            hospitalizationInfo.setModifiedDate(LocalDate.now());
            HospitalizationInfo savedHospitalization = hospitalizationService.saveHospitalization(hospitalizationInfo);

            if (savedHospitalization == null) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return ResponseEntity.badRequest().body("Failed to Admit Patient!! Try Again..");
            }

            appointment.setHospitalizationInfo(hospitalizationInfo);
//            boolean savedAppointment = appointmentService.saveAppointment(appointment);

            if (!appointmentService.saveAppointment(appointment)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return ResponseEntity.badRequest().body("Failed to Admit Patient!! Hospitalization not set to appointment!! Try Again..");
            }

            if(depositAmount > 0) {
                DepositController depositController = new DepositController();
                depositController.addDeposit(appointmentId, depositAmount, paymentMode != null ? paymentMode : PaymentMode.UNDEFINED);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("admission successfully created...");

        } catch (Exception e) {
            log.error("An unexpected error occurred while processing admission request for Hospitalization : 'Error' : " + ExceptionUtils.getStackTrace(e) + "Logged User Id:\n" + loginUser.getId());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }

    @PostMapping("/updateHospitalization/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    @Transactional
    public ResponseEntity<?> updateHospitalizationRecord(@PathVariable long appointmentId,
                                                         @RequestParam(required = false, defaultValue = "0.0") double nursingCharges,
                                                         @RequestParam(required = false, defaultValue = "0.0") double additionalCharges,
                                                         @RequestParam(required = false, defaultValue = "0.0") double canteenCharges) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            if (loginUser == null || loginUser.getHospital() == null) {
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            if(nursingCharges == 0 && canteenCharges == 0 && additionalCharges == 0){
                return ResponseEntity.badRequest().body("At least one charges should be provide..");
            }

            if(nursingCharges <0 && canteenCharges < 0 && additionalCharges < 0){
                return ResponseEntity.badRequest().body("Amount should be positive");
            }

            Appointment appointment = appointmentService.findAppointmentById(appointmentId);

            if (appointment == null || appointment.getHospital() == null || appointment.getHospitalizationInfo() == null) {
                return ResponseEntity.badRequest().body("Appointment or Hospital or HospitalizationInfo Not Found!!");
            }

            if (!appointment.getHospitalizationInfo().isHospitalized()) {
                return ResponseEntity.badRequest().body("Patient Is not Hospitalized!!..");
            }

            HospitalizationInfo hospitalizationInfo = hospitalizationService.getHospitalizationInfoById(appointment.getHospitalizationInfo().getId());

            if (hospitalizationInfo == null) {
                return ResponseEntity.badRequest().body("Hospitalization Record Not Found!!..");
            }

            hospitalizationInfo.setCanteenCharges(hospitalizationInfo.getCanteenCharges() + canteenCharges);
            hospitalizationInfo.setNursingCharges(hospitalizationInfo.getNursingCharges() + nursingCharges);
            hospitalizationInfo.setAdditionalCharges(hospitalizationInfo.getAdditionalCharges() + additionalCharges);

            HospitalizationInfo savedHospitalizationInfo = hospitalizationService.saveHospitalization(hospitalizationInfo);

            if (savedHospitalizationInfo == null) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return ResponseEntity.badRequest().body("Failed to update Patient Hospitalization record!! Try Again..");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body("HospitalizationInfo successfully updated...");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error While updating hospitalization record.");
        }
    }


    @PostMapping("/discharge/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> discharge(@PathVariable Long appointmentId) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            Appointment appointment = appointmentService.findAppointmentById(appointmentId);

            if (appointment == null || appointment.getHospital() == null || appointment.getHospitalizationInfo() == null) {
                return ResponseEntity.badRequest().body("Appointment or Hospital or HospitalizationInfo Not Found!!");
            }

            if (!appointment.getHospitalizationInfo().isHospitalized()) {
                return ResponseEntity.badRequest().body("Patient has already been discharged!!..");
            }

            HospitalizationInfo hospitalizationInfo = appointment.getHospitalizationInfo();

            hospitalizationInfo.setHospitalized(false);
            hospitalizationInfo.setDateOfDischarge(LocalDate.now());
            hospitalizationInfo.setModifiedDate(LocalDate.now());
            hospitalizationInfo.setModifiedUser(loginUser);

            int totalAdmittedDays = this.setTotalDaysAdmitted(hospitalizationInfo.getDateOfAdmission(), hospitalizationInfo.getDateOfDischarge());

            if (totalAdmittedDays == 0) {
                return ResponseEntity.badRequest().body("Failed to calculate the total admitted days. Please try again.");
            }

            hospitalizationInfo.setTotalDaysAdmitted(totalAdmittedDays);

            HospitalizationInfo saved = hospitalizationService.saveHospitalization(hospitalizationInfo);

            if (saved == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update discharge details. Please try again later.");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body("Discharge details updated successfully...");

        } catch (Exception e) {
            log.error("An unexpected error occurred while processing discharge request for Hospitalization : '\nError' : \n" + ExceptionUtils.getStackTrace(e) + "Logged User Id:\n " + loginUser.getId());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }


//    @PostMapping("/discharge/{appointmentId}")
//    @PreAuthorize("hasAuthority('RECEPTIONIST')")
//    public ResponseEntity<?> discharge(@PathVariable Long appointmentId,
//                                       @RequestParam("nursingCharges") double nursingCharges,
//                                       @RequestParam("additionalCharges") double additionalCharges) {
//        User loginUser = null;
//        try {
//            loginUser = userService.getAuthenticateUser();
//            Appointment appointment = appointmentService.findAppointmentById(appointmentId);
//            if (appointment == null) {
//                return ResponseEntity.badRequest().body("Appointment not found with this id" + appointmentId);
//            }
//
//            HospitalizationInfo existing = appointment.getHospitalizationInfo();
//            if (existing == null) {
//                return ResponseEntity.badRequest().body("Hospitalization Info not found for this appointment id" + appointmentId);
//            }
//
//
////            double tempNursingCharges = existing.getNursingCharges() != null ? existing.getNursingCharges() : BigDecimal.ZERO;
////            double tempAdditionalCharges = existing.getAdditionalCharges() != null ? existing.getAdditionalCharges() : BigDecimal.ZERO;
//
//            double tempNursingCharges = existing.getNursingCharges();
//            double tempAdditionalCharges = existing.getAdditionalCharges();
//
//            existing.setModifiedDate(LocalDate.now());

    /// /            existing.setIsHospitalized(false);
//            existing.setHospitalized(false);
//            existing.setModifiedUser(loginUser);
//            existing.setNursingCharges(tempNursingCharges + nursingCharges);
//            existing.setAdditionalCharges(tempAdditionalCharges + additionalCharges);
//            existing.setDateOfDischarge(LocalDate.now());
//            Integer totalAdmittedDays = this.setTotalDaysAdmitted(existing.getDateOfAdmission(), existing.getDateOfDischarge());
//            existing.setTotalDaysAdmitted(totalAdmittedDays);
//
//
//            HospitalizationInfo saved = hospitalizationService.saveHospitalization(existing);
//
//            if (saved == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
//                        "error", "Failed to update discharge details. Please try again later."
//                ));
//            }
//            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
//                    "message", "Discharge successfully updated..."
//            ));
//        } catch (Exception e) {
//            log.error("An unexpected error occurred while processing discharge request for Hospitalization : '\nError' : \n" + ExceptionUtils.getStackTrace(e) + "Logged User Id:\n " + loginUser.getId());
//            e.printStackTrace();
//            return ResponseEntity.badRequest()
//                    .body(Map.of(
//                            "error", "An unexpected error occurred",
//                            "details", e.getMessage()
//                    ));
//        }
//    }


    // Runs every day at midnight
//    @Scheduled(cron = "0 0 0 * * ?")
//    @Transactional
    @PostConstruct
    public void updateTotalDaysAdmitted() {
        Thread thread = new Thread(() -> {
            try {
                List<HospitalizationInfo> activeHospitalizations = hospitalizationService.findByDateOfDischargeIsNull();

                if (activeHospitalizations == null) {
                    return;
                } else if (activeHospitalizations.isEmpty()) {
                    return;
                } else {
                    for (HospitalizationInfo hospitalization : activeHospitalizations) {
                        if (hospitalization.isHospitalized()) {
                            hospitalization.setTotalDaysAdmitted(this.setTotalDaysAdmitted(hospitalization.getDateOfAdmission(), LocalDate.now()));
                        }
                    }
                    boolean isAllSaved = hospitalizationService.saveAllHospitalization(activeHospitalizations);
                    if (isAllSaved) {
                        return;
                    }
                }
//                24 * 60 * 60 * 1000:
//                24 → 24 hours
//                60 → 60 minutes in an hour
//                60 → 60 seconds in a minute
//                1000 → 1000 milliseconds in a second
//                Total: 24 × 60 × 60 × 1000 = 86,400,000 milliseconds (which equals 24 hours).
                Thread.sleep(24 * 60 * 60 * 1000);
            } catch (Exception e) {
                log.error("An error occurred while updating total days admitted information: " + ExceptionUtils.getStackTrace(e));
                e.printStackTrace();
            }
        });
        thread.start();
    }


    public int setTotalDaysAdmitted(LocalDate dateOfAdmission, LocalDate dateOfDischarge) {
        try {
//            if (dateOfDischarge == null) {
//                return Math.max(1, (int) ChronoUnit.DAYS.between(dateOfAdmission, LocalDate.now()) + 1);
//            }
            long daysBetween = ChronoUnit.DAYS.between(dateOfAdmission, dateOfDischarge);
            return Math.max(1, (int) daysBetween + 1);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while updating total days admitted information: " + ExceptionUtils.getStackTrace(e));
            return 0;
        }
    }

}
