package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.Reports;
import com.fspl.medica_healthcare.repositories.ReportsRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReportsService {

    private static final Logger logger = Logger.getLogger(ReportsService.class);

    @Autowired
    private ReportsRepository reportRepository;

    @Autowired
    private UserService userService;

    public boolean saveAllReports(List<Reports> reports) {
        try {
            List<Reports> reportList = reportRepository.saveAll(reports);
            return !reportList.isEmpty();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    public List<Reports> findReportsByPatientId(long id) {
        try {
            List<Reports> reportList = reportRepository.findByPatientId(id);
            if (reportList != null && !reportList.isEmpty()) {
                return reportList;
            } else
                return null;
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public boolean saveReport(Reports report) {
        try {
            Reports savedReport = reportRepository.save(report);
            return savedReport.getId() > 0;
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    public Reports findReportByIdAndPatientId(long reportId, long patientId) {
        try {
            Optional<Reports> report = reportRepository.findByIdAndPatientId(reportId, patientId);
            return report.orElse(null);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return null;
        }
    }
}