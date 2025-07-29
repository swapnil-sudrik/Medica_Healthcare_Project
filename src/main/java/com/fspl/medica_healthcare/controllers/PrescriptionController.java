package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.PrescriptionDTO;
import com.fspl.medica_healthcare.exceptions.ResourceNotFoundException;
import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.repositories.PrescriptionRepository;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;




@RestController
@RequestMapping("/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;


    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private SettingsService settingsService;



    private static final Logger logger = Logger.getLogger(PrescriptionController.class);


//----------------------------------------------------------------------------------------------------------------------

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('DOCTOR')")  // Ensures that only users with 'DOCTOR' role can access this method
    public synchronized ResponseEntity<String> createPrescription(@Valid @RequestBody PrescriptionDTO prescriptionDTO) {
        User loginUser = null;
        try {
            // Check if the medicine list is null, empty, or contains invalid names.
            // Medicine names must start with a letter and can contain letters, numbers, and spaces
            if (prescriptionDTO.getMedicine() == null || prescriptionDTO.getMedicine().isEmpty() ||
                    prescriptionDTO.getMedicine().stream().anyMatch(m -> !m.matches("^[a-zA-Z\\s][a-zA-Z0-9\\s]*$"))) {
                return ResponseEntity.badRequest().body("Medicine list cannot be null and empty OR Medicine names must start with a letter and can contain letters, numbers, and spaces.");
            }

            // Validation: Check if the dosage list is valid and matches the medicine count
            if (prescriptionDTO.getDosage() == null || prescriptionDTO.getDosage().isEmpty() ||
                    prescriptionDTO.getDosage().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.badRequest().body("Dosage list cannot be null and empty OR Size must match medicine count");
            }

            // Loop through the medicine list to validate dosage based on the type (Tablet or Syrup)
            for (int i = 0; i < prescriptionDTO.getDosage().size(); i++) {
                String dosage = prescriptionDTO.getDosage().get(i);
                String type = prescriptionDTO.getType().get(i);

                if (type.equalsIgnoreCase("Tablet")) {
                    // Tablet dosage cannot contain "ml"
                    if (dosage.toLowerCase().contains("ml")) {
                        return ResponseEntity.badRequest().body("Invalid tablet. Tablet dosages cannot contain 'ml'.");
                    }
                    // Tablet dosage must be a valid number
                    else if (!dosage.matches("^[0-9]+(\\.[0-9]+)?$")) {
                        return ResponseEntity.badRequest().body("Invalid tablet dosage format. Tablet dosages must be numbers.");
                    }
                } else if (type.equalsIgnoreCase("Syrup")) {
                    // Syrup dosage must end with "ml"
                    if (!dosage.toLowerCase().endsWith("ml")) {
                        return ResponseEntity.badRequest().body("Invalid syrup dosage. Syrup dosages must end with 'ml'.");
                    }
                    // Syrup dosage must have valid numbers before "ml"
                    else if (!dosage.substring(0, dosage.length() - 2).trim().matches("^[0-9]+(\\.[0-9]+)?$")) {
                        return ResponseEntity.badRequest().body("Invalid syrup dosage format. Syrup dosages must have numbers before 'ml'.");
                    }
                } else {
                    return ResponseEntity.badRequest().body("Invalid type. Only 'Tablet' and 'Syrup' are allowed.");
                }
            }

            // Validation: Schedule list should match the medicine count and contain valid values (1, 2, or 3 times)
            if (prescriptionDTO.getSchedule() == null || prescriptionDTO.getSchedule().isEmpty() ||
                    prescriptionDTO.getSchedule().size() != prescriptionDTO.getMedicine().size() ||
                    prescriptionDTO.getMedicine().contains(0) ||
                    prescriptionDTO.getSchedule().stream().anyMatch(s -> !s.matches("^[1-3]\\s+time(s)?$"))) {
                return ResponseEntity.badRequest().body("Schedule list cannot be null or empty,  Schedule must match count of medicine, Schedule must be greater than 0 OR '1 times', '2 times', or '3 times' only.");
            }

            // Validation: Check if the type list is valid and matches the medicine count
            if (prescriptionDTO.getType() == null || prescriptionDTO.getType().isEmpty() ||
                    prescriptionDTO.getType().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.badRequest().body("Type list cannot be null or empty OR Type count must match medicine count");
            }

            // Validate that only "Tablet" and "Syrup" are allowed types
            for (String type : prescriptionDTO.getType()) {
                if (!type.equalsIgnoreCase("Tablet") && !type.equalsIgnoreCase("Syrup")) {
                    return ResponseEntity.badRequest().body("Invalid prescription type: " + type + ". Only 'Tablet' and 'Syrup' are allowed.");
                }
            }

            // Validation: Quantity list should match the medicine count and contain values greater than 0
            if (prescriptionDTO.getQuantity() == null || prescriptionDTO.getQuantity().isEmpty() ||
                    prescriptionDTO.getQuantity().size() != prescriptionDTO.getMedicine().size() ||
                    prescriptionDTO.getQuantity().contains(0)) {
                return ResponseEntity.badRequest().body("Quantity list cannot be null or empty OR Quantity count must match medicine count, or Invalid Quantity must be greater than 0");
            }

            // Validation: Check if the number of days matches the medicine count
//            int medicineCount = 0;
//            for (int i = 0; i < prescriptionDTO.getMedicine().size(); i++) {
//                if (prescriptionDTO.getType().get(i).equals("Tablet")) {
//                    medicineCount++;
//                }
//            }

            // Ensure the number of days matches the tablet count
            if (prescriptionDTO.getNumberOfDays().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Number of days count must match medicine count");
            }

            // Fetch authenticated user (doctor) information
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            // Validate if appointment exists
            Appointment appointment = appointmentService.findAppointmentById(prescriptionDTO.getAppoinmentId());
            if (appointment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found");
            }

            // Check if prescription already exists for this appointment
            if (prescriptionService.getPrescriptionByAppointmentId(prescriptionDTO.getAppoinmentId(), loginUser)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Prescription is already available for this appointment");
            }

            // Create a new prescription entity from the prescriptionDTO
            Prescription prescription = prescriptionDTOToPrescription(prescriptionDTO);
            prescription.setCreatedUser(loginUser);
            prescription.setLoginUser(loginUser);
            prescription.setAppointment(appointment);
            prescription.setStatus(1);  // Set prescription as Active
            prescription.setCreatedDate(LocalDateTime.now());
            prescription.setModifiedDate(LocalDateTime.now());

            // Save or update the prescription in the database
            if (prescriptionService.saveOrUpdatePrescription(prescription)) {
                // Send prescription as a PDF if saved successfully
                if (sendPrescriptionAsPdf(prescription)) {
                    return ResponseEntity.ok("Prescription sent successfully");
                } else {
                    return ResponseEntity.ok("Prescription saved but PDF not sent");
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Prescription not saved");

            }
        } catch (Exception e) {
            e.printStackTrace();
            // Catch any exceptions that occur during the creation process and log the error
            logger.error("\n An error occurred while creating the prescription" + ExceptionUtils.getStackTrace(e) +
                    " \nLogged user" + loginUser.getId() + " \nUser Request " + prescriptionDTO);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating the prescription");
        }
    }

//-------------------------------------------------------------------------------------------------------------


    @PutMapping("/{prescriptionid}")
    @PreAuthorize("hasAuthority('DOCTOR')")  // Ensures that only users with 'DOCTOR' authority can access this endpoint
    public ResponseEntity<String> updatePrescription(
            @PathVariable long prescriptionid,  // Fetches the prescription ID from the URL path
            @Valid @RequestBody PrescriptionDTO prescriptionDTO) {  // Accepts the updated prescription details in the request body
        User loginUser = null;
        try {
            //  Validate the medicine list
            if (prescriptionDTO.getMedicine() == null || prescriptionDTO.getMedicine().isEmpty() ||
                    prescriptionDTO.getMedicine().stream().anyMatch(m -> !m.matches("^[a-zA-Z\\s][a-zA-Z0-9\\s]*$"))) {
                return ResponseEntity.badRequest().body("Medicine list cannot be null and empty OR Medicine names must start with a letter and can contain letters, numbers, and spaces.");
            }

            // Validate that the dosage list is not null, not empty, and the size matches the medicine count
            if (prescriptionDTO.getDosage() == null || prescriptionDTO.getDosage().isEmpty() ||
                    prescriptionDTO.getDosage().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.badRequest().body("Dosage list cannot be null and empty OR Size must match medicine count");
            }

            // Loop through medicine and validate dosage format for each medicine (Tablet/Syrup)
            for (int i = 0; i < prescriptionDTO.getDosage().size(); i++) {
                String dosage = prescriptionDTO.getDosage().get(i);
                String type = prescriptionDTO.getType().get(i);

                if (type.equalsIgnoreCase("Tablet")) {
                    // Tablet dosage cannot contain "ml" and should be a valid number
                    if (dosage.toLowerCase().contains("ml")) {
                        return ResponseEntity.badRequest().body("Invalid tablet dosage. Tablet dosages cannot contain 'ml'.");
                    } else if (!dosage.matches("^[0-9]+(\\.[0-9]+)?$")) {
                        return ResponseEntity.badRequest().body("Invalid tablet dosage format. Tablet dosages must be numbers.");
                    }
                } else if (type.equalsIgnoreCase("Syrup")) {
                    // Syrup dosage must end with "ml" and have valid numbers before "ml"
                    if (!dosage.toLowerCase().endsWith("ml")) {
                        return ResponseEntity.badRequest().body("Invalid syrup dosage. Syrup dosages must end with 'ml'.");
                    } else if (!dosage.substring(0, dosage.length() - 2).trim().matches("^[0-9]+(\\.[0-9]+)?$")) {
                        return ResponseEntity.badRequest().body("Invalid syrup dosage format. Syrup dosages must have numbers before 'ml'.");
                    }
                } else {
                    return ResponseEntity.badRequest().body("Invalid type. Only 'Tablet' and 'Syrup' are allowed.");
                }
            }

            // Validate the schedule list (should not be null/empty, and size must match medicine count)
            if (prescriptionDTO.getSchedule() == null || prescriptionDTO.getSchedule().isEmpty() ||
                    prescriptionDTO.getSchedule().size() != prescriptionDTO.getMedicine().size() ||
                    prescriptionDTO.getMedicine().contains(0) ||
                    prescriptionDTO.getSchedule().stream().anyMatch(s -> !s.matches("^[1-3]\\s+time(s)?$"))) {
                return ResponseEntity.badRequest().body("Schedule list cannot be null or empty,  Schedule must match count of medicine, Schedule must be greater than 0 OR '1 times', '2 times', or '3 times' only.");
            }

            // Validate that the type list is valid (only "Tablet" and "Syrup") and matches the medicine count
            if (prescriptionDTO.getType() == null || prescriptionDTO.getType().isEmpty() ||
                    prescriptionDTO.getType().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.badRequest().body("Type list cannot be null or empty OR Type count must match medicine count");
            }

            // Validate each type to ensure it's either "Tablet" or "Syrup"
            for (String type : prescriptionDTO.getType()) {
                if (!type.equalsIgnoreCase("Tablet") && !type.equalsIgnoreCase("Syrup")) {
                    return ResponseEntity.badRequest().body("Invalid prescription type: " + type + ". Only 'Tablet' and 'Syrup' are allowed.");
                }
            }

            //  Validate the quantity list (should not be null/empty, and size must match medicine count)
            if (prescriptionDTO.getQuantity() == null || prescriptionDTO.getQuantity().isEmpty() ||
                    prescriptionDTO.getQuantity().size() != prescriptionDTO.getMedicine().size() ||
                    prescriptionDTO.getQuantity().contains(0)) {
                return ResponseEntity.badRequest().body("Quantity list cannot be null or empty OR Quantity count must match medicine count, or Invalid Quantity must be greater than 0");
            }

            // Validate the number of days list (should match the number of tablet medicines and be valid)
            int medicineCount = 0;
            for (int i = 0; i < prescriptionDTO.getMedicine().size(); i++) {
                if (prescriptionDTO.getType().get(i).equals("Tablet")) {
                    medicineCount++;
                }
            }

            // Ensure the number of days matches the tablet count
            if (prescriptionDTO.getNumberOfDays().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.badRequest().body("Number of days count must match medicine count");
            }

            // Fetch the authenticated user (doctor) and handle the case where no user is authenticated
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");

            }

            // Fetch the existing prescription and validate that it exists
            Prescription prescription = prescriptionService.getPrescriptionById(prescriptionid, loginUser);
            if (prescription == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Check prescription ID therefore prescription not found");
            }

            //Update the prescription with the new data from the DTO
            prescription.setAppointment(prescription.getAppointment());
            prescription.setType(prescriptionDTO.getType());
            prescription.setMedicine(prescriptionDTO.getMedicine());
            prescription.setDosage(prescriptionDTO.getDosage());
            prescription.setSchedule(prescriptionDTO.getSchedule());
            prescription.setQuantity(prescriptionDTO.getQuantity());
            prescription.setNumberOfDays(prescriptionDTO.getNumberOfDays());
            prescription.setCreatedUser(prescription.getLoginUser());
            prescription.setLoginUser(loginUser);
            prescription.setModifiedDate(LocalDateTime.now());  // Set the modified date to the current time

            //Save the updated prescription and send it as a PDF if successful
            if (prescriptionService.saveOrUpdatePrescription(prescription)) {
                if (sendPrescriptionAsPdf(prescription)) {
                    return ResponseEntity.ok("Prescription updated successfully");
                } else {
                    return ResponseEntity.ok("Prescription saved but PDF not sent");
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Prescription not saved");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Catch any exceptions and log the error for debugging
            logger.error("\n Error while updating prescription: " + ExceptionUtils.getStackTrace(e) +
                    " \nLogged user: " + loginUser.getId() +
                    " \nUser Request: " + prescriptionDTO);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update prescription due to an internal error");
        }
    }

//------------------------------------------------------------------------------------------------------------


    @GetMapping("/{prescriptionid}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public  ResponseEntity<?>  getPrescriptionById(@PathVariable long prescriptionid) {
        User loginUser = null;
        try {
            // Fetch authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            // for get previous prescription status if prescription status not match with condition it show the return message
            Prescription prescription = prescriptionService.getPrescriptionById(prescriptionid,loginUser);
            if ((!(prescription ==null)) && prescription.getStatus() == 1) {
                return ResponseEntity.ok( prescription);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prescription not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error occurred while fetching prescription: " + ExceptionUtils.getStackTrace(e)  +"Logged user : " +loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch prescriptions due to an internal error");

        }
    }



//--------------------------------------------------------------------------------------------------------------


    @GetMapping("/allprescription")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<?> getAllPrescriptions() {
        User loginUser = null;
        try {
            // Fetch authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            // for get all prescription
            List<Prescription> prescriptions = prescriptionService.getAllPrescriptions(loginUser);
            // for validation if prescription null or isempty it show the error
            if (prescriptions == null || prescriptions.isEmpty()) {
                return ResponseEntity.status(404).body("No prescriptions found");
            }

            return ResponseEntity.ok(prescriptions);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error while fetching prescriptions: " +ExceptionUtils.getStackTrace(e) +"Logged User"+loginUser.getId());
            return ResponseEntity.status(500).body("Failed to fetch prescriptions due to an internal error");
        }
    }



//---------------------------------------------------------------------------------------------------------------------


    @DeleteMapping("statuschange/{prescriptionid}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<String> statusChange(@PathVariable long prescriptionid) {
        User loginUser = null;
        try {
            // Fetch authenticated/login user
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            // for validation to check id null or zero if match show message
            if (prescriptionid == -1 || prescriptionid <= 0) {
                return ResponseEntity.badRequest().body("Invalid prescription ID");
            }

            //  get previous prescription
            Prescription prescription = prescriptionService.getPrescriptionById(prescriptionid,loginUser);

            if(prescription==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No prescriptions found");
            }

            // set a status as a false due to soft delete
            prescription.setStatus(0);
            prescriptionService.saveOrUpdatePrescription(prescription);

            return ResponseEntity.ok("Status Change  successfully");

        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
            logger.error("\n Error while deleting prescription: " + ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch prescriptions due to an internal error");

        }
    }


//---------------------------------------------------------------------------------------------------------------


    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPrescriptionsByPatientId(
            @PathVariable("patientId") long patientId) {
        User loginUser=null;
        try {
            // Fetch authenticated user if login user null it show the return message
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");

            }
            List<Prescription> prescriptions = prescriptionService.findPrescriptionByPatientId(patientId,loginUser);
            // if prescription isempty  on that patientid  it show string message
            if (prescriptions.isEmpty()) {
                String message = "No prescriptions found for patientId: " + patientId;
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
            }

            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error while fetching prescriptions for patientId " + patientId + ": " + ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }




//--------------------------------------------------------------------------------------------------------------------------

    @GetMapping("/medicines")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<?> getAllMedicine() {
        User loginUser=null;
        try {
            // Fetch authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<List<String>> medicines = prescriptionService.findAllMedicine(loginUser);
            // for the validation  to check the medicine is empty it show the message
            if (medicines == null || medicines.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Medicine not Found");
            }
            //  sort the  list for  unique medicine
            Set<String> flattenedMedicines = medicines.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());

            return ResponseEntity.ok(flattenedMedicines);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error while fetching medicines: " +ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch medicines due to an internal error");
        }
    }


    //-------------------------------------------------------------------------------------------------------------------
    @Async
    public boolean sendPrescriptionAsPdf(Prescription prescription) {
        try {
            // Validate prescription and patient information
            if (prescription == null || prescription.getAppointment() == null ||
                    prescription.getAppointment().getPatient() == null ||
                    prescription.getAppointment().getPatient().getEmailId() == null) {
                return false;
            }

//             String email = prescription.getAppointment().getPatient().getEmailId();
            String email = encryptionUtil.decrypt(new String(prescription.getAppointment().getPatient().getEmailId()));


            // Generate the prescription PDF
            ByteArrayOutputStream pdfStream = generatePrescriptionPdf(prescription);

            if (pdfStream == null) {
                logger.error("Failed to generate");
                return false;
            }
            // Prepare email details
            String subject = "Your Prescription Details";
            String content = "Please find attached your prescription details.";

            // Ensure the PDF is sent with correct MIME type
            String fileName = "Prescription_" + prescription.getMedicine() + ".pdf";
            // Send the email with the PDF attachment
            emailService.sendEmailWithAttachment(
                    email,
                    subject,
                    content,
                    fileName,
                    pdfStream.toByteArray()
            );
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error sending prescription PDF: " + ExceptionUtils.getStackTrace(e) );
            return false;
        }
    }
//    ----------------------------------------------------------------------------------------------------------

    // here we have to generateprescriptionpdf
    private ByteArrayOutputStream generatePrescriptionPdf(Prescription prescription) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            //if the prescription or appointment or patient null it show error message
            Hospital hospital = prescription.getAppointment().getHospital();

            if (prescription == null || prescription.getAppointment() == null || prescription.getAppointment().getPatient() == null) {
                logger.error("Prescription or appointment or patient is null");
                return null;
            }
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();



            // *Contact and Address Information*
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // *Table for Patient and Doctor Details*
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setSpacingBefore(10f);
            detailsTable.setWidths(new float[]{50f, 50f}); // Equal-width columns



            if (prescription.getAppointment().getBloodPressure() != null) {
                prescription.getAppointment().setBloodPressure(encryptionUtil.decrypt(prescription.getAppointment().getBloodPressure()));
            }

            if (prescription.getAppointment().getPatient().getEmailId()!=null){
                prescription.getAppointment().getPatient().setEmailId(encryptionUtil.decrypt(new String(prescription.getAppointment().getPatient().getEmailId())).getBytes());
            }
            Settings settings = settingsService.getSettingsById(prescription.getAppointment().getHospital().getId());

            if (settings != null) {
                Image letterheadImage = Image.getInstance(settings.getHospitalLetterHead());
//                letterheadImage.scaleToFit(500f, 100f); // scale as needed
                letterheadImage.scaleToFit(595f, Float.MAX_VALUE); // scales to A4 width, keeps aspect ratio
                letterheadImage.setAlignment(Element.ALIGN_CENTER);
                document.add(letterheadImage);
            }


            // *Left Column - Patient Details*
            PdfPCell patientDetails = new PdfPCell();
            patientDetails.setBorder(Rectangle.NO_BORDER);
            patientDetails.addElement(new Paragraph("Patient Name: " + encryptionUtil.decrypt(new String(prescription.getAppointment().getPatient().getName())), normalFont));
            patientDetails.addElement(new Paragraph("Age:" + encryptionUtil.decrypt(new String(prescription.getAppointment().getPatient().getAge())), normalFont));
            patientDetails.addElement(new Paragraph("Gender: " + encryptionUtil.decrypt(new String(prescription.getAppointment().getPatient().getGender())), normalFont));
            patientDetails.addElement(new Paragraph("Height: " +
                    (prescription.getAppointment().getHeight() != null ? prescription.getAppointment().getHeight() + " " : "N/A"), normalFont));
            patientDetails.addElement(new Paragraph("Weight: " +
                    (prescription.getAppointment().getWeight() != 0 ? prescription.getAppointment().getWeight() + " " : "N/A"), normalFont));
            patientDetails.addElement(new Paragraph("Blood Pressure: " +
                    (prescription.getAppointment().getBloodPressure() != null ? prescription.getAppointment().getBloodPressure() : "N/A"), normalFont));
            LocalDateTime createdDateTime = prescription.getCreatedDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
            String formattedDate = prescription.getCreatedDate().format(formatter);

            PdfPCell doctorDetails = new PdfPCell();
            doctorDetails.setBorder(Rectangle.NO_BORDER);
            doctorDetails.setHorizontalAlignment(Element.ALIGN_RIGHT);
            doctorDetails.addElement(new Paragraph("Doctor Name: " + prescription.getLoginUser().getName(), normalFont));
            doctorDetails.addElement(new Paragraph("Date Issued: " + formattedDate, normalFont));
            // *Add Cells to Table*
            detailsTable.addCell(patientDetails);
            detailsTable.addCell(doctorDetails);

            // *Add Table to Document*
            document.add(detailsTable);
            document.add(Chunk.NEWLINE);


            // *Table for Prescription Details*
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(80f);
            table.setWidths(new float[]{2f, 3f, 2f, 3f, 2f, 2f});

            // *Centering the table itself*
            table.setHorizontalAlignment(Element.ALIGN_CENTER);

            document.add(Chunk.NEWLINE);


            // *Table Headers*
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // *Add headers and ensure each header is centered*
            table.addCell(createCenteredCell("Type", headerFont));
            table.addCell(createCenteredCell("Medicine", headerFont));
            table.addCell(createCenteredCell("Dosage", headerFont));
            table.addCell(createCenteredCell("Schedule", headerFont));
            table.addCell(createCenteredCell("Quantity", headerFont));
            table.addCell(createCenteredCell("Number of Days", headerFont));

            // *Table Data row*
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            List<String> medicines = prescription.getMedicine();
            List<String> types = prescription.getType();
            List<String> dosages = prescription.getDosage();
            List<String> schedules = prescription.getSchedule();
            List<Integer> quantity = prescription.getQuantity();
            List<Integer> numberOfDays = prescription.getNumberOfDays();

            // to set a null to empty number of days for syrup
//            if (medicines.size() != numberOfDays.size()) {
//                int k = medicines.size();
//                int ii = numberOfDays.size();
//                for (int i = ii; i < k; i++) {
//                    numberOfDays.add(null);
//                }
//            }
            // to set the data in cell
            for (int i = 0; i < medicines.size(); i++) {

                table.addCell(createCenteredCell(types.get(i), dataFont));
                table.addCell(createCenteredCell(medicines.get(i), dataFont));
                table.addCell(createCenteredCell(dosages.get(i), dataFont));
                table.addCell(createCenteredCell(schedules.get(i).toUpperCase(Locale.ROOT), dataFont));
                table.addCell(createCenteredCell(quantity.get(i).toString(), dataFont));
                // Integer numberOfDaysValue = numberOfDays.get(i);
                table.addCell(createCenteredCell(numberOfDays.get(i).toString(), dataFont));
                // table.addCell(createCenteredCell(numberOfDaysValue == null ? null : numberOfDaysValue.toString(), dataFont));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Clinical Note: " + new String(prescription.getAppointment().getClinicalNote())));
            document.add(Chunk.NEWLINE);

            // *Rx Logo in the top-right corner*
            String rXPath = "src/images/RxLogo.png";
            Image rxLogo = Image.getInstance(rXPath);
            rxLogo.scaleToFit(100f, 300f);

            float xPosition = document.getPageSize().getWidth() - rxLogo.getScaledWidth() - 450;
            float yPosition = document.getPageSize().getHeight() - rxLogo.getScaledHeight() - 350;
            rxLogo.setAbsolutePosition(xPosition, yPosition);
            document.add(rxLogo);
            document.add(Chunk.NEWLINE);


            Paragraph signature = new Paragraph("Signature: " + prescription.getLoginUser().getName(), normalFont);
            signature.setAlignment(Element.ALIGN_RIGHT);
            document.add(signature);
            document.add(Chunk.NEWLINE);

            document.close();
            return outputStream;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error generating prescription PDF"+ExceptionUtils.getStackTrace(e) );
            return null;
        }

    }


    private PdfPCell createCenteredCell(String content, Font font) {
        try {
            PdfPCell cell = new PdfPCell(new Phrase(content, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            return cell;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error creating centered cell: " + ExceptionUtils.getStackTrace(e) );
            return new PdfPCell(new Phrase("Error"));
        }
    }


    public Prescription prescriptionDTOToPrescription(PrescriptionDTO prescriptionDTO) {
        try {

            if (prescriptionDTO == null) {
                return null;
            }
            Appointment appointment = appointmentService.findAppointmentById(prescriptionDTO.getAppoinmentId());
            if (appointment==null) {
                return null;
            }

            Prescription prescription = new Prescription();

            prescription.setType(prescriptionDTO.getType());
            prescription.setMedicine(prescriptionDTO.getMedicine());
            prescription.setDosage(prescriptionDTO.getDosage());
            prescription.setSchedule(prescriptionDTO.getSchedule());
            prescription.setQuantity(prescriptionDTO.getQuantity());
            prescription.setNumberOfDays(prescriptionDTO.getNumberOfDays());
            prescription.setAppointment(appointment);

            return prescription;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error converting PrescriptionDTO to Prescription: " + ExceptionUtils.getStackTrace(e) );
            return null;
        }
    }


    public PrescriptionDTO prescriptionToPrescriptionDTO(Prescription prescription) {
        try {
            if (prescription == null) {
                // System.err.println("Prescription is null");
                return null;
            }
            PrescriptionDTO prescriptionDTO = new PrescriptionDTO();

            if (prescription.getAppointment() == null) {
                // System.err.println("Appointment is null in Prescription");
                return null;
            }

            prescriptionDTO.setId(prescription.getId());
            prescriptionDTO.setType(prescription.getType());
            prescriptionDTO.setMedicine(prescription.getMedicine());
            prescriptionDTO.setDosage(prescription.getDosage());
            prescriptionDTO.setSchedule(prescription.getSchedule());
            prescriptionDTO.setQuantity(prescription.getQuantity());
            prescriptionDTO.setNumberOfDays(prescription.getNumberOfDays());
            prescriptionDTO.setAppoinmentId(prescription.getAppointment().getId());

            return prescriptionDTO;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("\n Error converting Prescription to PrescriptionDTO: " +ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

}