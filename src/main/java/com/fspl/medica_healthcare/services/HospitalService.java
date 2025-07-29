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

    static int status;

    public List<Hospital> getAllHospitals() {
        try {
            List<Hospital> hospitals = hospitalRepository.findAll();
            return hospitals;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getAllHospitals" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Hospital getHospitalById(long id) {
        try {
            Hospital hospital = hospitalRepository.findById(id).orElse(null);
            return hospital;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getHospitalById" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Boolean saveHospital(Hospital hospital) {
        try {
            hospitalRepository.save(hospital);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while saveHospital" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    public List<Hospital> findHospitalByName(String name) {
        try {
            List<Hospital>hospitals = hospitalRepository.findByNameContainingIgnoreCase(name);
            return hospitals;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findHospitalByName" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Hospital> findDeactivatedHospitals() {
        status = 0;
        try {
            List<Hospital> hospitals = hospitalRepository.findDeactiveHospitals(status);
            return hospitals;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findDeactivatedHospitals" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Hospital> findActiveHospitals() {
        status = 1;
        try {
            List<Hospital> hospitals = hospitalRepository.findActiveHospitals(status);
            return hospitals;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findActiveHospitals" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public boolean existsByEmailId(String emailId) {
        try {
            boolean exists = hospitalRepository.existsByEmailId(emailId);
            return exists;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while existsByEmailId" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    public Hospital getBranchesByHospitalId(long id) {
        try {
            Hospital hospitals = hospitalRepository.getBranchesByHospitalId(id);
            return hospitals;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while fetching hospital branches" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

}