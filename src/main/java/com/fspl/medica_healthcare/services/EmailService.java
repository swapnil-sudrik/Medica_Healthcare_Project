//package com.fspl.medica_healthcare.services;
//
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.MimeMessage;
//import jakarta.mail.util.ByteArrayDataSource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.CompletableFuture;
//
//@Service
//public class EmailService {
//
//
//    @Autowired
//    private JavaMailSender mailSender;
//
//
//    @Async
//    public void sendEmail(String to, String subject, String templete) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//            helper.setTo(to);
//            helper.setSubject(subject);
//            helper.setText(templete, true); // Use 'true' for HTML content
//
//            mailSender.send(message);
//            System.out.println("Email sent successfully.");
//        } catch (MessagingException e) {
//
//            System.err.println("Error while sending email: " + e.getMessage());
//        }
//    }
//
//    public void sendEmailWithAttachment(String to, String subject, String content, String fileName, byte[] attachmentData) {
//        MimeMessage message = mailSender.createMimeMessage();
//
//        try {
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//            helper.setTo(to);
//            helper.setSubject(subject);
//            helper.setText(content, true);
//
//            // Attach PDF
//            ByteArrayDataSource dataSource = new ByteArrayDataSource(attachmentData, "application/pdf");
//            helper.addAttachment(fileName, dataSource);
//
//            mailSender.send(message);
//        } catch (MessagingException e) {
//            e.printStackTrace();
//            throw new RuntimeException("Error sending email with attachment: " + e.getMessage());
//
//        }
//    }
//
//    @Async
//    public CompletableFuture<String> sendEmailWithAttachmentForInvoice(String recipientEmail, String subject, String htmlContent, byte[] attachment, String fileName) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//            helper.setTo(recipientEmail);
//            helper.setSubject(subject);
//            helper.setText(htmlContent, true);
//
//            helper.addAttachment(fileName, new ByteArrayResource(attachment));
//
//            mailSender.send(message);
//            System.out.println("Mail send successfully....");
//            return CompletableFuture.completedFuture("Success");
//        } catch (MessagingException e) {
//            e.printStackTrace();
////            log.error("Failed to send email to {}. Error: {}", recipientEmail, e.getMessage(), e);
//            return CompletableFuture.completedFuture("Failed");
//        }
//    }
//}
package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.Subscription;
import com.fspl.medica_healthcare.repositories.SubscribedEmailRepository;
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


