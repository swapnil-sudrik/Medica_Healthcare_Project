package com.fspl.medica_healthcare.templets;


import com.fspl.medica_healthcare.models.*;
import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.border.DashedBorder;
import com.itextpdf.layout.border.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class PdfTemplate {

    @Autowired
    private EncryptionUtil util;
//    Document document;
//    PdfDocument pdfDocument;
//    String pdfName;
//    float threecol = 190f;
//    float twocol = 285f;
//    float twocol150 = twocol + 150f;
//    float[] twocolumnWidth = {twocol150, twocol};
//    float[] threeColumnWidth = {threecol, threecol, threecol};
//    float[] fullwidth = {threecol * 3};
//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private Document document;
    private PdfDocument pdfDocument;
    private PdfWriter writer;
    private final float threecol = 190f;
    private final float twocol = 285f;
    private final float twocol150 = twocol + 150f;
    private final float[] twocolumnWidth = {twocol150, twocol};
    private final float[] threeColumnWidth = {threecol, threecol, threecol};
    private final float[] fullwidth = {threecol * 3};
    // First adjust your column width definitions at the class level
    private static final float PAGE_WIDTH = PageSize.A4.getWidth() - 80; // Account for margins
    private final float[] twocolumnsWidth = {PAGE_WIDTH / 2, PAGE_WIDTH / 2}; // Equal columns
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

//    public PdfTemplate(String pdfName) {
//        this.pdfName = pdfName;
//    }

//    public void createInvoiceAsBytes(Invoice invoice,Patient patient, Hospital hospital,List<Deposit> deposits) throws FileNotFoundException {
//        PdfTemplate pdfCreator = new PdfTemplate("hospital_invoice_test3.pdf");
//        pdfCreator.createDocument();
//        pdfCreator.createHeader(invoice);
//        pdfCreator.createPatientAndHospitalInfo(patient, hospital);
//        pdfCreator.createServiceCharges(invoice);
//        pdfCreator.createDepositsTable(deposits);
//        pdfCreator.createPaymentSummary(invoice);
//        pdfCreator.createTnc(Arrays.asList(
//                "Payment is due within 7 days.",
//                "Late payment may attract additional charges.",
//                "Contact invoice desk for any queries."
//        ));
//    }
//
//    private void createDocument() throws FileNotFoundException {
//        PdfWriter pdfWriter = new PdfWriter(pdfName);
//        pdfDocument = new PdfDocument(pdfWriter);
//        pdfDocument.setDefaultPageSize(PageSize.A4);
//        this.document = new Document(pdfDocument);
//    }

    public byte[] createInvoiceAsBytes(Invoice invoice, List<Deposit> deposits)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.writer = new PdfWriter(baos);

        try {
            createDocument();
            createHeader(invoice);
            createPatientAndHospitalInfo(invoice.getAppointment().getPatient(), invoice.getAppointment().getHospital());
            createServiceCharges(invoice);
            createDepositsTable(deposits);
            createPaymentSummary(invoice);
            createTnc(Arrays.asList(
                    "Payment is due within 7 days.",
                    "Late payment may attract additional charges.",
                    "Contact invoice desk for any queries."
            ));
            return baos.toByteArray();
        } finally {
            closeResources();
        }
    }

    private void createDocument() {
        pdfDocument = new PdfDocument(writer);
        pdfDocument.setDefaultPageSize(PageSize.A4);
        this.document = new Document(pdfDocument);
        document.setMargins(40, 40, 40, 40);
    }

    private void closeResources() throws IOException {
        if (document != null) {
            document.close();
        }
        if (writer != null) {
            writer.close();
        }
    }


    private void createHeader(Invoice invoice) {
        Table table = new Table(twocolumnWidth);
        table.addCell(new Cell().add("HOSPITAL BILL INVOICE").setFontSize(20f).setBorder(Border.NO_BORDER).setBold());

        Table nestedTable = new Table(new float[]{twocol / 2, twocol / 2});
        addHeaderRow(nestedTable, "Invoice Number:", String.valueOf(invoice.getId()));
        addHeaderRow(nestedTable, "Invoice Date:", formatDate(invoice.getCreatedDate()));
        addHeaderRow(nestedTable, "Due Date:", formatDate(invoice.getDueDate()));
        addHeaderRow(nestedTable, "Status:", invoice.getStatus().name());

        table.addCell(new Cell().add(nestedTable).setBorder(Border.NO_BORDER));
        document.add(table);
        document.add(createDivider());
    }

    private void addHeaderRow(Table table, String label, String value) {
        table.addCell(createCell(label, true, TextAlignment.LEFT));
        table.addCell(createCell(value, false, TextAlignment.LEFT));
    }

//    private void createPatientAndHospitalInfo(Patient patient, Hospital hospital) {
//        Table twoColTable = new Table(twocolumnWidth);
//        twoColTable.addCell(createBoldCell("Patient Details"));
//        twoColTable.addCell(createBoldCell("Hospital Details"));
//        document.add(twoColTable);
//
//        Table detailsTable = new Table(twocolumnWidth);
//        detailsTable.addCell(createInfoCell("Name: " + patient.getName()));
//        detailsTable.addCell(createInfoCell("Name: " + hospital.getName()));
//        detailsTable.addCell(createInfoCell("Contact: " + patient.getContactNumber()));
//        detailsTable.addCell(createInfoCell("Address: " + hospital.getAddress()));
//        document.add(detailsTable.setMarginBottom(15f));
//    }

