package com.fspl.medica_healthcare.dtos;

import lombok.Data;

@Data
public class CareTakerBookingDTO {
    private long bookingId;
    private long caretakerId;
    private String startDate;
    private String endDate;
    private String fromTime;
    private String toTime;
    private String address;
    private String notes;
    private int boookingStatus;
}
