package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.controllers.CatalogController;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.repositories.HospitalRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;


@Service
public class HospitalService {
    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserService userService;

    private static final Logger log = Logger.getLogger(CatalogController.class);

    public List<Hospital> getAllHospitals() {
        try {
            return hospitalRepository.findAll();

        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllHospitals" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Hospital getHospitalById(long id) {
        try {
            return hospitalRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getHospitalById" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Hospital saveHospital(Hospital hospital) {
        try {
            return hospitalRepository.save(hospital);
        } catch (Exception e) {
            log.error("An unexpected error occurred while saveHospital" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Hospital> findHospitalByName(String name) {
        try {
            return hospitalRepository.findByNameContainingIgnoreCase(name);
        } catch (Exception e) {
            log.error("An unexpected error occurred while findHospitalByName" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Hospital> findDeactivatedHospitals(int status) {
        try {
            return hospitalRepository.findDeactiveHospitals(status);
        } catch (Exception e) {
            log.error("An unexpected error occurred while findDeactivatedHospitals" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Hospital> findActiveHospitals(int status) {
        try {
            return hospitalRepository.findActiveHospitals(status);
        } catch (Exception e) {
            log.error("An unexpected error occurred while findActiveHospitals" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public boolean existsByEmailId(String emailId) {
        try {
            return hospitalRepository.existsByEmailId(emailId);
        } catch (Exception e) {
            log.error("An unexpected error occurred while existsByEmailId" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    public boolean existsByName(String name) {
        try {
            return hospitalRepository.existsByName(name);
        } catch (Exception e) {
            log.error("An unexpected error occurred while existsByName" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return false;
        }
    }
}