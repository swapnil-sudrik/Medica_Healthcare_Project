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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
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

    //=================================================== UPLOAD REPORTS =================================================================//
    @PostMapping("/uploadReport/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<String> saveReport(@PathVariable long id,
                                                          @RequestParam("medicalReports") MultipartFile[] medicalReports, // Accepts multiple files
                                                          @RequestParam(value = "reportName", required = false) String reportNames) { // Optional parameter for report names


        User logInUser = null;

        try {
            logInUser = userService.getAuthenticateUser(); // Retrieves the currently authenticated user
            Patient patient = patientService.getPatientById(id); // Fetches patient using Patient ID

            if (patient == null) {
                return ResponseEntity.status(200).body("Patient not found with ID : " + id);
            }

            // Allowed file types for reports
            Set<String> allowedReportTypes = Set.of("application/pdf", "image/jpeg", "image/jpg", "image/png", "image/svg+xml");

            Queue<String> reportNamesQueue = new LinkedList<>(); // Initializes a queue to store report names if provided

            if (reportNames != null && !reportNames.trim().isEmpty()) {
                reportNamesQueue.addAll(Arrays.asList(reportNames.split(","))); // Splits report names by comma and adds to queue
            }

            List<Reports> allReports = new ArrayList<>(); // List to store reports before saving

            for (MultipartFile file : medicalReports) { // Iterates over each uploaded file
                if (file.isEmpty()) {
                    return ResponseEntity.badRequest().body("Please provide a report.");
                }
                if ("application/zip".equalsIgnoreCase(file.getContentType()) ||
                        "application/x-zip-compressed".equalsIgnoreCase(file.getContentType())) {
                    try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
                        ZipEntry entry;
                        while ((entry = zipInputStream.getNextEntry()) != null) {
                            if (!entry.isDirectory()) {
                                String extractedFileName = entry.getName();
                                int lastDotIndex = extractedFileName.lastIndexOf(".");
                                String reportName = (lastDotIndex == -1) ? extractedFileName : extractedFileName.substring(0, lastDotIndex);
                                String extension = (lastDotIndex == -1) ? "" : extractedFileName.substring(lastDotIndex + 1).toLowerCase();

                                if (!extension.matches("pdf|jpeg|png|jpg")) {
                                    return ResponseEntity.status(415).body("ZIP file contains unsupported file type!!\nPlease provide PDF, JPEG, JPG, SVG OR PNG file.");
                                }

                                String assignedReportName = !reportNamesQueue.isEmpty() ? reportNamesQueue.poll().trim() : reportName;
                                byte[] fileBytes = zipInputStream.readAllBytes();

                                Reports report = new Reports();
                                report.setReport(fileBytes);
                                report.setName(assignedReportName);

                                String mimeType;
                                switch (extension.toLowerCase()) {
                                    case "pdf" -> mimeType = "application/pdf";
                                    case "jpg", "jpeg" -> mimeType = "image/jpeg";
                                    case "png" -> mimeType = "image/png";
                                    case "svg" -> mimeType = "image/svg+xml";
                                    default -> mimeType = "application/octet-stream";
                                }
                                report.setType(mimeType);

                                // report.setType(getMimeTypeByExtension(extension)); // <-- Correct MIME type
                                report.setCreatedUser(logInUser);
                                report.setCreatedDate(LocalDate.now());
                                report.setModifiedUser(logInUser);
                                report.setModifiedDate(LocalDate.now());
                                report.setPatient(patient);

                                allReports.add(report);
                            }
                        }
                    }
                } else if (allowedReportTypes.contains(file.getContentType())) { // Checks if file type is allowed (PDF, JPEG, PNG, JPG)
                    String originalFileName = file.getOriginalFilename(); // Gets the original file name
                    if (originalFileName == null) {
                        return ResponseEntity.badRequest().body("Report is not having name!!");
                    }
                    int lastDotIndex = originalFileName.lastIndexOf("."); // Finds the last dot index to extract file name
                    String cleanFileName = (lastDotIndex == -1) ? originalFileName : originalFileName.substring(0, lastDotIndex); // Extracts file name without extension

                    // Assigns report name from queue if available; otherwise, uses file name
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
                    return ResponseEntity.status(415).body("Only PDF, JPG, JPEG, SVG and PNG files are allowed.");
                }
            }
            // Checks if extra report names were provided
            if (!reportNamesQueue.isEmpty()) {
                return ResponseEntity.badRequest().body("Mismatch : More report names provided than uploaded files.");
            }
            boolean isSaved = reportsService.saveAllReports(allReports); // Calls service to save all reports
            if (isSaved) {
                return ResponseEntity.ok("Reports Uploaded Successfully. Total Reports : " + allReports.size());
            } else {
                return ResponseEntity.status(500).body("Report not saved!! Please try again");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.status(500).body("Error Uploading Reports : " + e.getMessage());
        }
    }

    //==================================================== GET ALL REPORTS BY PATIENT ID ==================================================//
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    @GetMapping("/getReportListByPatientId/{id}")
    public synchronized ResponseEntity<?> getReportsByPatientId(@PathVariable long id) {
        try {
            List<Reports> reportList = reportsService.findReportsByPatientId(id);  // Fetches the list of reports associated with the given patient ID
            if (reportList != null)
                return ResponseEntity.ok(reportList);
            else
                return ResponseEntity.status(200).body("No reports found for patient ID : " + id);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.badRequest().body("An unexpected error occurred : " + e.getMessage());
        }
    }

    //=========================================================== UPDATE REPORT ============================================================//
    @PutMapping("/updateReport")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<String> updateReport(
            @RequestParam("medicalReport") MultipartFile[] files, // Accepts array to check for more than one
            @RequestParam("reportId") long reportId) {

        User logInUser = null;
        try {
            logInUser = userService.getAuthenticateUser();
            Reports existingReport = reportsService.getReportByReportId(reportId);
            if (existingReport == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Report not found with ID : " + reportId);
            }

            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body("Please provide a report file.");
            }

            if (files.length > 1) {
                return ResponseEntity.badRequest().body("Only one file is allowed.");
            }

            MultipartFile file = files[0];

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Provided file is empty.");
            }

            List<String> allowedTypes = List.of("application/pdf", "image/jpeg", "image/jpg", "image/png", "image/svg+xml");
            if (!allowedTypes.contains(file.getContentType())) {
                return ResponseEntity.badRequest().body("Only PDF, JPG, JPEG, SVG and PNG files are allowed.");
            }

            existingReport.setReport(file.getBytes());
            existingReport.setName(file.getOriginalFilename());
            existingReport.setType(file.getContentType());
            existingReport.setModifiedUser(logInUser);
            existingReport.setModifiedDate(LocalDate.now());

            boolean isSaved = reportsService.saveReport(existingReport);
            if (isSaved) {
                return ResponseEntity.ok("Report updated successfully for Report ID : " + reportId);
            } else {
                return ResponseEntity.badRequest().body("Report not updated!! Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file : " + e.getMessage());
        }
    }

    //=================================================== VIEW REPORT ==============================================================//
    @GetMapping("/viewReport/{reportId}")
    @PreAuthorize("hasAnyAuthority('RECEPTIONIST', 'DOCTOR')")
    public synchronized ResponseEntity<?> viewReport(@PathVariable long reportId) {
        try {
            // Fetch the report object from the database using the report ID
            Reports report = reportsService.getReportByReportId(reportId);

            // If no report is found, return a 200 OK with a message
            if (report == null) {
                return ResponseEntity.ok().body("No Report Found For Given ID");
            }

            // Get the actual report data as a byte array
            byte[] reportData = report.getReport();

            // If report data is empty or null, return HTTP 204 No Content
            if (reportData == null || reportData.length == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            // Wrap the byte array into a resource that Spring can stream in the response
            ByteArrayResource resource = new ByteArrayResource(reportData);

            // Parse the media type (like application/pdf, image/png) stored in the database
            MediaType mediaType = MediaType.parseMediaType(report.getType());

            // Create HTTP headers for the response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType); // Set the Content-Type header dynamically
            // Set Content-Disposition to inline, so browser tries to display the file
            // Also sets the filename so browsers know how to name the file if saved
            headers.setContentDisposition(ContentDisposition.inline().filename(report.getName()).build());
            headers.setContentLength(reportData.length); // Set Content-Length header for the file size

            // Return the file data with headers and HTTP 200 OK status
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + " Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //================================================= GET REPORT BY REPORT ID =========================================================//
    @GetMapping("/getReportById/{id}")
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RECEPTIONIST')")
    public synchronized ResponseEntity<?> getReportByReportId(@PathVariable long id) {
        try {
            Reports report = reportsService.getReportByReportId(id);
            if (report == null) {
                return ResponseEntity.status(200).body("No Report Found for Given ID");
            } else {
                return ResponseEntity.ok().body(report);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(ExceptionUtils.getStackTrace(e) + "Log-in User ID : " + userService.getAuthenticateUser().getId());
            return ResponseEntity.status(500).body("Something went wrong!!");
        }
    }
}