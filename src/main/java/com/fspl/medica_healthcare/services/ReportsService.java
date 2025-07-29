package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.Reports;
import com.fspl.medica_healthcare.repositories.ReportsRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportsService {

    private static final Logger logger = Logger.getLogger(ReportsService.class);

    @Autowired
    private ReportsRepository reportRepository;

    @Autowired
    private UserService userService;

    //=========================================== SAVE A LIST OF MEDICAL REPORTS ========================================================//
    public boolean saveAllReports(List<Reports> reports) {
        try {
            List<Reports> reportList = reportRepository.saveAll(reports); // Save all reports to the database
            return !reportList.isEmpty(); // Return true if reports are saved successfully
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID: " + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    //================================================= FETCH REPORTS BY PATIENT ID =========================================================//
    public List<Reports> findReportsByPatientId(long id) {
        try {
            List<Reports> reportList = reportRepository.findByPatientId(id); // Retrieve reports for the given patient ID

            return (reportList != null && !reportList.isEmpty()) ? reportList : null; // Return the list of reports if found
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID: " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    //================================================ SAVE A SINGLE MEDICAL REPORT =========================================================//
    public boolean saveReport(Reports report) {
        try {
            Reports savedReport = reportRepository.save(report); // Save the report to the database
            return savedReport.getId() > 0; // Return true if the report is saved successfully
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID: " + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    //================================================ GET REPORT BY REPORT ID ==========================================================//
    public Reports getReportByReportId(long id) {
        try {
            return reportRepository.findById(id).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + "Log-in User Id : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }
}