package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.CareTakerBooking;
import com.fspl.medica_healthcare.models.Staff;
import com.fspl.medica_healthcare.repositories.CareTakerBookingRepository;
import com.fspl.medica_healthcare.repositories.StaffRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class CareTakerBookingService {

    private static final Logger logger = Logger.getLogger(CareTakerBookingService.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CareTakerBookingRepository bookingRepository;

    // Method 1: Get available caretakers (role = caretaker)
    public List<Staff> getAllCaretakers() {
        try {
            return staffRepository.findByRoles("caretaker");
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching available caretakers: " + sw.toString());
            return new ArrayList<>();
        }
    }

    // Method 2: Create booking
    public CareTakerBooking createBooking(CareTakerBooking booking) {
        try {
            return bookingRepository.save(booking);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error creating caretaker booking: " + sw.toString());
            return null;
        }
    }

    // Method 3: Get bookings by caretaker ID
    public List<CareTakerBooking> findByCaretakerId(Long caretakerId) {
        try {
            return bookingRepository.findByCaretakerId(caretakerId);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching bookings for caretaker ID " + caretakerId + ": " + sw.toString());
            return new ArrayList<>();
        }
    }

    // Method 4: Get all bookings
    public List<CareTakerBooking> findAllBookings() {
        try {
            return bookingRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error fetching all caretaker bookings: " + sw.toString());
            return new ArrayList<>();
        }
    }

    public CareTakerBooking findByBookingId(long bookingId) {
        try {
            return bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
        } catch (Exception e) {
            logger.error("Error in fetching booking with ID " + bookingId, e);
            throw e;
        }
    }


    public CareTakerBooking updateBooking(CareTakerBooking booking) {
        try {
            return bookingRepository.save(booking);
        } catch (Exception e) {
            e.printStackTrace(pw);
            logger.error("Error updating caretaker booking: " + sw.toString());
            throw e;
        }
    }


    // Method to check booking should not overlap
    public boolean hasOverlappingBooking(Long caretakerId, LocalDate date, LocalTime fromTime, LocalTime toTime) {
        List<CareTakerBooking> overlapping = bookingRepository.findOverlappingBookings(caretakerId, date, fromTime, toTime);
        return !overlapping.isEmpty();
    }

/*
    // To get available slots on specific date of specific caretakerId
    public List<Map<String, String>> getAvailableSlotsForDate(Long caretakerId, LocalDate date) {
        List<Map<String, String>> availableSlots = new ArrayList<>();

        // Define working hours
        LocalTime workStart = LocalTime.of(10, 0);
        LocalTime workEnd = LocalTime.of(19, 0);

        // Fetch bookings for this caretaker on the given date
        List<CareTakerBooking> bookings = bookingRepository.findByCaretakerIdAndDate(caretakerId, date);

        // Sort bookings by fromTime
        bookings.sort(Comparator.comparing(CareTakerBooking::getFromTime));

        LocalTime slotStart = workStart;

        for (CareTakerBooking booking : bookings) {
            LocalTime bookedStart = booking.getFromTime();
            LocalTime bookedEnd = booking.getToTime();

            if (slotStart.isBefore(bookedStart)) {
                // Available slot found
                Map<String, String> slot = new HashMap<>();
                slot.put("from", slotStart.toString());
                slot.put("to", bookedStart.toString());
                availableSlots.add(slot);
            }

            // Move slotStart forward
            if (slotStart.isBefore(bookedEnd)) {
                slotStart = bookedEnd;
            }
        }

        // Check for last slot till end of day
        if (slotStart.isBefore(workEnd)) {
            Map<String, String> slot = new HashMap<>();
            slot.put("from", slotStart.toString());
            slot.put("to", workEnd.toString());
            availableSlots.add(slot);
        }
        return availableSlots;
    }
*/


}
