package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.PrescriptionDTO;
import com.fspl.medica_healthcare.exceptions.ResourceNotFoundException;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Patient;
import com.fspl.medica_healthcare.models.Prescription;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.repositories.PrescriptionRepository;
import com.fspl.medica_healthcare.services.*;
import com.fspl.medica_healthcare.utils.ExceptionMessages;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import com.itextpdf.text.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private UserService userService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;


    @Autowired
    private JavaMailSender mailSender;

    private static final Logger logger = LogManager.getLogger(PrescriptionController.class);


//----------------------------------------------------------------------------------------------------------------------

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public synchronized ResponseEntity<?> createPrescription(@Valid @RequestBody PrescriptionDTO prescriptionDTO) {
        User loginUser=null;
        try {
            if (prescriptionDTO.getMedicine() == null || prescriptionDTO.getMedicine().isEmpty()) {
                return ResponseEntity.ok("Medicine list cannot be null or empty");
            }

            if (prescriptionDTO.getDosage() == null || prescriptionDTO.getDosage().isEmpty() || prescriptionDTO.getDosage().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Dosage list cannot be null or empty or size must match medicine count");
            }


            if (prescriptionDTO.getSchedule() == null || prescriptionDTO.getSchedule().isEmpty() || prescriptionDTO.getSchedule().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Schedule list cannot be null or empty orr schedule must match count of medicine");
            }

            if (prescriptionDTO.getType() == null || prescriptionDTO.getType().isEmpty() || prescriptionDTO.getType().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Type list cannot be null or empty or Type count must match medicine count");
            }

            if (prescriptionDTO.getQuantity() == null || prescriptionDTO.getQuantity().isEmpty() || prescriptionDTO.getQuantity().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Quantity list cannot be null or empty or Quantity count must match medicine count");
            }

            int count=0;
            for(int i=0;i<prescriptionDTO.getMedicine().size();i++) {
                if(prescriptionDTO.getType().get(i).equals("Tablet")){
                    count++;
                }
            }

            if (prescriptionDTO.getNumberOfDays().size()!=count) {

                return ResponseEntity.ok("Number of days count must match medicine count");
            }

            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return ResponseEntity.ok("User not authenticated");
            }

            Appointment appointment = appointmentService.findAppointmentById(prescriptionDTO.getAppoinmentId());
            if (appointment==null) {
                return ResponseEntity.ok("Appointment not found");
            }

            if(prescriptionService.getPrescriptionByAppointmentId(prescriptionDTO.getAppoinmentId(),loginUser)){
                return ResponseEntity.ok("Prescription is available in this appointment");
            }

            Prescription prescription = prescriptionDTOToPrescription(prescriptionDTO);


            prescription.setCreatedUser(loginUser);
            prescription.setLoginUser(loginUser);
            prescription.setAppointment(appointment);
            prescription.setStatus(1); // Set as Active
            prescription.setCreatedDate(LocalDateTime.now());
            prescription.setModifiedDate(LocalDateTime.now());

            if (prescriptionService.saveOrUpdatePrescription(prescription)) {

                if(sendPrescriptionAsPdf(prescription)) {
                    return ResponseEntity.ok(prescription);
                }else {
                    return ResponseEntity.ok("prescription save but pdf not send");
                }
            } else {
                return ResponseEntity.ok("prescription  not save ");
            }
        } catch (Exception e) {
            logger.error("\n An error occurred while creating the prescription \n"+ ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating the prescription");
        }
    }


