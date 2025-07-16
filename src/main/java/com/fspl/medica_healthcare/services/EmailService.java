package com.fspl.medica_healthcare.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {


    @Autowired
    private JavaMailSender mailSender;


    @Async
    public void sendEmail(String to, String subject, String templete) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(templete, true); // Use 'true' for HTML content

            mailSender.send(message);
            System.out.println("Email sent successfully.");
        } catch (MessagingException e) {

            System.err.println("Error while sending email: " + e.getMessage());
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String content, String fileName, byte[] attachmentData) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            // Attach PDF
            ByteArrayDataSource dataSource = new ByteArrayDataSource(attachmentData, "application/pdf");
            helper.addAttachment(fileName, dataSource);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error sending email with attachment: " + e.getMessage());

        }
    }

    @Async
    public CompletableFuture<String> sendEmailWithAttachmentForBill(String recipientEmail, String subject, String htmlContent, byte[] attachment, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            helper.addAttachment(fileName, new ByteArrayResource(attachment));

            mailSender.send(message);
            System.out.println("Mail send successfully....");
            return CompletableFuture.completedFuture("Success");
        } catch (MessagingException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture("Failed to send email: " + e.getMessage());
        }
    }
}