public class
EmailService {


    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SubscribedEmailRepository subscribedEmailRepository;

    @Async
    public void sendEmail(String to, String subject, String template) {
//        if (isUserUnsubscribed(to)) {
//            System.out.println("User " + to + " has unsubscribed. Skipping email send.");
//            return; // Skip sending the email if the user is unsubscribed
//        }
        if (isUserUnsubscribed(to)) {
            System.out.println("User " + to + " has unsubscribed. Skipping email send.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);

            // Generate the appropriate link based on subscription status
            String subscriptionLink;
            String buttonText;

            if (isUserUnsubscribed(to)) {
                // If the user is unsubscribed, show "Subscribe" button
                subscriptionLink = generateSubscribeLink(to);
                buttonText = "Subscribe";
            } else {
                // If the user is subscribed, show "Unsubscribe" button
                subscriptionLink = generateUnsubscribeLink(to);
                buttonText = "Unsubscribe";
            }

            // Append the subscription/unsubscription button to the email template
            String emailContentWithSubscriptionButton = template +
                    "<br><br><a href=\"" + subscriptionLink + "\" style=\"background-color: #FF5733; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">" + buttonText + "</a>";

            helper.setText(emailContentWithSubscriptionButton, true); // Use 'true' for HTML content


            mailSender.send(message);
            System.out.println("Email sent successfully.");
        } catch (MessagingException e) {
            System.err.println("Error while sending email: " + e.getMessage());
        }
    }

    // Method to generate the unsubscribe link
    private String generateUnsubscribeLink(String email) {
        return "http://localhost:8080/subscription/unsubscribe?email=" + email;
    }

    // Method to generate the subscribe link
    private String generateSubscribeLink(String email) {
        return "http://localhost:8080/subscription/subscribe?email=" + email;
    }



    // Check if the user is unsubscribed (this checks against the Subscription entity)
    private boolean isUserUnsubscribed(String email) {
        Subscription subscription = subscribedEmailRepository.findByEmail(email);
        return subscription != null && !subscription.isSubscribed();
    }


    // Method to subscribe the user
    public void subscribeUser(String email) {
        System.out.println("Subscribing email: " + email);

        // Check if the email is already subscribed or unsubscribed
        Subscription existingSubscription = subscribedEmailRepository.findByEmail(email);

        if (existingSubscription != null) {
            // If user exists and is unsubscribed, update their status to subscribed
            if (!existingSubscription.isSubscribed()) {
                existingSubscription.setSubscribed(true);
                subscribedEmailRepository.save(existingSubscription);
                System.out.println("User " + email + " has been re-subscribed.");
            } else {
                System.out.println("User " + email + " is already subscribed.");
            }
        } else {
            // If the user does not exist, create a new subscription with status true (subscribed)
            Subscription newSubscription = new Subscription(email, true);
            subscribedEmailRepository.save(newSubscription);
            System.out.println("User " + email + " has been subscribed.");
        }
    }

    // Method to unsubscribe the user
    public void unsubscribeUser(String email) {
        System.out.println("Unsubscribing email: " + email);

        // Check if the email exists
        Subscription existingSubscription = subscribedEmailRepository.findByEmail(email);

        if (existingSubscription != null) {
            // If user exists and is subscribed, update their status to unsubscribed
            if (existingSubscription.isSubscribed()) {
                existingSubscription.setSubscribed(false);
                subscribedEmailRepository.save(existingSubscription);
                System.out.println("User " + email + " has been unsubscribed.");
            } else {
                System.out.println("User " + email + " is already unsubscribed.");
            }
        } else {
            // If the user does not exist in the database, they cannot be unsubscribed
            Subscription newSubscription = new Subscription(email, false);
            subscribedEmailRepository.save(newSubscription);
            System.out.println("User " + email + " has been Unsubscribed.");
            //System.out.println("User " + email + " does not exist.");
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String content, String fileName, byte[] attachmentData) {
        MimeMessage message = mailSender.createMimeMessage();
        if (isUserUnsubscribed(to)) {
            System.out.println("User " + to + " has unsubscribed. Skipping email send.");
            return; // Skip sending the email if the user is unsubscribed
        }
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
// Generate the appropriate link based on subscription status
            String subscriptionLink;
            String buttonText;

            if (isUserUnsubscribed(to)) {
                // If the user is unsubscribed, show "Subscribe" button
                subscriptionLink = generateSubscribeLink(to);
                buttonText = "Subscribe";
            } else {
                // If the user is subscribed, show "Unsubscribe" button
                subscriptionLink = generateUnsubscribeLink(to);
                buttonText = "Unsubscribe";
            }

            // Attach PDF
            ByteArrayDataSource dataSource = new ByteArrayDataSource(attachmentData, "application/pdf");
            helper.addAttachment(fileName, dataSource);

            // Append the subscription/unsubscription button to the email template
            String emailContentWithSubscriptionButton =
                    "<br><br><a href=\"" + subscriptionLink + "\" style=\"background-color: #FF5733; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">" + buttonText + "</a>";

            helper.setText(emailContentWithSubscriptionButton, true); // Use 'true' for HTML content


            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error sending email with attachment: " + e.getMessage());

        }
    }

    @Async
//    public CompletableFuture<String> sendEmailWithAttachmentForBill(String recipientEmail, String subject, String htmlContent, byte[] attachment, String fileName) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//            helper.setTo(recipientEmail);
//            helper.setSubject(subject);
//            helper.setText(htmlContent, true);
//
//            helper.addAttachment(fileName, new ByteArrayResource(attachment));
//
//            mailSender.send(message);
//            System.out.println("Mail send successfully....");
//            return CompletableFuture.completedFuture("Success");
//        } catch (MessagingException e) {
//            e.printStackTrace();
//            return CompletableFuture.completedFuture("Failed to send email: " + e.getMessage());
//        }
//    }
    public CompletableFuture<String> sendEmailWithAttachmentForInvoice(String recipientEmail, String subject, String htmlContent, byte[] attachment, String fileName) {
        // Optional: Skip sending email if user is unsubscribed
        if (isUserUnsubscribed(recipientEmail)) {
            System.out.println("User " + recipientEmail + " has unsubscribed. Skipping email send.");
            return CompletableFuture.completedFuture("Skipped: User unsubscribed");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            helper.setSubject(subject);

            // Generate the appropriate link and button
            String subscriptionLink;
            String buttonText;

            if (isUserUnsubscribed(recipientEmail)) {
                subscriptionLink = generateSubscribeLink(recipientEmail);
                buttonText = "Subscribe";
            } else {
                subscriptionLink = generateUnsubscribeLink(recipientEmail);
                buttonText = "Unsubscribe";
            }

            // HTML button for subscription management
            String subscriptionButtonHtml = "<br><br><a href=\"" + subscriptionLink + "\" style=\"background-color: #FF5733; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">" + buttonText + "</a>";

            // Append the button to the original HTML content
            String updatedHtmlContent = htmlContent + subscriptionButtonHtml;

            helper.setText(updatedHtmlContent, true); // Enable HTML content
            helper.addAttachment(fileName, new ByteArrayResource(attachment));

            mailSender.send(message);
            System.out.println("Mail sent successfully....");
            return CompletableFuture.completedFuture("Success");
        } catch (MessagingException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture("Failed to send email: " + e.getMessage());
        }
    }

}