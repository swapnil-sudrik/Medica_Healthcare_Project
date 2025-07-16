package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.exceptions.ResourceNotFoundException;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.Prescription;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.AppointmentRepository;
import com.fspl.medica_healthcare.repositories.PatientRepository;
import com.fspl.medica_healthcare.repositories.PrescriptionRepository;
import com.fspl.medica_healthcare.repositories.UserRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;


    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserService userService;

    private static final Logger logger = LogManager.getLogger(PrescriptionService.class);


//    ------------------------------------------------------------------------------------------------------


    public boolean saveOrUpdatePrescription(Prescription prescription) {
        try {
            prescriptionRepository.save(prescription);
            return true;
        } catch (Exception e) {
            logger.error("\n Error while saving or updating prescription: "+ ExceptionUtils.getStackTrace(e) +"Logging User"+prescription.getLoginUser().getId());
            return false;
        }
    }


//    ----------------------------------------------------------------------------------------------------------


    public Prescription getPrescriptionById(Long id, User loginUser) {

        try {
            Optional<Prescription> prescription = prescriptionRepository.findById(id);
            if (prescription.isEmpty()) {
                return null;
            }
            return prescription.get();
        } catch (Exception e) {
            logger.error("\n Error while fetching prescription: " + ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return null;
        }
    }


//    -------------------------------------------------------------------------------------------------------

    public List<Prescription> getAllPrescriptions(User loginUser) {
        try {
            List<Prescription> prescriptions = prescriptionRepository.findAll();

            if (prescriptions.isEmpty()) {
                return null;
            }

            return prescriptions;
        } catch (Exception e) {
            logger.error("\n Error while fetching prescriptions: " + ExceptionUtils.getStackTrace(e) +"Logging User :"+loginUser.getId());

            return null;
        }
    }


//    ----------------------------------------------------------------------------------------------------------


    public Prescription deletePrescription(Prescription prescription) {
        if (prescription == null) {
            return null;
        }
        try {
            prescriptionRepository.delete(prescription);
        } catch (Exception e) {
            logger.error("\n Error while deleting prescription: " + ExceptionUtils.getStackTrace(e) +"Logging User ;"+prescription.getLoginUser().getId());
        }
        return null;


    }


//    ----------------------------------------------------------------------------------------------------------


    public List<List<String>> findAllMedicine(User loginUser) {
        try {
            List<List<String>> medicines = prescriptionRepository.findAllMedicines();

            if (medicines == null || medicines.isEmpty()) {
                return null;
            }

            return medicines;
        } catch (Exception e) {
            logger.error("\n Error while fetching medicines: " + ExceptionUtils.getStackTrace(e) +"Logging User  :"+loginUser);
            return null;
        }
    }

//    ---------------------------------------------------------------------------------------------------

    public List<Prescription> findPrescriptionByPatientId(Long patientId,User loginUser) {
        try {
            return prescriptionRepository.findPrescriptionsByPatientId(patientId);
        } catch (Exception e) {
            logger.error("\n Error while fetching prescriptions for patientId " + patientId + ": " + ExceptionUtils.getStackTrace(e) +"Logging User  :"+loginUser.getId());
            return Collections.emptyList();
        }
    }


//----------------------------------------------------------------------------------------------------------------

    public boolean getPrescriptionByAppointmentId(long id,User loginUser) {
        try {
            List< Prescription> prescription= prescriptionRepository.findPrescriptionByAppointmentId(id);
            if(!prescription.isEmpty()) {
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
            logger.error("\n Error while saving or updating prescription: " +ExceptionUtils.getStackTrace(e) +"Logging User  :"+loginUser.getId());
            return false;
        }
    }
}