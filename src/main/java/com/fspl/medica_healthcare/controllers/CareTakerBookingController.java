package com.fspl.medica_healthcare.controllers;

import com.fspl.medica_healthcare.dtos.CareTakerBookingDTO;
import com.fspl.medica_healthcare.models.CareTakerBooking;
import com.fspl.medica_healthcare.models.Staff;
import com.fspl.medica_healthcare.repositories.StaffRepository;
import com.fspl.medica_healthcare.services.CareTakerBookingService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/caretakers")
public class CareTakerBookingController {

    private static final Logger logger = Logger.getLogger(CareTakerBookingController.class);
    private final StringWriter stringWriter = new StringWriter();
    private final PrintWriter printWriter = new PrintWriter(stringWriter);

    @Autowired
    private CareTakerBookingService bookingService;

    @Autowired
    private StaffRepository staffRepository;

    // _________________________________________________________________________________________________________________________________________________

    @PostMapping("/book")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> bookCaretaker(@RequestBody CareTakerBookingDTO careTakerBookingDTO) {
        try {
            //Validate caretakerId
            long caretakerId = careTakerBookingDTO.getCaretakerId();

            // check caretakerId is not null and a valid positive number or not
            if (caretakerId <= 0) {
                return ResponseEntity.badRequest().body("Caretaker ID must be a positive number.");
            }

            LocalDate startDate;
            LocalDate endDate = null;
            LocalTime fromTime;
            LocalTime toTime;

            //validating sartDate
            try {
                startDate = LocalDate.parse(careTakerBookingDTO.getStartDate());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid startDate format. Use yyyy-MM-dd.");
            }

            // validating endDate and endDate can not be before startDate
            if (careTakerBookingDTO.getEndDate() != null && !careTakerBookingDTO.getEndDate().trim().isEmpty()) {
                try {
                    endDate = LocalDate.parse(careTakerBookingDTO.getEndDate());
                    if (endDate.isBefore(startDate)) {
                        return ResponseEntity.badRequest().body("End date cannot be before start date.");
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Invalid endDate format. Use yyyy-MM-dd.");
                }
            }

            //validating fromTime and toTime
            try {
                fromTime = LocalTime.parse(careTakerBookingDTO.getFromTime());
                toTime = LocalTime.parse(careTakerBookingDTO.getToTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid time format. Use HH:mm.");
            }

            //check fromTime is after toTime
            if (fromTime.isAfter(toTime)) {
                return ResponseEntity.badRequest().body("From time must be before to time.");
            }

            LocalTime WORK_START = LocalTime.of(0, 0);
            LocalTime WORK_END = LocalTime.of(23, 59);

           /* if (fromTime.isBefore(WORK_START) || toTime.isAfter(WORK_END)) {
                return ResponseEntity.badRequest().body("Booking time must be between 10:00 and 19:00.");
            }*/

            //Validating Address
            if (careTakerBookingDTO.getAddress() == null || careTakerBookingDTO.getAddress().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Address cannot be null or empty.");
            }

            // Validating Notes
            if (careTakerBookingDTO.getNotes() != null && careTakerBookingDTO.getNotes().length() > 500) {
                return ResponseEntity.badRequest().body("Notes cannot exceed 500 characters.");
            }

            //check if careTaker exists with provided id or not
            Optional<Staff> optionalCaretaker = staffRepository.findByIdAndRoles(caretakerId, "caretaker");
            if (optionalCaretaker.isEmpty()) {
                return ResponseEntity.badRequest().body("Caretaker does not exist with this ID.");
            }

            // get All bookings for the given careTakerId
            List<CareTakerBooking> existingBookings = bookingService.findByCaretakerId(caretakerId);

            //If the user gives an endDate, use it as the last date to check; otherwise,
            // check for the next 30 days starting from startDate
            LocalDate futureLimit;
            if (endDate != null) {
                futureLimit = endDate;
            } else {
                futureLimit = startDate.plusDays(30);
            }


            for (LocalDate date = startDate; !date.isAfter(futureLimit); date = date.plusDays(1)) {
                for (CareTakerBooking booking : existingBookings) {

                    // Skip bookings with incomplete data
                    if (booking.getStartDate() == null || booking.getFromTime() == null || booking.getToTime() == null) {
                        continue;
                    }

                    boolean isSameDay = booking.getStartDate().isEqual(date);
                    boolean isRangeBooking = booking.getEndDate() != null &&
                            (date.isEqual(booking.getStartDate()) || (date.isAfter(booking.getStartDate()) && !date.isAfter(booking.getEndDate())));
                    boolean isOpenEndedBooking = booking.getEndDate() == null && !booking.getStartDate().isAfter(date);

                    if (isSameDay || isRangeBooking || isOpenEndedBooking) {
                        LocalTime existingStart = booking.getFromTime().minusMinutes(59);
                        LocalTime existingEnd = booking.getToTime().plusMinutes(59);

                        if (!(toTime.isBefore(existingStart) || fromTime.isAfter(existingEnd))) {
                            return ResponseEntity.badRequest().body("Caretaker is already booked on " + date + " during or near this time.");
                        }
                    }
                }
            }

            // All validations passed , create booking
            CareTakerBooking booking = new CareTakerBooking();
            booking.setCaretaker(optionalCaretaker.get());
            booking.setStartDate(startDate);
            booking.setEndDate(endDate);
            booking.setFromTime(fromTime);
            booking.setToTime(toTime);
            booking.setAddress(careTakerBookingDTO.getAddress().getBytes());
            booking.setNotes(careTakerBookingDTO.getNotes() != null ? careTakerBookingDTO.getNotes().getBytes() : null);
            booking.setBookingStatus(1);

            CareTakerBooking savedBooking = bookingService.createBooking(booking);
            return ResponseEntity.ok(savedBooking);

        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error booking caretaker: " + stringWriter + " | Request Data: " + careTakerBookingDTO);
            return ResponseEntity.internalServerError().body("An error occurred while booking the caretaker.");
        }
    }

    //____________________________________________________________________________________________________________________________________________________________

    // Get full list of CareTakers
    @GetMapping("/getAllCaretakers")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> getAllCaretakers() {
        try {
            List<Staff> caretakers = bookingService.getAllCaretakers();
            return ResponseEntity.ok(caretakers);
        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error fetching available caretakers: " + stringWriter);
            return ResponseEntity.internalServerError().body("An error occurred while fetching caretakers.");
        }
    }

    //____________________________________________________________________________________________________________________________________________________________


    // Get Bookings by caretakerId
    @GetMapping("/getCaretakerBookings/{caretakerId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getBookingsForCaretaker(@PathVariable Long caretakerId) {
        try {
            List<CareTakerBooking> bookings = bookingService.findByCaretakerId(caretakerId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error fetching bookings for caretaker ID " + caretakerId + ": " + stringWriter);
            return ResponseEntity.internalServerError().body("An error occurred while fetching bookings.");
        }
    }

    //____________________________________________________________________________________________________________________________________________________________


    // Get All Bookings
    @GetMapping("/getAllbookings")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAllBookings() {
        try {
            List<CareTakerBooking> bookings = bookingService.findAllBookings();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.error("Error fetching all caretaker bookings: " + stringWriter);
            return ResponseEntity.internalServerError().body("An error occurred while fetching all bookings.");
        }
    }

    //____________________________________________________________________________________________________________________________________________________________


    @DeleteMapping("/cancelBooking/{bookingId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        try {
            CareTakerBooking existingBooking = bookingService.findByBookingId(bookingId);
            existingBooking.setBookingStatus(0); // Mark as cancelled
            bookingService.updateBooking(existingBooking); // Save the change
            return ResponseEntity.ok("Booking canceled successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error canceling booking ID " + bookingId + ": ", e);
            return ResponseEntity.internalServerError().body("An error occurred while canceling the booking.");
        }
    }


    //____________________________________________________________________________________________________________________________________________________________


  /*  //Get availabl slots of care taker by caretakerId
    @GetMapping("/{caretakerId}/availableSlots")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> getAvailableSlots(
            @PathVariable Long caretakerId,
            @RequestParam String date) {
        try {
            LocalDate bookingDate;
            try {
                bookingDate = LocalDate.parse(date);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid date format. Use yyyy-MM-dd.");
            }

            List<Map<String, String>> availableSlots = bookingService.getAvailableSlotsForDate(caretakerId, bookingDate);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error fetching available slots.");
        }
    }
*/
    //____________________________________________________________________________________________________________________________________________________________


    /*

    // update Booking by caretakerId

     @PutMapping("/updateByCaretakerId/{caretakerId}")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public ResponseEntity<?> updateUpcomingBookingByCaretakerId(
        @PathVariable Long caretakerId,
        @RequestBody CareTakerBookingDTO dto) {

    try {
        if (caretakerId == null || caretakerId <= 0) {
            return ResponseEntity.badRequest().body("Invalid caretaker ID.");
        }

        Staff caretaker = staffRepository.findById(caretakerId)
                .orElseThrow(() -> new RuntimeException("Caretaker not found"));

        // Fetch the upcoming booking (assuming your service has this method)
        CareTakerBooking existingBooking = bookingService
                .findNextBookingForCaretaker(caretakerId)
                .orElseThrow(() -> new RuntimeException("No upcoming booking found for caretaker."));

        // Parse and validate times
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(dto.getStartTime());
            end = LocalDateTime.parse(dto.getEndTime());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use ISO format: yyyy-MM-ddTHH:mm:ss");
        }

        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().body("Start time must be before end time.");
        }

        if (start.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Start time cannot be in the past.");
        }

        long durationInMinutes = java.time.Duration.between(start, end).toMinutes();
        if (durationInMinutes > 24 * 60) {
            return ResponseEntity.badRequest().body("Booking duration cannot exceed 24 hours.");
        }

        int currentYear = LocalDateTime.now().getYear();
        if (start.getYear() < currentYear || end.getYear() < currentYear) {
            return ResponseEntity.badRequest().body("Booking year cannot be in the past.");
        }

        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Address cannot be null or empty.");
        }

        if (dto.getNotes() != null && dto.getNotes().length() > 500) {
            return ResponseEntity.badRequest().body("Notes cannot exceed 500 characters.");
        }

        // Update fields
        existingBooking.setStartTime(start);
        existingBooking.setEndTime(end);
        existingBooking.setAddress(dto.getAddress().getBytes());
        existingBooking.setNotes(dto.getNotes() != null ? dto.getNotes().getBytes() : null);

        CareTakerBooking updated = bookingService.createBooking(existingBooking);
        return ResponseEntity.ok(updated);

    } catch (Exception e) {
        e.printStackTrace(printWriter);
        logger.error("Error updating caretaker booking: " + stringWriter + " | Request Data: " + dto);
        return ResponseEntity.internalServerError().body("An error occurred while updating the booking.");
    }
}

    * */
}
