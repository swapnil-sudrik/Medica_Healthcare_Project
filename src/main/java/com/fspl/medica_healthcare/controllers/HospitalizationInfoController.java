package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.enums.RoomType;
import com.fspl.medica_healthcare.exceptions.RecordNotFoundException;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.repositories.RoomChargeRepository;
import com.fspl.medica_healthcare.services.AppointmentService;
import com.fspl.medica_healthcare.services.CatalogService;
import com.fspl.medica_healthcare.services.HospitalizationService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/admit/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> admission(
            @PathVariable Long appointmentId,
            @RequestParam("roomCategory") String roomCategory) {
        User loggedUser = null;

        try {
            loggedUser = userService.getAuthenticateUser();
            Appointment appointment = appointmentService.findAppointmentById(appointmentId);

            if (appointment == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "appointment not get"));
            }

            User loginUser = userService.getAuthenticateUser();

            if (appointment.getHospitalizationInfo() != null) {
                return ResponseEntity.status(409).body(Map.of("error", "Hospitalization info already exists"));
            }
//        RoomCharge roomCharge = roomChargeRepository.findByAppointment_AppointmentIdAndRoomType(appointmentId,roomType).orElseThrow(() -> new RoomChargeNofFoundException("Room Charge Not Found"));

//            RoomCharge roomCharge = roomChargeRepository.findByHospitalAndRoomType(appointment.getHospital(), roomType).orElseThrow(() -> new RecordNotFoundException("Room Charge Not Found"));

            List<Catalog> catalogByCategory = catalogService.findCatalogByCategory(appointment.getHospital().getId(), "ROOM");

            Catalog roomCatalog = catalogByCategory.stream().filter(catalog -> catalog.getName().equalsIgnoreCase(roomCategory)).findFirst().get();

            HospitalizationInfo hospitalizationInfo = new HospitalizationInfo();

            hospitalizationInfo.setIsHospitalized(true);
            hospitalizationInfo.setDateOfAdmission(LocalDate.now());
            hospitalizationInfo.setCatalog(roomCatalog);
            hospitalizationInfo.setDateOfDischarge(null);
            hospitalizationInfo.setAdditionalCharges(BigDecimal.ZERO);
            hospitalizationInfo.setNursingCharges(BigDecimal.ZERO);
            hospitalizationInfo.setTotalDaysAdmitted(1);
            hospitalizationInfo.setCreatedUser(loginUser);
            hospitalizationInfo.setModifiedUser(loginUser);
            hospitalizationInfo.setCreatedDate(LocalDate.now());
            hospitalizationInfo.setModifiedDate(LocalDate.now());
            HospitalizationInfo saved = hospitalizationService.createAdmission(hospitalizationInfo);

            // bug : hospitalizationId is null because after saving hospitalization return boolean
            appointment.setHospitalizationInfo(hospitalizationInfo);
            appointmentService.saveAppointment(appointment);

            if (Boolean.FALSE.equals(saved)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Failed to create admission. Please try again later."
                ));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "admission successfully created..."
            ));

        } catch (Exception e) {
            log.error("An unexpected error occurred while processing admission request for Hospitalization : 'Error' : "+ ExceptionUtils.getStackTrace(e)+ "Logged User Id:\n"+loggedUser.getId());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }


    @PostMapping("/discharge/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<?> discharge(@PathVariable Long appointmentId,
                                       @RequestParam("nursingCharges") BigDecimal nursingCharges,
                                       @RequestParam("additionalCharges") BigDecimal additionalCharges) {
        User loginUser=null;
        try {
            loginUser = userService.getAuthenticateUser();
            Appointment appointment = appointmentService.findAppointmentById(appointmentId);
            if (appointment == null) {
                return ResponseEntity.badRequest().body("Appointment not found with this id" + appointmentId);
            }

            HospitalizationInfo existing = appointment.getHospitalizationInfo();
            if (existing == null) {
                return ResponseEntity.badRequest().body("Hospitalization Info not found for this appointment id" + appointmentId);
            }


            BigDecimal tempNursingCharges = existing.getNursingCharges() != null ? existing.getNursingCharges() : BigDecimal.ZERO;
            BigDecimal tempAdditionalCharges = existing.getAdditionalCharges() != null ? existing.getAdditionalCharges() : BigDecimal.ZERO;

            existing.setModifiedDate(LocalDate.now());
            existing.setIsHospitalized(false);
            existing.setModifiedUser(loginUser);
            existing.setNursingCharges(tempNursingCharges.add(nursingCharges));
            existing.setAdditionalCharges(tempAdditionalCharges.add(additionalCharges));
            existing.setDateOfDischarge(LocalDate.now());
            Integer totalAdmittedDays = this.setTotalDaysAdmitted(existing.getDateOfAdmission(), existing.getDateOfDischarge());
            existing.setTotalDaysAdmitted(totalAdmittedDays);


            HospitalizationInfo saved = hospitalizationService.createAdmission(existing);

            if (saved == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Failed to update discharge details. Please try again later."
                ));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Discharge successfully updated..."
            ));
        } catch (Exception e) {
            log.error("An unexpected error occurred while processing discharge request for Hospitalization : '\nError' : \n"+ ExceptionUtils.getStackTrace(e) + "Logged User Id:\n "+loginUser.getId());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "An unexpected error occurred",
                            "details", e.getMessage()
                    ));
        }
    }



    // Runs every day at midnight
//    @Scheduled(cron = "0 0 0 * * ?")
//    @Transactional
    @PostConstruct
    public void updateTotalDaysAdmitted() {
        Thread thread= new Thread(()->{
            try {
                List<HospitalizationInfo> activeHospitalizations = hospitalizationService.findByDateOfDischargeIsNull();

                if (activeHospitalizations == null) {
                    return;
                }
                else if (activeHospitalizations.isEmpty()) {
                    return;
                }
                else {
                    for (HospitalizationInfo hospitalization : activeHospitalizations) {
                        if (hospitalization.getIsHospitalized()){
                            hospitalization.setTotalDaysAdmitted(this.setTotalDaysAdmitted(hospitalization.getDateOfAdmission(), LocalDate.now()));
                        }
                    }
                    boolean isAllSaved =  hospitalizationService.saveAllHospitalization(activeHospitalizations);
                    if (isAllSaved){
                        return;
                    }
                }
//                24 * 60 * 60 * 1000:
//                24 → 24 hours
//                60 → 60 minutes in an hour
//                60 → 60 seconds in a minute
//                1000 → 1000 milliseconds in a second
//                Total: 24 × 60 × 60 × 1000 = 86,400,000 milliseconds (which equals 24 hours).
                Thread.sleep(24*60*60*1000);
            } catch (Exception e) {
                log.error("An error occurred while updating total days admitted information: "+ ExceptionUtils.getStackTrace(e));
                e.printStackTrace();
            }
        });
        thread.start();
    }


    public Integer setTotalDaysAdmitted(LocalDate dateOfAdmission, LocalDate dateOfDischarge) {
        try {
//            if (dateOfDischarge == null) {
//                return Math.max(1, (int) ChronoUnit.DAYS.between(dateOfAdmission, LocalDate.now()) + 1);
//            }
            long daysBetween = ChronoUnit.DAYS.between(dateOfAdmission, dateOfDischarge);
            return Math.max(1, (int) daysBetween + 1);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An error occurred while updating total days admitted information: "+ ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

}
