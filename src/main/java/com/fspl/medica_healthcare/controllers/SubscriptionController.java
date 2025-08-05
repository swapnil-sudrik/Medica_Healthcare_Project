package com.fspl.medica_healthcare.controllers;
import com.fspl.medica_healthcare.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/subscription")
public class SubscriptionController {
    @Autowired
    private EmailService emailService;

    // Endpoint to unsubscribe user
    @GetMapping("/unsubscribe")
    public String unsubscribe(@RequestParam("email") String email) {
        System.out.println("Unsubscribe request received for email: " + email);
        // Mark the user as unsubscribed
        emailService.unsubscribeUser(email);
        System.out.println("Unsubscription processed for email: " + email);
        return "You have been unsubscribed successfully.";
    }

    // Endpoint to subscribe user
    @GetMapping("/subscribe")
    public String subscribe(@RequestParam("email") String email) {
        System.out.println("Subscribe request received for email: " + email);
        // Mark the user as subscribed
        emailService.subscribeUser(email);
        System.out.println("Subscription processed for email: " + email);
        return "You have been subscribed successfully.";
    }
}
