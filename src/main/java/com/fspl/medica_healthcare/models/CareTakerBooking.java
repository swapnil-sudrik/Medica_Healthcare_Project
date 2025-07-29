package com.fspl.medica_healthcare.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
public class CareTakerBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long bookingId;

    @ManyToOne
    @JoinColumn(name = "id", nullable = false)
    private Staff caretaker;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalTime fromTime;

    private LocalTime toTime;
    @Lob
    private byte[] address;

    @Lob
    @Column(length = 500)
    private byte[] notes;

    private int bookingStatus;
}