//    private void createPatientAndHospitalInfo(Patient patient, Hospital hospital) {
//        // Create main table with two columns
//        Table mainTable = new Table(twocolumnWidth)
//                .setFixedLayout()  // Maintain column widths strictly
//                .setMarginBottom(15f);
//
//        // Patient Column
//        Cell patientCell = new Cell()
//                .add(new Paragraph("Patient Details").setBold())
//                .add(new Paragraph("Name: " + patient.getName()))
//                .add(new Paragraph("Contact: " + patient.getContactNumber()))
//                .setBorder(Border.NO_BORDER)
//                .setPadding(5)
//                .setKeepTogether(true);
//
//        // Hospital Column
//        Cell hospitalCell = new Cell()
//                .add(new Paragraph("Hospital Details").setBold())
//                .add(new Paragraph("Name: " + hospital.getName()))
//                .add(new Paragraph("Address: " + hospital.getAddress()))
//                .setBorder(Border.NO_BORDER)
//                .setPadding(5)
//                .setKeepTogether(true);
//
//        // Add cells to main table
//        mainTable.addCell(patientCell);
//        mainTable.addCell(hospitalCell);
//
//        document.add(mainTable);
//        document.add(createDivider());
//    }


    private void createPatientAndHospitalInfo(Patient patient, Hospital hospital) {
        // Main container table with proper width calculation
        Table mainTable = new Table(twocolumnsWidth)
                .setWidth(PAGE_WIDTH)
                .setFixedLayout()
                .setMarginBottom(15f);

        // Patient Column
        Cell patientCell = new Cell()
                .add(createSectionHeader("Patient Details"))
                .add(createWrappedDetail("Name: ", util.decrypt(new String(patient.getName())), twocolumnsWidth[0]))
                .add(createWrappedDetail("Contact: ", patient.getContactNumber(), twocolumnsWidth[0]))
                .add(createWrappedDetail("Email: ", util.decrypt(new String(patient.getEmailId())), twocolumnsWidth[0]))
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setVerticalAlignment(VerticalAlignment.TOP);

        String doctorName = patient.getCurrentDoctor() != null ? patient.getCurrentDoctor().getName() : "N/A";
        // Hospital Column
        Cell hospitalCell = new Cell()
                .add(createSectionHeader("Hospital Details"))
                .add(createWrappedDetail("Name: ", hospital.getName(), twocolumnsWidth[1]))
                .add(createWrappedDetail("Address: ", hospital.getAddress().toString(), twocolumnsWidth[1]))
                .add(createWrappedDetail("Doctor: ", patient.getCurrentDoctor() != null ? patient.getCurrentDoctor().getName() : "N/A", twocolumnsWidth[1]))
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setVerticalAlignment(VerticalAlignment.TOP);

        mainTable.addCell(patientCell);
        mainTable.addCell(hospitalCell);

        document.add(mainTable);
        document.add(createDivider());
    }

    // Modified helper method with dynamic width
    private Paragraph createWrappedDetail(String label, String value, float columnWidth) {
        float contentWidth = columnWidth - 16; // Account for padding
        Paragraph p = new Paragraph()
                .add(new Text(label).setBold())
                .add(new Text(breakLongText(value, 35))) // More reasonable line length
                .setFixedLeading(14f)
                .setMarginTop(4f)
                .setMarginBottom(4f)
                .setWidth(contentWidth)
                .setTextAlignment(TextAlignment.LEFT);
        return p;
    }

    // Improved text breaking with word boundary detection
    private String breakLongText(String text, int maxLineLength) {
        if (text == null) return "";
        if (text.length() <= maxLineLength) return text;

        StringBuilder sb = new StringBuilder();
        int lineLength = 0;

        for (String word : text.split(" ")) {
            if (lineLength + word.length() > maxLineLength) {
                sb.append("\n");
                lineLength = 0;
            }
            sb.append(word).append(" ");
            lineLength += word.length() + 1;
        }
        return sb.toString().trim();
    }

    private Paragraph createSectionHeader(String text) {
        return new Paragraph(text)
                .setBold()
                .setFontColor(new DeviceRgb(41, 128, 185)) // Blue color
                .setMarginBottom(8f)
                .setBorderBottom(new SolidBorder(1.5f))
                .setPaddingBottom(4f);
    }

    private void createServiceCharges(Invoice invoice) {
        document.add(new Paragraph("Service Charges").setBold());

        Table chargesTable = new Table(threeColumnWidth);

        // Header Row
        chargesTable.addCell(createHeaderCellWithBackground("Description"));
        chargesTable.addCell(createHeaderCellWithBackground("Days/Qty"));
        chargesTable.addCell(createHeaderCellWithBackground("Amount"));

        // Doctor Fee
        addChargeRow(chargesTable, "Consultation Fee", 1, invoice.getDoctorFee());

        // Hospitalization Charges
        if (invoice.getHospitalizationInfo() != null) {
            HospitalizationInfo hi = invoice.getHospitalizationInfo();
            Catalog roomType = hi.getCatalog();

            addChargeRow(chargesTable, roomType.getCategory() + " (" + roomType.getName() + ")",
                    hi.getTotalDaysAdmitted(), roomType.getFees() * hi.getTotalDaysAdmitted());
            addChargeRow(chargesTable, "Nursing Charges", 1, hi.getNursingCharges());
            addChargeRow(chargesTable, "Canteen Charges", 1, hi.getCanteenCharges());
            addChargeRow(chargesTable, "Additional Charges", 1, hi.getAdditionalCharges());
        }

        document.add(chargesTable);
        document.add(createDivider());
    }


