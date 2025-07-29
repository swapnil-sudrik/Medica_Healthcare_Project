package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.CareTakerBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CareTakerBookingRepository extends JpaRepository<CareTakerBooking , Long> {

    List<CareTakerBooking> findByCaretakerId(Long caretakerId);


   /* @Query("SELECT b FROM CareTakerBooking b WHERE b.caretaker.id = :caretakerId AND " +
            "(:startTime < b.endTime AND :endTime > b.startTime)")
    List<CareTakerBooking> findOverlappingBookings(Long caretakerId, LocalDateTime startTime, LocalDateTime endTime);
*/

    @Query("SELECT b FROM CareTakerBooking b WHERE b.caretaker.id = :caretakerId AND b.startDate = :date " +
            "AND (:fromTime < b.toTime AND :toTime > b.fromTime)")
    List<CareTakerBooking> findOverlappingBookings(Long caretakerId, LocalDate date, LocalTime fromTime, LocalTime toTime);

/*

    @Query("SELECT b FROM CareTakerBooking b WHERE b.caretaker.id = :caretakerId AND b.startDate = :date")
    List<CareTakerBooking> findByCaretakerIdAndDate(Long caretakerId, LocalDate date);

*/

}
