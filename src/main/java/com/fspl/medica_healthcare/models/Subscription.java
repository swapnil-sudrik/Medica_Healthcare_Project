package com.fspl.medica_healthcare.models;

import jakarta.persistence.*;

@Entity
public class Subscription  {

    @Id
    private String email;

    private boolean isSubscribed; // true = subscribed, false = unsubscribed

    // Default constructor
    public Subscription() {
    }

    // Constructor with email and subscription status
    public Subscription(String email, boolean isSubscribed) {
        this.email = email;
        this.isSubscribed = isSubscribed;
    }

    public boolean isSubscribed() {
        return isSubscribed;
    }

    public void setSubscribed(boolean subscribed) {
        isSubscribed = subscribed;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "email='" + email + '\'' +
                ", isSubscribed=" + isSubscribed +
                '}';
    }
}
