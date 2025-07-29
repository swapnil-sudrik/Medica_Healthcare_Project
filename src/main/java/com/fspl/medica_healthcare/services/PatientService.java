package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.PatientRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PatientService {

    private static final Logger logger = Logger.getLogger(PatientService.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserService userService;


    public List<Patient> getPatientsByStatus(int status) {
        try {
            List<Patient> patientList = patientRepository.findByStatus(status);
            if (patientList != null && !patientList.isEmpty())
                return patientList;
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Patient> getPatientsByCurrentStatus(int currentStatus) {
        try {
            List<Patient> patientList = patientRepository.findByCurrentStatus(currentStatus);
            if (patientList != null && !patientList.isEmpty())
                return patientList;
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Patient getPatientById(long id) {
        try {
            return patientRepository.findById(id).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Patient> getPatientBirthdayWithHospital(int birthdateMonth, int birthdateDay) {
        try {
            List<Patient> patientList = patientRepository.findPatientsByBirthdateWithHospital(birthdateMonth, birthdateDay);
            if (patientList != null && !patientList.isEmpty())
                return patientList;
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Patient> getPatientsByContactNumber(String contactNumber) {
        try {
            List<Patient> patientList = patientRepository.findByContactNumber(contactNumber); // fetch By Contact Number

            if (patientList != null && !patientList.isEmpty())
                return patientList;
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Patient> getPatientsByName(String name) {
        try {
            List<Patient> patientList = patientRepository.findByName(name);
            if (patientList != null && !patientList.isEmpty())
                return patientList;
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Patient> getAllPatientsByHospital(long hospital_id) {
        try {
            List<Patient> patientList = patientRepository.findAllPatientsByHospital(hospital_id);
            if (patientList != null && !patientList.isEmpty())
                return patientList;
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    //For appointment use
    public Patient findPatientByDetails(byte[] name, byte[] emailId, String contactNumber) {
        try {
            return patientRepository.findPatientByDetails(name, emailId, contactNumber);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Patient savePatient(Patient patient) {
        try {
            User loginUser = userService.getAuthenticateUser();
            patient.setHospital(loginUser.getHospital());
            patient.setCreatedUser(loginUser);
            patient.setCreatedDate(LocalDate.now());
            patient.setStatus(1);
            patient.setModifiedUser(loginUser);
            patient.setModifiedDate(LocalDate.now());
            return patientRepository.save(patient);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }
}