//-------------------------------------------------------------------------------------------------------------


    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<?> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionDTO prescriptionDTO) {
        User loginUser=null;
        try {

            if (prescriptionDTO.getMedicine() == null || prescriptionDTO.getMedicine().isEmpty()) {
                return ResponseEntity.ok("Medicine list cannot be null or empty");
            }

            if (prescriptionDTO.getDosage() == null || prescriptionDTO.getDosage().isEmpty() || prescriptionDTO.getDosage().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Dosage list cannot be null or empty or size must match medicine count");
            }

            if (prescriptionDTO.getSchedule() == null || prescriptionDTO.getSchedule().isEmpty() || prescriptionDTO.getSchedule().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Schedule list cannot be null or empty orr schedule must match count of medicine");
            }

            if (prescriptionDTO.getType() == null || prescriptionDTO.getType().isEmpty() || prescriptionDTO.getType().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Type list cannot be null or empty or Type count must match medicine count");
            }

            if (prescriptionDTO.getQuantity() == null || prescriptionDTO.getQuantity().isEmpty() || prescriptionDTO.getQuantity().size() != prescriptionDTO.getMedicine().size()) {
                return ResponseEntity.ok("Quantity list cannot be null or empty or Quantity count must match medicine count");
            }
            int count=0;
            for(int i=0;i<prescriptionDTO.getMedicine().size();i++) {
                if(prescriptionDTO.getType().get(i).equals("Tablet")){
                    count++;
                }

            }

            if (prescriptionDTO.getNumberOfDays().size()!=count) {

                return ResponseEntity.ok("Number of days count must match medicine count");
            }
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            Appointment appointment = appointmentService.findAppointmentById(prescriptionDTO.getAppoinmentId());
            if (appointment==null) {
                return ResponseEntity.ok("Appointment not found");
            }

            Prescription prescription = prescriptionService.getPrescriptionById(id,loginUser);
            if (prescription==null) {
                return ResponseEntity.badRequest().body("Invalid prescription ID");
            }

            prescription.setAppointment(appointment);
            prescription.setType(prescriptionDTO.getType());
            prescription.setMedicine(prescriptionDTO.getMedicine());
            prescription.setDosage(prescriptionDTO.getDosage());
            prescription.setSchedule(prescriptionDTO.getSchedule());
            prescription.setQuantity(prescriptionDTO.getQuantity());
            prescription.setNumberOfDays(prescriptionDTO.getNumberOfDays());
            prescription.setCreatedUser(prescription.getLoginUser());
            prescription.setLoginUser(loginUser);
            prescription.setModifiedDate(LocalDateTime.now());


            if (prescriptionService.saveOrUpdatePrescription(prescription)) {
                if(sendPrescriptionAsPdf(prescription)) {
                    return ResponseEntity.ok(prescription);
                }else {
                    return ResponseEntity.ok("prescription save but pdf not send");
                }
            } else {
                return ResponseEntity.ok("prescription  not save ");
            }
        } catch (Exception e) {
            logger.error("\n Error while updating prescription: " +ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update prescription due to an internal error");
        }
    }


//------------------------------------------------------------------------------------------------------------


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public  ResponseEntity<?>  getPrescriptionById(@PathVariable Long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            Prescription prescription = prescriptionService.getPrescriptionById(id,loginUser);
            if ((!(prescription ==null)) && prescription.getStatus() == 1) {
                return ResponseEntity.ok( prescription);
            } else {
                return ResponseEntity.ok("Prescription Not available");
            }
        } catch (Exception e) {
            logger.error("\n Error occurred while fetching prescription: " + ExceptionUtils.getStackTrace(e)  +"Logged user : " +loginUser.getId());
            return  ResponseEntity.status(500).body("Failed to fetch prescriptions due to an internal error");
        }
    }


//--------------------------------------------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<?> getAllPrescriptions() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<Prescription> prescriptions = prescriptionService.getAllPrescriptions(loginUser);

            if (prescriptions == null || prescriptions.isEmpty()) {
                return ResponseEntity.status(404).body("No prescriptions found");
            }

            return ResponseEntity.ok(prescriptions);

        } catch (Exception e) {
            logger.error("\n Error while fetching prescriptions: " +ExceptionUtils.getStackTrace(e) +"Logged User"+loginUser.getId());
            return ResponseEntity.status(500).body("Failed to fetch prescriptions due to an internal error");
        }
    }