//    public void createServiceCharges(Invoice invoice) {
//        document.add(new Paragraph("Service Charges").setBold());
//        Table chargesTable = new Table(threeColumnWidth);
//        chargesTable.setBackgroundColor(Color.BLACK, 0.7f);
//
//        chargesTable.addCell(createHeaderCell("Description"));
//        chargesTable.addCell(createHeaderCell("Days/Qty"));
//        chargesTable.addCell(createHeaderCell("Amount"));
//
//        // Doctor Fee
//        addChargeRow(chargesTable, "Consultation Fee", 1, invoice.getDoctorFee());
//
//        // Hospitalization Charges
//        if(invoice.getHospitalizationInfo() != null) {
//            HospitalizationInfo hi = invoice.getHospitalizationInfo();
//            Catalog roomType = hi.getCatalog();
//
//            addChargeRow(chargesTable, roomType.getCategory() + " (" + roomType.getName() + ")",
//                    hi.getTotalDaysAdmitted(), roomType.getFees() * hi.getTotalDaysAdmitted());
//            addChargeRow(chargesTable, "Nursing Charges", 1, hi.getNursingCharges());
//            addChargeRow(chargesTable, "Canteen Charges", 1, hi.getCanteenCharges());
//            addChargeRow(chargesTable, "Additional Charges", 1, hi.getAdditionalCharges());
//        }
//
//        document.add(chargesTable);
//        document.add(createDivider());
//    }

