package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.dtos.Notification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private final Map<String, List<Notification>> notifications = new ConcurrentHashMap<>();

    public void addNotification(String username, Notification notification) {
        notifications.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(notification);
    }

    public List<Notification> getNotifications(String username) {
        return notifications.getOrDefault(username, new ArrayList<>());
    }

    public void removeNotification(String username, String id) {
        List<Notification> list = notifications.get(username);
        if (list != null) {
            list.removeIf(n -> n.getId().equals(id));
        }
    }

    public void clearNotifications(String username) {
        notifications.remove(username);
    }
}