//---------------------------------------------------------------------------------------------------------------------


    @DeleteMapping("status/{id}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<String> statusChange(@PathVariable Long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body("Invalid prescription ID");
            }

            Prescription prescription = prescriptionService.getPrescriptionById(id,loginUser);

            if(prescription==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prescription Not Found");
            }


            prescription.setStatus(0);
            prescriptionService.saveOrUpdatePrescription(prescription);

            return ResponseEntity.ok("Status Change  successfully");

        } catch (ResourceNotFoundException e) {
            logger.error("\n Error while deleting prescription: " + ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.ok("Failed to Change Status due to an internal error");
        } catch (Exception e) {
            logger.error("\n Error while deleting prescription: " + ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.ok("Failed to Change Status due to an internal error");
        }
    }


//---------------------------------------------------------------------------------------------------------------


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCTOR')")
    public ResponseEntity<String> deletePrescription(@PathVariable Long id) {
        User loginUser = null;
        try {

            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }

            Prescription prescription = prescriptionService.getPrescriptionById(id,loginUser);
            if (prescription==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prescription not Found");
            }

            prescriptionService.deletePrescription(prescription);

            return ResponseEntity.ok("Prescription deleted successfully");

        } catch (ResourceNotFoundException e) {
            logger.error("\n Error: Prescription not found with id: " + id+ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Failed to delete prescription due to an internal error: ");
        } catch (Exception e) {
            logger.error("\n Error: An unexpected error occurred while deleting the prescription."+id+ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete prescription due to an internal error.");
        }
    }


//--------------------------------------------------------------------------------------------------------------------

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPrescriptionsByPatientId(
            @PathVariable("patientId") Long patientId) {
        User loginUser=null;
        try {
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<Prescription> prescriptions = prescriptionService.findPrescriptionByPatientId(patientId,loginUser);

            if (prescriptions.isEmpty()) {
                String message = "No prescriptions found for patientId: " + patientId;
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
            }

            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
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
            loginUser = userService.getAuthenticateUser();
            if (loginUser==null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExceptionMessages.SOMETHING_WENT_WRONG);
            }
            List<List<String>> medicines = prescriptionService.findAllMedicine(loginUser);

            if (medicines == null || medicines.isEmpty()) {
                return ResponseEntity.ok("Medicines Not found");
            }
            Set<String> flattenedMedicines = medicines.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());

            return ResponseEntity.ok(flattenedMedicines);

        } catch (Exception e) {
            logger.error("\n Error while fetching medicines: " +ExceptionUtils.getStackTrace(e) +"Logged user"+loginUser.getId());
            return ResponseEntity.ok("Failed to fetch medicines due to an internal error");
        }
    }

    //-------------------------------------------------------------------------------------------------------------------

    public boolean sendPrescriptionAsPdf(Prescription prescription) {
        try {
            if (prescription == null) {
               // System.err.println("Prescription is null");
                return false;
            }

            if (prescription.getAppointment() == null || prescription.getAppointment().getPatient() == null) {
               // System.err.println("Prescription appointment or patient is null");
                return false;
            }

            Patient patient = patientService.getPatientById(prescription.getAppointment().getPatient().getId());
            if (patient == null || patient.getEmailId() == null) {
               // System.err.println("Patient or email is null");
                return false;
            }

            String email = patient.getEmailId();
            //System.out.println(email);

            ByteArrayOutputStream pdfStream = generatePrescriptionPdf(prescription);
            if (pdfStream == null) {
                logger.error("Failed to generate PDF");
                return false;
            }

            String subject = "Your Prescription Details";
            String content = "Please find attached your prescription details.";

            String fileName = "Prescription_" + prescription.getMedicine() + ".pdf";

            CompletableFuture<Boolean> emailSentFuture = sendEmailWithAttachment(
                    email,
                    subject,
                    content,
                    fileName,
                    pdfStream.toByteArray(),
                    "application/pdf"
            );
            return true;

        } catch (Exception e) {
            logger.error("\n Error sending prescription PDF: " + ExceptionUtils.getStackTrace(e) );
            return false;
        }
    }


