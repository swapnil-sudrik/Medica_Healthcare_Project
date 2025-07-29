package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.HospitalExtraCharge;
import com.fspl.medica_healthcare.models.Settings;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.HospitalExtraChargeRepository;
import com.fspl.medica_healthcare.repositories.HospitalRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class HospitalExtraChargeService {

    private static final Logger logger = Logger.getLogger(HospitalExtraChargeService.class);

    @Autowired
    private HospitalExtraChargeRepository hospitalExtraChargeRepository;

    @Autowired
    private HospitalRepository hospitalRepository; // Assuming HospitalRepository exists

    // Save a new charge
    public void saveCharge(long hospitalId, HospitalExtraCharge charge) {
        try {
            Hospital hospital = hospitalRepository.findById(hospitalId)
                    .orElseThrow(() -> new RuntimeException("Hospital not found! " + hospitalId));
            charge.setHospital(hospital);
            hospitalExtraChargeRepository.save(charge);
        } catch (Exception e) {
            logger.error("Error saving charge for Hospital ID " + hospitalId + ": " + e.getMessage(), e);
        }

    }

    // Get total charges by year
    public double getTotalChargesByYear(long hospitalId, int year) {
        try {
            return hospitalExtraChargeRepository.calculateTotalChargesForHospitalByYear(hospitalId, year);
        } catch (Exception e) {
            logger.error("Error retrieving total charges for Hospital ID " + hospitalId + " in year " + year + ": " + e.getMessage(), e);
            return 0.0;
        }
    }

    // Calculate total extra charges for a specific hospital and month
    public Double calculateTotalChargesForHospital(long hospitalId, int year, int month) {
        try {
            Hospital hospital = hospitalRepository.findById(hospitalId)
                    .orElseThrow(() -> new RuntimeException("Hospital not found!"));
            return hospitalExtraChargeRepository.calculateTotalChargesForHospital(
                    hospital,
                    LocalDate.of(year, month, 1),
                    LocalDate.of(year, month, YearMonth.of(year, month).lengthOfMonth())
            );
        } catch (Exception e) {
            logger.error("Error calculating total charges for Hospital ID " + hospitalId +
                    " for Year: " + year + ", Month: " + month + " - " + e.getMessage(), e);
            return 0.0;
        }
    }

    // Update total extra charges for a specific hospital and month
    public void updateCharge(long chargeId, HospitalExtraCharge updatedCharge, long userId) {
        try {
            HospitalExtraCharge existingCharge = hospitalExtraChargeRepository.findById(chargeId)
                    .orElseThrow(() -> new RuntimeException("Charge not found with ID: " + chargeId));

            existingCharge.setChargeType(updatedCharge.getChargeType());
            existingCharge.setAmount(updatedCharge.getAmount());
            existingCharge.setModifyPaymentDate(LocalDate.now());

            User modifiedUser = new User();
            modifiedUser.setId(userId);
            existingCharge.setModifiedUser(modifiedUser);

            hospitalExtraChargeRepository.save(existingCharge);
        } catch (Exception e) {
            logger.error("Error updating charge with ID " + chargeId + ": " + e.getMessage(), e);
            throw new RuntimeException("Error updating charge.");
        }
    }

    public boolean existsById(long chargeId) {
        return hospitalExtraChargeRepository.existsById(chargeId);
    }




}