//    public void createDepositsTable(List<Deposit> deposits) {
//        document.add(new Paragraph("Payments Made").setBold());
//        Table depositsTable = new Table(new float[]{threecol, threecol, threecol});
//        depositsTable.setBackgroundColor(Color.BLACK, 0.7f);
//
//        depositsTable.addCell(createHeaderCell("Date"));
//        depositsTable.addCell(createHeaderCell("Payment Mode"));
//        depositsTable.addCell(createHeaderCell("Amount"));
//
//        deposits.forEach(deposit -> {
//            depositsTable.addCell(createCell(formatDate(deposit.getCreatedDate()), false, TextAlignment.LEFT));
//            depositsTable.addCell(createCell(deposit.getPaymentMode().name(), false, TextAlignment.LEFT));
//            depositsTable.addCell(createCell(String.valueOf(deposit.getDepositAmount()), false, TextAlignment.RIGHT));
//        });
//
//        document.add(depositsTable);
//        document.add(createDivider());
//    }

    private void createDepositsTable(List<Deposit> deposits) {
        document.add(new Paragraph("Payments Made").setBold());

        Table depositsTable = new Table(new float[]{threecol, threecol, threecol});

        // Header Row
        depositsTable.addCell(createHeaderCellWithBackground("Date"));
        depositsTable.addCell(createHeaderCellWithBackground("Payment Mode"));
        depositsTable.addCell(createHeaderCellWithBackground("Amount"));

        // Data Rows
        if(deposits !=null) {
            deposits.forEach(deposit -> {
                depositsTable.addCell(createCell(formatDate(deposit.getCreatedDate()), false, TextAlignment.LEFT));
                depositsTable.addCell(createCell(deposit.getPaymentMode().name(), false, TextAlignment.LEFT));
                depositsTable.addCell(createCell(String.valueOf(deposit.getDepositAmount()), false, TextAlignment.RIGHT));
            });
        }

        document.add(depositsTable);
        document.add(createDivider());
    }


    private void createPaymentSummary(Invoice invoice) {
        Table summaryTable = new Table(new float[]{twocol, threecol});
        summaryTable.addCell(createCell("Total Amount:", true, TextAlignment.RIGHT));
        summaryTable.addCell(createCell(String.valueOf(invoice.getTotalAmount()), false, TextAlignment.RIGHT));
        summaryTable.addCell(createCell("Total Payments:", true, TextAlignment.RIGHT));
        summaryTable.addCell(createCell(String.valueOf(invoice.getPaidAmount()), false, TextAlignment.RIGHT));
        summaryTable.addCell(createCell("Balance Due:", true, TextAlignment.RIGHT));
        summaryTable.addCell(createCell(String.valueOf(invoice.getBalanceAmount()), false, TextAlignment.RIGHT));

        document.add(summaryTable.setMarginBottom(15f));
        document.add(new Paragraph("Payment Status: " + invoice.getStatus().name()).setBold());
    }

    private void createTnc(List<String> tncList) {
        Table tncTable = new Table(fullwidth);
        tncTable.addCell(createBoldCell("Terms & Conditions"));
        tncList.forEach(tnc -> tncTable.addCell(createCell(tnc, false, TextAlignment.LEFT)));
        document.add(tncTable);
        document.close();
    }

    // Helper methods
    private Cell createHeaderCell(String text) {
        return new Cell().add(text)
                .setBold()
                .setFontColor(Color.WHITE)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createHeaderCellWithBackground(String text) {
        return new Cell()
                .add(text)
                .setBackgroundColor(Color.BLACK, 0.7f)
                .setFontColor(Color.WHITE)
                .setBold()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createCell(String text, boolean isBold, TextAlignment alignment) {
        Cell cell = new Cell().add(text)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(alignment);
        return isBold ? cell.setBold() : cell;
    }

    private Cell createBoldCell(String text) {
        return new Cell().add(text).setBold().setBorder(Border.NO_BORDER);
    }

    private Cell createInfoCell(String text) {
        return new Cell().add(text).setBorder(Border.NO_BORDER);
    }

    private Table createDivider() {
        return new Table(fullwidth).setBorder(new DashedBorder(Color.GRAY, 0.5f));
    }

    private void addChargeRow(Table table, String desc, int days, double amount) {
        table.addCell(createCell(desc, false, TextAlignment.LEFT));
        table.addCell(createCell(String.valueOf(days), false, TextAlignment.CENTER));
        table.addCell(createCell(String.valueOf(amount), false, TextAlignment.RIGHT));
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "N/A";
    }
}


// latest updated ------------------------------------------------------------------------


//import com.fspl.medica_healthcare.models.Appointment;
//import com.fspl.medica_healthcare.models.Invoice;
//import com.fspl.medica_healthcare.models.HospitalizationInfo;
//import com.itextpdf.kernel.color.Color;
//import com.itextpdf.kernel.color.DeviceRgb;
//import com.itextpdf.kernel.font.PdfFont;
//import com.itextpdf.kernel.font.PdfFontFactory;
//import com.itextpdf.kernel.geom.Rectangle;
//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
//import com.itextpdf.layout.Document;
//import com.itextpdf.layout.border.Border;
//import com.itextpdf.layout.border.SolidBorder;
//import com.itextpdf.layout.element.Cell;
//import com.itextpdf.layout.element.Paragraph;
//import com.itextpdf.layout.element.Table;
//import com.itextpdf.layout.property.TextAlignment;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayOutputStream;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.Optional;
//
//@Service
//public class PdfTemplate {
//
//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
/// /    private final PdfFont DEFAULT_FONT = initializeFont();
//
//    private static final Logger log = LogManager.getLogger(PdfTemplate.class);
//
//    private PdfFont initializeFont() {
//        try {
//            return PdfFontFactory.createFont("Helvetica");
//        } catch (Exception e) {
//            log.error("Failed to initialize PDF font: {}"+e.getMessage(), e);
//            return null;
//        }
//    }
//
//    public byte[] createInvoiceAsBytes(Invoice invoice) {
//        if (invoice == null) {
//            return null;
//        }
//
//        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        )
//        {
//            PdfWriter writer = new PdfWriter(outputStream);
//            PdfDocument pdf = new PdfDocument(writer);
//            Document document = new Document(pdf);
//
//            PdfFont defaultFont = initializeFont();
//            addPageBorder(pdf);
//
//            document.add(new Paragraph(getHospitalName(invoice) + " Invoice")
//                    .setFontSize(18)
////                    .setFont(DEFAULT_FONT)
//                    .setFont(defaultFont)
//                    .setFontColor(Color.WHITE)
//                    .setBold()
//                    .setTextAlignment(TextAlignment.CENTER)
//                    .setBackgroundColor(new DeviceRgb(200, 182, 255))
//                    .setMarginBottom(10));
//
//            document.add(createHeaderTable(invoice,defaultFont));
//
//            document.add(new Table(new float[]{570}).setBorder(new SolidBorder(Color.BLACK, 0f)).setMarginBottom(12f));
//
//            document.add(createProfessionalFeesTable(invoice,defaultFont));
//
//            document.close();
//            pdf.close();
//            writer.close();
//
//            return outputStream.toByteArray();
//
//        } catch (Exception e) {
//            System.out.println("heloo");
//            log.error("Failed to generate pdf!!! : 'error' : {}"+e.getMessage());
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    private void addPageBorder(PdfDocument pdf) {
//        PdfCanvas canvas = new PdfCanvas(pdf.addNewPage());
//        Rectangle pageSize = pdf.getFirstPage().getPageSize();
//        float margin = 34f;
//        canvas.setLineWidth(1)
//                .rectangle(margin, margin, pageSize.getWidth() - 2 * margin, pageSize.getHeight() - 2.1f * margin)
//                .stroke();
//
//    }
//
//    private Table createHeaderTable(Invoice invoice,PdfFont defaultFont) {
//        Table table = new Table(new float[]{2, 2}).useAllAvailableWidth()
//                .setFont(defaultFont).setFontSize(12f).setMarginBottom(10);
//
//        table.addCell(createCellNoBorder("Invoice No:", String.valueOf(invoice.getId()),defaultFont));
//        table.addCell(createCellNoBorder("Invoice Date:", LocalDate.now().format(DATE_FORMATTER),defaultFont));
//        table.addCell(createCellNoBorder("Patient Name:", getPatientName(invoice),defaultFont));
//        table.addCell(createCellNoBorder("Date of Admission:", formatDate(getAdmissionDate(invoice)),defaultFont));
//        table.addCell(createCellNoBorder("Date of Discharge:", formatDate(getDischargeDate(invoice)),defaultFont));
//        table.addCell(createCellNoBorder("Treating Doctor:", getDoctorName(invoice),defaultFont));
//
//        return table;
//    }
//
//    private Table createProfessionalFeesTable(Invoice invoice,PdfFont defaultFont) {
//        Table table = new Table(new float[]{1, 3, 1, 1, 1}).useAllAvailableWidth()
//                .setFont(defaultFont).setFontSize(12f).setKeepTogether(true);
//
//        table.addHeaderCell(createHeaderCell("Sl. No."));
//        table.addHeaderCell(createHeaderCell("Description"));
//        table.addHeaderCell(createHeaderCell("Quantity"));
//        table.addHeaderCell(createHeaderCell("Price / Unit"));
//        table.addHeaderCell(createHeaderCell("Amount"));
//
//        int count = 1;
//        addRowToTable(table, count++, "Consultation Fee", "1", invoice.getDoctorFee());
//
//        HospitalizationInfo info = invoice.getAppointment().getHospitalizationInfo();
//        if (info != null) {
//            addRowToTable(table, count++, "Room Charge (" + getRoomType(info) + ")",
//                    String.valueOf(getTotalDaysAdmitted(info)), getRoomCharge(info));
//
//            addRowToTable(table, count++, "Nursing Charge", "1", getNursingCharge(info));
//            addRowToTable(table, count++, "Additional Charges", "1", getAdditionalCharges(info));
//        }
//
//        Cell combinedCell = new Cell(5, 3)
//                .add(new Paragraph(""))
//                .setBorder(Border.NO_BORDER)
//                .setBorderLeft(new SolidBorder(0.5f))
//                .setBorderBottom(new SolidBorder(0.5f));
//        table.addCell(combinedCell);
//        addSummaryRows(table, invoice);
//        addClosingPhrase(table);
//        return table;
//    }
//
//
//    private void addSummaryRows(Table table, Invoice invoice) {
//        table.addCell(createCell("Sub Total:", true));
//        table.addCell(createCell(String.valueOf(getTotalAmount(invoice)), false));
//
//        table.addCell(createCell("Discount:", false));
//        table.addCell(createCell("0.00", false));
//
//        table.addCell(createCell("Final Amount:", true)
//                .setBackgroundColor(new DeviceRgb(123, 80, 233))
//                .setFontColor(Color.WHITE));
//        table.addCell(createCell(String.valueOf(getTotalAmount(invoice)), false));
//
//        table.addCell(createCell("Amount Paid:", false));
//        table.addCell(createCell(String.valueOf(getPaidAmount(invoice)), false));
//
//        table.addCell(createCell("Balance:", false));
//        table.addCell(createCell(String.valueOf(getBalanceAmount(invoice)), false));
//    }
//
//    private void addClosingPhrase(Table table){
//        Cell emptyRow = new Cell(1, 5)
//                .add(new Paragraph(""))
//                .setPaddingTop(40f)
//                .setBorder(Border.NO_BORDER);
//        table.addCell(emptyRow);
//        System.out.println("1");
//
//        Cell clientSignatureCell = new Cell(1, 2)
//                .add(new Paragraph("Client's Signature").setFontSize(14))
//                .setTextAlignment(TextAlignment.CENTER)
//                .setBorder(Border.NO_BORDER);
//
//        System.out.println("hello");
//        Cell middleSpace = new Cell(1, 1)
//                .add(new Paragraph(""))
//                .setBorder(Border.NO_BORDER);
//
//        Cell businessSignatureCell = new Cell(1, 2)
//                .add(new Paragraph("Business Signature").setFontSize(14))
//                .setTextAlignment(TextAlignment.CENTER)
//                .setBorder(Border.NO_BORDER);
//
//
//        Cell fillerRow = new Cell(1, 5)
//                .add(new Paragraph(" "))
//                .setPaddingTop(40f)
//                .setBorder(Border.NO_BORDER);
//        table.addCell(fillerRow);
//
//        table.addCell(clientSignatureCell);
//        table.addCell(middleSpace);
//        table.addCell(businessSignatureCell);
//    }
//
//    private static Cell createCell(String content, boolean bold) {
//        return new Cell().add(new Paragraph(content)).setTextAlignment(TextAlignment.CENTER).setBold();
//    }
//
//    private static Cell createHeaderCell(String content) {
//        return new Cell().add(new Paragraph(content)).setBackgroundColor(new DeviceRgb(86, 3, 173))
//                .setFontColor(Color.WHITE).setTextAlignment(TextAlignment.CENTER);
//    }
//
//    private static void addRowToTable(Table table, int serialNo, String description, String quantity, double price) {
////        double total = price.multiply(new BigDecimal(quantity));
//        double total = price*Double.valueOf(quantity);
//        table.addCell(createCell(String.valueOf(serialNo), false));
//        table.addCell(createCell(description, false));
//        table.addCell(createCell(quantity, false));
//        table.addCell(createCell(String.valueOf(price), false));
//        table.addCell(createCell(String.valueOf(total), false));
//    }
//
//    private  Cell createCellNoBorder(String label, String value,PdfFont defaultFont) {
//        Paragraph paragraph = new Paragraph(label + " " + value)
//                .setTextAlignment(TextAlignment.LEFT)
//                .setFont(defaultFont)
//                .setFontSize(12f);
//
//        return new Cell().add(paragraph).setBorder(Border.NO_BORDER);
//    }
//
//
//    // Utility Methods
//    private static String formatDate(LocalDate date) {
//        return (date != null) ? date.format(DATE_FORMATTER) : "N/A";
//    }
//
//    private static String getHospitalName(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment().getHospital().getName()).orElse("Unknown");
//    }
//
//    private static String getPatientName(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment().getPatient().getName()).orElse("Unknown");
//    }
//
//    private static String getDoctorName(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment().getDoctor().getName())
//                .orElse("Unknown");
//    }
//
//    private static String getRoomType(HospitalizationInfo info) {
//        return Optional.ofNullable(info.getCatalog().getName().toString())
//                .orElse("N/A");
//    }
//
//    private static double getDoctorFee(Invoice invoice) {
//        return invoice.getDoctorFee();
//    }
//
//    private static double getRoomCharge(HospitalizationInfo info) {
//        return info.getCatalog().getFees();
//    }
//
//    private static double getNursingCharge(HospitalizationInfo info) {
//        return info.getNursingCharges();
//    }
//
//    private static double getAdditionalCharges(HospitalizationInfo info) {
//        return info.getAdditionalCharges();
//    }
//
//    private static int getTotalDaysAdmitted(HospitalizationInfo info) {
//        return Optional.ofNullable(info)
//                .map(HospitalizationInfo::getTotalDaysAdmitted)
//                .orElse(0);
//    }
//
//    private static double getTotalAmount(Invoice invoice) {
//        return invoice.getTotalAmount();
//    }
//
//    private static double getPaidAmount(Invoice invoice) {
//        return invoice.getPaidAmount();
//    }
//
//    private static double getBalanceAmount(Invoice invoice) {
//        return getTotalAmount(invoice) - getPaidAmount(invoice);
//    }
//
//    private static LocalDate getAdmissionDate(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment())
//                .map(Appointment::getHospitalizationInfo)
//                .map(HospitalizationInfo::getDateOfAdmission)
//                .orElse(null);
//    }
//
//    private static LocalDate getDischargeDate(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment())
//                .map(Appointment::getHospitalizationInfo)
//                .map(HospitalizationInfo::getDateOfDischarge)
//                .orElse(null);
//    }
//}


//==================================================================================================================


//package com.fspl.medica_healthcare.templets;
//
//import com.fspl.medica_healthcare.models.Appointment;
//import com.fspl.medica_healthcare.models.Invoice;
//import com.fspl.medica_healthcare.models.HospitalizationInfo;
//import com.itextpdf.kernel.color.Color;
//import com.itextpdf.kernel.color.DeviceRgb;
//import com.itextpdf.kernel.font.PdfFont;
//import com.itextpdf.kernel.font.PdfFontFactory;
//import com.itextpdf.kernel.geom.Rectangle;
//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfWriter;
//import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
//import com.itextpdf.layout.Document;
//import com.itextpdf.layout.border.Border;
//import com.itextpdf.layout.border.SolidBorder;
//import com.itextpdf.layout.element.Cell;
//import com.itextpdf.layout.element.Paragraph;
//import com.itextpdf.layout.element.Table;
//import com.itextpdf.layout.property.TextAlignment;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayOutputStream;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.Optional;
//
//@Service
//public class PdfTemplate {
//
//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
//    private static final PdfFont DEFAULT_FONT = initializeFont();
//
//    private static final Logger log = LogManager.getLogger(PdfTemplate.class);
//
//    private static PdfFont initializeFont() {
//        try {
//            return PdfFontFactory.createFont("Helvetica");
//        } catch (Exception e) {
//            log.error("Failed to initialize PDF font: {}"+e.getMessage(), e);
//            return null;
//        }
//    }
//
//    public byte[] createInvoiceAsBytes(Invoice invoice) {
//        if (invoice == null) {
//            return null;
//        }
//
//        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//             PdfWriter writer = new PdfWriter(outputStream);
//             PdfDocument pdf = new PdfDocument(writer);
//             Document document = new Document(pdf)) {
//
//            addPageBorder(pdf);
//
//            document.add(new Paragraph(getHospitalName(invoice) + " Invoice")
//                    .setFontSize(18)
//                    .setFont(DEFAULT_FONT)
//                    .setFontColor(Color.WHITE)
//                    .setBold()
//                    .setTextAlignment(TextAlignment.CENTER)
//                    .setBackgroundColor(new DeviceRgb(200, 182, 255))
//                    .setMarginBottom(10));
//
//            document.add(createHeaderTable(invoice));
//
//            document.add(new Table(new float[]{570}).setBorder(new SolidBorder(Color.BLACK, 0f)).setMarginBottom(12f));
//
//            document.add(createProfessionalFeesTable(invoice));
//
//            document.close();
//
//            return outputStream.toByteArray();
//
//        } catch (Exception e) {
//            log.error("Failed to generate pdf!!! : 'error' : {}"+e.getMessage());
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    private void addPageBorder(PdfDocument pdf) {
//        PdfCanvas canvas = new PdfCanvas(pdf.addNewPage());
//        Rectangle pageSize = pdf.getFirstPage().getPageSize();
//        float margin = 34f;
//        canvas.setLineWidth(1)
//                .rectangle(margin, margin, pageSize.getWidth() - 2 * margin, pageSize.getHeight() - 2.1f * margin)
//                .stroke();
//    }
//
//    private Table createHeaderTable(Invoice invoice) {
//        Table table = new Table(new float[]{2, 2}).useAllAvailableWidth()
//                .setFont(DEFAULT_FONT).setFontSize(12f).setMarginBottom(10);
//
//        table.addCell(createCellNoBorder("Invoice No:", String.valueOf(invoice.getId())));
//        table.addCell(createCellNoBorder("Invoice Date:", LocalDate.now().format(DATE_FORMATTER)));
//        table.addCell(createCellNoBorder("Patient Name:", getPatientName(invoice)));
//        table.addCell(createCellNoBorder("Date of Admission:", formatDate(getAdmissionDate(invoice))));
//        table.addCell(createCellNoBorder("Date of Discharge:", formatDate(getDischargeDate(invoice))));
//        table.addCell(createCellNoBorder("Treating Doctor:", getDoctorName(invoice)));
//
//        return table;
//    }
//
//    private Table createProfessionalFeesTable(Invoice invoice) {
//        Table table = new Table(new float[]{1, 3, 1, 1, 1}).useAllAvailableWidth()
//                .setFont(DEFAULT_FONT).setFontSize(12f).setKeepTogether(true);
//
//        table.addHeaderCell(createHeaderCell("Sl. No."));
//        table.addHeaderCell(createHeaderCell("Description"));
//        table.addHeaderCell(createHeaderCell("Quantity"));
//        table.addHeaderCell(createHeaderCell("Price / Unit"));
//        table.addHeaderCell(createHeaderCell("Amount"));
//
//        int count = 1;
//        addRowToTable(table, count++, "Consultation Fee", "1", invoice.getDoctorFee());
//
//        HospitalizationInfo info = invoice.getAppointment().getHospitalizationInfo();
//        if (info != null) {
//            addRowToTable(table, count++, "Room Charge (" + getRoomType(info) + ")",
//                    String.valueOf(getTotalDaysAdmitted(info)), getRoomCharge(info));
//
//            addRowToTable(table, count++, "Nursing Charge", "1", getNursingCharge(info));
//            addRowToTable(table, count++, "Additional Charges", "1", getAdditionalCharges(info));
//        }
//
//        Cell combinedCell = new Cell(5, 3)
//                .add(new Paragraph(""))
//                .setBorder(Border.NO_BORDER)
//                .setBorderLeft(new SolidBorder(0.5f))
//                .setBorderBottom(new SolidBorder(0.5f));
//        table.addCell(combinedCell);
//        addSummaryRows(table, invoice);
//        addClosingPhrase(table);
//        return table;
//    }
//
//
//    private void addSummaryRows(Table table, Invoice invoice) {
//        table.addCell(createCell("Sub Total:", true));
//        table.addCell(createCell(getTotalAmount(invoice).toString(), false));
//
//        table.addCell(createCell("Discount:", false));
//        table.addCell(createCell("0.00", false));
//
//        table.addCell(createCell("Final Amount:", true)
//                .setBackgroundColor(new DeviceRgb(123, 80, 233))
//                .setFontColor(Color.WHITE));
//        table.addCell(createCell(getTotalAmount(invoice).toString(), false));
//
//        table.addCell(createCell("Amount Paid:", false));
//        table.addCell(createCell(getPaidAmount(invoice).toString(), false));
//
//        table.addCell(createCell("Balance:", false));
//        table.addCell(createCell(getBalanceAmount(invoice).toString(), false));
//    }
//
//    private void addClosingPhrase(Table table){
//        Cell emptyRow = new Cell(1, 5)
//                .add(new Paragraph(""))
//                .setPaddingTop(40f)
//                .setBorder(Border.NO_BORDER);
//        table.addCell(emptyRow);
//        System.out.println("1");
//
//        Cell clientSignatureCell = new Cell(1, 2)
//                .add(new Paragraph("Client's Signature").setFontSize(14))
//                .setTextAlignment(TextAlignment.CENTER)
//                .setBorder(Border.NO_BORDER);
//
//        System.out.println("hello");
//        Cell middleSpace = new Cell(1, 1)
//                .add(new Paragraph(""))
//                .setBorder(Border.NO_BORDER);
//
//        Cell businessSignatureCell = new Cell(1, 2)
//                .add(new Paragraph("Business Signature").setFontSize(14))
//                .setTextAlignment(TextAlignment.CENTER)
//                .setBorder(Border.NO_BORDER);
//
//
//        Cell fillerRow = new Cell(1, 5)
//                .add(new Paragraph(" "))
//                .setPaddingTop(40f)
//                .setBorder(Border.NO_BORDER);
//        table.addCell(fillerRow);
//
//        table.addCell(clientSignatureCell);
//        table.addCell(middleSpace);
//        table.addCell(businessSignatureCell);
//    }
//
//    private static Cell createCell(String content, boolean bold) {
//        return new Cell().add(new Paragraph(content)).setTextAlignment(TextAlignment.CENTER).setBold();
//    }
//
//    private static Cell createHeaderCell(String content) {
//        return new Cell().add(new Paragraph(content)).setBackgroundColor(new DeviceRgb(86, 3, 173))
//                .setFontColor(Color.WHITE).setTextAlignment(TextAlignment.CENTER);
//    }
//
//    private static void addRowToTable(Table table, int serialNo, String description, String quantity, BigDecimal price) {
//        BigDecimal total = price.multiply(new BigDecimal(quantity));
//        table.addCell(createCell(String.valueOf(serialNo), false));
//        table.addCell(createCell(description, false));
//        table.addCell(createCell(quantity, false));
//        table.addCell(createCell(price.toString(), false));
//        table.addCell(createCell(total.toString(), false));
//    }
//
//    private static Cell createCellNoBorder(String label, String value) {
//        Paragraph paragraph = new Paragraph(label + " " + value)
//                .setTextAlignment(TextAlignment.LEFT)
//                .setFont(DEFAULT_FONT)
//                .setFontSize(12f);
//
//        return new Cell().add(paragraph).setBorder(Border.NO_BORDER);
//    }
//
//
//    // Utility Methods
//    private static String formatDate(LocalDate date) {
//        return (date != null) ? date.format(DATE_FORMATTER) : "N/A";
//    }
//
//    private static String getHospitalName(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment().getHospital().getName()).orElse("Unknown");
//    }
//
//    private static String getPatientName(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment().getPatient().getName()).orElse("Unknown");
//    }
//
//    private static String getDoctorName(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment().getDoctor().getName())
//                .orElse("Unknown");
//    }
//
//    private static String getRoomType(HospitalizationInfo info) {
//        return Optional.ofNullable(info.getCatalog().getName().toString())
//                .orElse("N/A");
//    }
//
//    private static BigDecimal getDoctorFee(Invoice invoice) {
//        return Optional.ofNullable(invoice.getDoctorFee()).orElse(BigDecimal.ZERO);
//    }
//
//    private static BigDecimal getRoomCharge(HospitalizationInfo info) {
//        return Optional.ofNullable(new BigDecimal(info.getCatalog().getFees()))
//                .orElse(BigDecimal.ZERO);
//    }
//
//    private static BigDecimal getNursingCharge(HospitalizationInfo info) {
//        return Optional.ofNullable(info.getNursingCharges())
//                .orElse(BigDecimal.ZERO);
//    }
//
//    private static BigDecimal getAdditionalCharges(HospitalizationInfo info) {
//        return Optional.ofNullable(info)
//                .map(HospitalizationInfo::getAdditionalCharges)
//                .orElse(BigDecimal.ZERO);
//    }
//
//    private static int getTotalDaysAdmitted(HospitalizationInfo info) {
//        return Optional.ofNullable(info)
//                .map(HospitalizationInfo::getTotalDaysAdmitted)
//                .orElse(0);
//    }
//
//    private static BigDecimal getTotalAmount(Invoice invoice) {
//        return Optional.ofNullable(invoice.getTotalAmount()).orElse(BigDecimal.ZERO);
//    }
//
//    private static BigDecimal getPaidAmount(Invoice invoice) {
//        return Optional.ofNullable(invoice.getPaidAmount()).orElse(BigDecimal.ZERO);
//    }
//
//    private static BigDecimal getBalanceAmount(Invoice invoice) {
//        return getTotalAmount(invoice).subtract(getPaidAmount(invoice));
//    }
//
//    private static LocalDate getAdmissionDate(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment())
//                .map(Appointment::getHospitalizationInfo)
//                .map(HospitalizationInfo::getDateOfAdmission)
//                .orElse(null);
//    }
//
//    private static LocalDate getDischargeDate(Invoice invoice) {
//        return Optional.ofNullable(invoice.getAppointment())
//                .map(Appointment::getHospitalizationInfo)
//                .map(HospitalizationInfo::getDateOfDischarge)
//                .orElse(null);
//    }
//}