//    ----------------------------------------------------------------------------------------------------------

    private ByteArrayOutputStream generatePrescriptionPdf(Prescription prescription) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (prescription == null || prescription.getAppointment() == null || prescription.getAppointment().getPatient() == null) {
                System.out.println("Prescription or appointment or patient is null");
                return null;
            }
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // *Title Section*
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("PRESCRIPTION DETAILS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // *Logo Section*
            String logoPath = "C:/Users/DELL/Desktop/images/logo.png";
            Image logo = Image.getInstance(logoPath);
            logo.setAlignment(Element.ALIGN_CENTER);
            logo.scaleToFit(200f, 100f);
            document.add(logo);
            document.add(Chunk.NEWLINE);

            // *Contact and Address Information*
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph contactInfo = new Paragraph("Contact: (123) 456-7890", normalFont);
            contactInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(contactInfo);

            Paragraph address = new Paragraph("Address: Geera Imperium, Pune, Maharashtra, India", normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
            document.add(Chunk.NEWLINE);

            // *Table for Patient and Doctor Details*
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setSpacingBefore(10f);
            detailsTable.setWidths(new float[]{50f, 50f}); // Equal-width columns

            // *Left Column - Patient Details*
            PdfPCell patientDetails = new PdfPCell();
            patientDetails.setBorder(Rectangle.NO_BORDER);
            patientDetails.addElement(new Paragraph("Patient Name: " + prescription.getAppointment().getPatient().getName(), normalFont));
            patientDetails.addElement(new Paragraph("Age:" + prescription.getAppointment().getPatient().getAge(), normalFont));
            patientDetails.addElement(new Paragraph("Gender: " + prescription.getAppointment().getPatient().getGender(), normalFont));
            patientDetails.addElement(new Paragraph("Height: " +
                    (prescription.getAppointment().getHeight() != null ? prescription.getAppointment().getHeight() + " " : "N/A"), normalFont));
            patientDetails.addElement(new Paragraph("Weight: " +
                    (prescription.getAppointment().getWeight() != 0 ? prescription.getAppointment().getWeight() + " " : "N/A"), normalFont));
            patientDetails.addElement(new Paragraph("BpCount: " +
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
            table.setSpacingBefore(10f);
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

            if (medicines.size() != numberOfDays.size()) {
                int k = medicines.size();
                int ii = numberOfDays.size();
                for (int i = ii; i < k; i++) {
                    numberOfDays.add(null);
                }
            }

            for (int i = 0; i < medicines.size(); i++) {

                table.addCell(createCenteredCell(types.get(i), dataFont));
                table.addCell(createCenteredCell(medicines.get(i), dataFont));
                table.addCell(createCenteredCell(dosages.get(i), dataFont));
                table.addCell(createCenteredCell(schedules.get(i).toUpperCase(Locale.ROOT), dataFont));
                table.addCell(createCenteredCell(quantity.get(i).toString(), dataFont));
                Integer numberOfDaysValue = numberOfDays.get(i);
                table.addCell(createCenteredCell(numberOfDaysValue == null ? null : numberOfDaysValue.toString(), dataFont));
            }


            document.add(table);
            document.add(Chunk.NEWLINE);


            document.add(new Paragraph("Clinical Note: " + new String(prescription.getAppointment().getClinicalNote())));
            document.add(Chunk.NEWLINE);

            // *Rx Logo in the top-right corner*
            String rXPath = "C:/Users/DELL/Desktop/images/RxLogo.png";
            Image rxLogo = Image.getInstance(rXPath);
            rxLogo.scaleToFit(100f, 50f);

            float xPosition = document.getPageSize().getWidth() - rxLogo.getScaledWidth() - 450;
            float yPosition = document.getPageSize().getHeight() - rxLogo.getScaledHeight() - 410;
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
            logger.error("\n Error converting Prescription to PrescriptionDTO: " +ExceptionUtils.getStackTrace(e));
            return null;
        }
    }


    public CompletableFuture<Boolean> sendEmailWithAttachment(String to, String subject, String content, String fileName, byte[] attachmentData, String contentType) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            // Attach PDF
            ByteArrayDataSource dataSource = new ByteArrayDataSource(attachmentData, contentType);
            helper.addAttachment(fileName, dataSource);

            mailSender.send(message);
            //System.out.println("Email with attachment sent successfully to " + to);
            return CompletableFuture.completedFuture(true);
        } catch (MessagingException e) {
            logger.error("\n Error while sending email with attachment to " + to + ": " + ExceptionUtils.getStackTrace(e) );
            return CompletableFuture.completedFuture(false);
        }
    }

}