package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.enums.PaymentMode;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Deposit;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.AppointmentService;
import com.fspl.medica_healthcare.services.DepositService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/deposit")
public class DepositController {

    @Autowired
    private DepositService depositService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/addDeposit/{appointmentId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST')")
    public ResponseEntity<String> addDeposit(@PathVariable long appointmentId,
                                             @RequestParam double depositAmount,
                                             @RequestParam PaymentMode paymentMode){
        User loginUser = null;
        try{
            loginUser = userService.getAuthenticateUser();

            if(loginUser == null || loginUser.getHospital() == null){
                return ResponseEntity.badRequest().body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            if(paymentMode.equals(PaymentMode.INSURANCE)){
                return ResponseEntity.badRequest().body("paymentMode is not valid!!");
            }

            Appointment appointment = appointmentService.findAppointmentById(appointmentId);

            if(appointment == null || appointment.getHospitalizationInfo() == null){
                return ResponseEntity.badRequest().body("Appointment or HospitalizationInfo Not Found!!");
            }

            if(!appointment.getHospitalizationInfo().isHospitalized()){
                return ResponseEntity.badRequest().body("Patient is already discharged..");
            }

            Deposit deposit = new Deposit();

            deposit.setDepositAmount(depositAmount);
            deposit.setPaymentMode(paymentMode!=null? paymentMode : PaymentMode.UNDEFINED);
            deposit.setAppointment(appointment);
            deposit.setCreatedUser(loginUser);
            deposit.setModifiedUser(loginUser);
            deposit.setCreatedDate(LocalDate.now());
            deposit.setModifiedDate(LocalDate.now());

            if(!depositService.addDeposit(loginUser,deposit)){
                return ResponseEntity.badRequest().body("error while adding deposit");
            }
            return ResponseEntity.ok("Deposit Added successfully...");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("error wile adding deposit");
        }
    }
}
