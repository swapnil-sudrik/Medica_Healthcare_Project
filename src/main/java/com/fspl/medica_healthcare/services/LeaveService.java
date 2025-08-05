package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.controllers.PrescriptionController;
import com.fspl.medica_healthcare.models.Leaves;
import com.fspl.medica_healthcare.models.Prescription;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.LeaveRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class LeaveService {
    @Autowired
    private LeaveRepository leaveRepository;
    private static final Logger logger = Logger.getLogger(LeaveService.class);

    public boolean saveOrUpdateLeave(List<Leaves> leavesList) {
        try {
            List<Leaves> savedLeaves = leaveRepository.saveAll(leavesList);
            return savedLeaves.size() == leavesList.size();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to save leave entries", e);
            return false;
        }
    }


    public  List<Leaves> getLeaves(LocalDate date) {
        try {
            List<Leaves> leaves= leaveRepository.findAllByDate(date);
            return leaves;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error fetching leaves for date: " + date, e);
            return null;
        }
    }
    public List<Leaves> getLeavesInRange(LocalDate fromDate, LocalDate toDate) {
        return leaveRepository.findLeavesInRange(fromDate, toDate);
    }

    public boolean isDoctorOnLeave(long id, LocalDate appointmentDate){
        try{
            List<Leaves> leavesList = leaveRepository.findDoctorLeaveByDate(id,appointmentDate);
            return !leavesList.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
