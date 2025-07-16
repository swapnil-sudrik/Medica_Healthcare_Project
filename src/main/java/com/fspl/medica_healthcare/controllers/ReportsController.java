package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.Reports;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.PatientService;
import com.fspl.medica_healthcare.services.ReportsService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private static final Logger logger = Logger.getLogger(ReportsController.class);

    @Autowired
    private ReportsService reportsService;

    @Autowired
    private UserService userService;

    @Autowired
    private PatientService patientService;

    @PostMapping("/addReport/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<String> saveReport(@PathVariable long id,
                                                          @RequestParam("medicalReports") MultipartFile[] medicalReports,
                                                          @RequestParam(value = "reportNames", required = false) String reportNames) {
        User logInUser = null;
        try {
            logInUser = userService.getAuthenticateUser();
            Patient patient = patientService.getPatientById(id);
            if (patient == null) {
                return ResponseEntity.badRequest().body("Patient not found with ID : " + id);
            }
            Set<String> allowedReportTypes = Set.of("application/pdf", "image/jpeg", "image/jpg", "image/png");

            Queue<String> reportNamesQueue = new LinkedList<>();
            if (reportNames != null && !reportNames.trim().isEmpty()) {
                reportNamesQueue.addAll(Arrays.asList(reportNames.split(",")));
            }

            List<Reports> allReports = new ArrayList<>();

            for (MultipartFile file : medicalReports) {
                if (file.isEmpty()) {
                    return ResponseEntity.badRequest().body("Please provide a report.");
                }
                if ("application/zip".equalsIgnoreCase(file.getContentType())) {
                    try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
                        ZipEntry entry;
                        while ((entry = zipInputStream.getNextEntry()) != null) {

                            if (!entry.isDirectory()) {
                                String extractedFileName = entry.getName();
                                int lastDotIndex = extractedFileName.lastIndexOf(".");

                                String reportName = (lastDotIndex == -1) ? extractedFileName : extractedFileName.substring(0, lastDotIndex);

                                String extension = (lastDotIndex == -1) ? "" : extractedFileName.substring(lastDotIndex + 1).toLowerCase();

                                if (!extension.equalsIgnoreCase("pdf") && !extension.equalsIgnoreCase("jpeg") && !extension.equalsIgnoreCase("png") && !extension.equalsIgnoreCase("jpg")) {
                                    return ResponseEntity.badRequest().body("ZIP file contains unsupported file type!!\nPlease provide PDF, JPEG, JPG OR PNG file.");
                                }
                                String assignedReportName = !reportNamesQueue.isEmpty() ? reportNamesQueue.poll().trim() : reportName;

                                byte[] fileBytes = zipInputStream.readAllBytes();

                                Reports report = new Reports();
                                report.setReport(fileBytes);
                                report.setName(assignedReportName);
                                report.setType(file.getContentType());
                                report.setCreatedUser(logInUser);
                                report.setCreatedDate(LocalDate.now());
                                report.setModifiedUser(logInUser);
                                report.setModifiedDate(LocalDate.now());
                                report.setPatient(patient);

                                allReports.add(report);
                            }
                        }
                    }
                } else if (allowedReportTypes.contains(file.getContentType())) {
                    String originalFileName = file.getOriginalFilename();
                    if (originalFileName == null) {
                        return ResponseEntity.badRequest().body("Report is not having name!!");
                    }
                    int lastDotIndex = originalFileName.lastIndexOf(".");

                    String cleanFileName = (lastDotIndex == -1) ? originalFileName : originalFileName.substring(0, lastDotIndex);

                    String assignedReportName = !reportNamesQueue.isEmpty() ? reportNamesQueue.poll().trim() : cleanFileName;

                    Reports report = new Reports();
                    report.setReport(file.getBytes());
                    report.setName(assignedReportName);
                    report.setType(file.getContentType());
                    report.setCreatedUser(logInUser);
                    report.setCreatedDate(LocalDate.now());
                    report.setModifiedUser(logInUser);
                    report.setModifiedDate(LocalDate.now());
                    report.setPatient(patient);

                    allReports.add(report);
                } else {
                    return ResponseEntity.badRequest().body("Only PDF, JPG, JPEG, and PNG files are allowed.");
                }
            }
            if (!reportNamesQueue.isEmpty()) {
                return ResponseEntity.badRequest().body("Mismatch : More report names provided than uploaded files.");
            }
            boolean isSaved = reportsService.saveAllReports(allReports);
            if (isSaved) {
                return ResponseEntity.ok("Reports Uploaded Successfully. Total Reports : " + allReports.size());
            } else {
                return ResponseEntity.badRequest().body("Report not saved!! Please try again");
            }

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("Error Uploading Reports : " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<?> getReportsByPatientId(@PathVariable long id) {
        try {
            List<Reports> reportList = reportsService.findReportsByPatientId(id);
            if (reportList != null)
                return ResponseEntity.ok(reportList);
            else
                return ResponseEntity.badRequest().body("No reports found for patient ID : " + id);
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("An unexpected error occurred : " + e.getMessage());
        }
    }

    @PutMapping("/updateReport")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<String> updateReport(
            @RequestParam("medicalReport") MultipartFile file,
            @RequestParam("reportId") long reportId,
            @RequestParam("patientId") long patientId) {

        User logInUser = null;
        try {
            logInUser = userService.getAuthenticateUser();

            Reports existingReport = reportsService.findReportByIdAndPatientId(reportId, patientId);
            if (existingReport == null) {
                return ResponseEntity.badRequest()
                        .body("Report not found with ID : " + reportId + " for patient ID : " + patientId);
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please provide a valid report file.");
            }

            List<String> allowedTypes = List.of("application/pdf", "image/jpeg", "image/jpg", "image/png");
            String contentType = file.getContentType();

            if (!allowedTypes.contains(contentType)) {
                return ResponseEntity.badRequest().body("Only PDF, JPG, JPEG, and PNG files are allowed.");
            }

            existingReport.setReport(file.getBytes());
            existingReport.setName(file.getOriginalFilename());
            existingReport.setType(contentType);
            existingReport.setModifiedUser(logInUser);
            existingReport.setModifiedDate(LocalDate.now());

            boolean isSaved = reportsService.saveReport(existingReport);
            if (isSaved) {
                return ResponseEntity.ok("Report updated successfully for patient ID : " + patientId + " with report ID : " + reportId);

            } else {
                return ResponseEntity.badRequest().body("Report not updated!! Please try again.");
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e)  + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file : " + e.getMessage());
        }
    }
}