package com.fspl.medica_healthcare.templets;

import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Billing;
import com.fspl.medica_healthcare.models.HospitalizationInfo;
import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.border.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class PdfTemplate {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final PdfFont DEFAULT_FONT = initializeFont();

    private static final Logger log = LogManager.getLogger(PdfTemplate.class);

    private static PdfFont initializeFont() {
        try {
            return PdfFontFactory.createFont("Helvetica");
        } catch (Exception e) {
            log.error("Failed to initialize PDF font: {}"+e.getMessage(), e);
            return null;
        }
    }

    public byte[] createBillAsBytes(Billing bill) {
        if (bill == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            addPageBorder(pdf);

            document.add(new Paragraph(getHospitalName(bill) + " Bill")
                    .setFontSize(18)
                    .setFont(DEFAULT_FONT)
                    .setFontColor(Color.WHITE)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBackgroundColor(new DeviceRgb(200, 182, 255))
                    .setMarginBottom(10));

            document.add(createHeaderTable(bill));

            document.add(new Table(new float[]{570}).setBorder(new SolidBorder(Color.BLACK, 0f)).setMarginBottom(12f));

            document.add(createProfessionalFeesTable(bill));

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate pdf!!! : 'error' : {}"+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void addPageBorder(PdfDocument pdf) {
        PdfCanvas canvas = new PdfCanvas(pdf.addNewPage());
        Rectangle pageSize = pdf.getFirstPage().getPageSize();
        float margin = 34f;
        canvas.setLineWidth(1)
                .rectangle(margin, margin, pageSize.getWidth() - 2 * margin, pageSize.getHeight() - 2.1f * margin)
                .stroke();
    }

    private Table createHeaderTable(Billing bill) {
        Table table = new Table(new float[]{2, 2}).useAllAvailableWidth()
                .setFont(DEFAULT_FONT).setFontSize(12f).setMarginBottom(10);

        table.addCell(createCellNoBorder("Bill No:", String.valueOf(bill.getId())));
        table.addCell(createCellNoBorder("Bill Date:", LocalDate.now().format(DATE_FORMATTER)));
        table.addCell(createCellNoBorder("Patient Name:", getPatientName(bill)));
        table.addCell(createCellNoBorder("Date of Admission:", formatDate(getAdmissionDate(bill))));
        table.addCell(createCellNoBorder("Date of Discharge:", formatDate(getDischargeDate(bill))));
        table.addCell(createCellNoBorder("Treating Doctor:", getDoctorName(bill)));

        return table;
    }

    private Table createProfessionalFeesTable(Billing bill) {
        Table table = new Table(new float[]{1, 3, 1, 1, 1}).useAllAvailableWidth()
                .setFont(DEFAULT_FONT).setFontSize(12f).setKeepTogether(true);

        table.addHeaderCell(createHeaderCell("Sl. No."));
        table.addHeaderCell(createHeaderCell("Description"));
        table.addHeaderCell(createHeaderCell("Quantity"));
        table.addHeaderCell(createHeaderCell("Price / Unit"));
        table.addHeaderCell(createHeaderCell("Amount"));

        int count = 1;
        addRowToTable(table, count++, "Consultation Fee", "1", bill.getDoctorFee());

        HospitalizationInfo info = bill.getAppointment().getHospitalizationInfo();
        if (info != null) {
            addRowToTable(table, count++, "Room Charge (" + getRoomType(info) + ")",
                    String.valueOf(getTotalDaysAdmitted(info)), getRoomCharge(info));

            addRowToTable(table, count++, "Nursing Charge", "1", getNursingCharge(info));
            addRowToTable(table, count++, "Additional Charges", "1", getAdditionalCharges(info));
        }

        Cell combinedCell = new Cell(5, 3)
                .add(new Paragraph(""))
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(0.5f))
                .setBorderBottom(new SolidBorder(0.5f));
        table.addCell(combinedCell);
        addSummaryRows(table, bill);
        addClosingPhrase(table);
        return table;
    }


    private void addSummaryRows(Table table, Billing bill) {
        table.addCell(createCell("Sub Total:", true));
        table.addCell(createCell(getTotalAmount(bill).toString(), false));

        table.addCell(createCell("Discount:", false));
        table.addCell(createCell("0.00", false));

        table.addCell(createCell("Final Amount:", true)
                .setBackgroundColor(new DeviceRgb(123, 80, 233))
                .setFontColor(Color.WHITE));
        table.addCell(createCell(getTotalAmount(bill).toString(), false));

        table.addCell(createCell("Amount Paid:", false));
        table.addCell(createCell(getPaidAmount(bill).toString(), false));

        table.addCell(createCell("Balance:", false));
        table.addCell(createCell(getBalanceAmount(bill).toString(), false));
    }

    private void addClosingPhrase(Table table){
        Cell emptyRow = new Cell(1, 5)
                .add(new Paragraph(""))
                .setPaddingTop(40f)
                .setBorder(Border.NO_BORDER);
        table.addCell(emptyRow);
        System.out.println("1");

        Cell clientSignatureCell = new Cell(1, 2)
                .add(new Paragraph("Client's Signature").setFontSize(14))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER);

        System.out.println("hello");
        Cell middleSpace = new Cell(1, 1)
                .add(new Paragraph(""))
                .setBorder(Border.NO_BORDER);

        Cell businessSignatureCell = new Cell(1, 2)
                .add(new Paragraph("Business Signature").setFontSize(14))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER);


        Cell fillerRow = new Cell(1, 5)
                .add(new Paragraph(" "))
                .setPaddingTop(40f)
                .setBorder(Border.NO_BORDER);
        table.addCell(fillerRow);

        table.addCell(clientSignatureCell);
        table.addCell(middleSpace);
        table.addCell(businessSignatureCell);
    }

    private static Cell createCell(String content, boolean bold) {
        return new Cell().add(new Paragraph(content)).setTextAlignment(TextAlignment.CENTER).setBold();
    }

    private static Cell createHeaderCell(String content) {
        return new Cell().add(new Paragraph(content)).setBackgroundColor(new DeviceRgb(86, 3, 173))
                .setFontColor(Color.WHITE).setTextAlignment(TextAlignment.CENTER);
    }

    private static void addRowToTable(Table table, int serialNo, String description, String quantity, BigDecimal price) {
        BigDecimal total = price.multiply(new BigDecimal(quantity));
        table.addCell(createCell(String.valueOf(serialNo), false));
        table.addCell(createCell(description, false));
        table.addCell(createCell(quantity, false));
        table.addCell(createCell(price.toString(), false));
        table.addCell(createCell(total.toString(), false));
    }

    private static Cell createCellNoBorder(String label, String value) {
        Paragraph paragraph = new Paragraph(label + " " + value)
                .setTextAlignment(TextAlignment.LEFT)
                .setFont(DEFAULT_FONT)
                .setFontSize(12f);

        return new Cell().add(paragraph).setBorder(Border.NO_BORDER);
    }


    // Utility Methods
    private static String formatDate(LocalDate date) {
        return (date != null) ? date.format(DATE_FORMATTER) : "N/A";
    }

    private static String getHospitalName(Billing bill) {
        return Optional.ofNullable(bill.getAppointment().getHospital().getName()).orElse("Unknown");
    }

    private static String getPatientName(Billing bill) {
        return Optional.ofNullable(bill.getAppointment().getPatient().getName()).orElse("Unknown");
    }

    private static String getDoctorName(Billing bill) {
        return Optional.ofNullable(bill.getAppointment().getDoctor().getName())
                .orElse("Unknown");
    }

    private static String getRoomType(HospitalizationInfo info) {
        return Optional.ofNullable(info.getCatalog().getName().toString())
                .orElse("N/A");
    }

    private static BigDecimal getDoctorFee(Billing bill) {
        return Optional.ofNullable(bill.getDoctorFee()).orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getRoomCharge(HospitalizationInfo info) {
        return Optional.ofNullable(new BigDecimal(info.getCatalog().getFees()))
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getNursingCharge(HospitalizationInfo info) {
        return Optional.ofNullable(info.getNursingCharges())
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getAdditionalCharges(HospitalizationInfo info) {
        return Optional.ofNullable(info)
                .map(HospitalizationInfo::getAdditionalCharges)
                .orElse(BigDecimal.ZERO);
    }

    private static int getTotalDaysAdmitted(HospitalizationInfo info) {
        return Optional.ofNullable(info)
                .map(HospitalizationInfo::getTotalDaysAdmitted)
                .orElse(0);
    }

    private static BigDecimal getTotalAmount(Billing bill) {
        return Optional.ofNullable(bill.getTotalAmount()).orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getPaidAmount(Billing bill) {
        return Optional.ofNullable(bill.getPaidAmount()).orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getBalanceAmount(Billing bill) {
        return getTotalAmount(bill).subtract(getPaidAmount(bill));
    }

    private static LocalDate getAdmissionDate(Billing bill) {
        return Optional.ofNullable(bill.getAppointment())
                .map(Appointment::getHospitalizationInfo)
                .map(HospitalizationInfo::getDateOfAdmission)
                .orElse(null);
    }

    private static LocalDate getDischargeDate(Billing bill) {
        return Optional.ofNullable(bill.getAppointment())
                .map(Appointment::getHospitalizationInfo)
                .map(HospitalizationInfo::getDateOfDischarge)
                .orElse(null);
    }
}
