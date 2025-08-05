package com.fspl.medica_healthcare.controllers;


import com.fspl.medica_healthcare.dtos.Notification;
import com.fspl.medica_healthcare.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public List<Notification> getAll(@AuthenticationPrincipal UserDetails user) {
        return notificationService.getNotifications(user.getUsername());
    }

    @DeleteMapping("/{id}")
    public void remove(@AuthenticationPrincipal UserDetails user, @PathVariable String id) {
        System.out.println("ok"+ id);
        notificationService.removeNotification(user.getUsername(), id);
    }

    @DeleteMapping("/clear")
    public void clear(@AuthenticationPrincipal UserDetails user) {
        notificationService.clearNotifications(user.getUsername());
    }

    @PostMapping("/send")
    public void sendToUser(@RequestParam String username, @RequestParam String message) {
        Notification notification = new Notification(UUID.randomUUID().toString(), message, Instant.now());
        notificationService.addNotification(username, notification);
        messagingTemplate.convertAndSendToUser(username, "/user/notifications", notification);
    }
}
