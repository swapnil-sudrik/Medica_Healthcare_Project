package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.enums.BillingStatus;
import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    @Query("SELECT b FROM Billing b WHERE b.modifiedUser.hospital.id = :id")
    List<Billing> findAllHospitalBillsByHospitalId(long id);

//    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Billing b " +
//            "WHERE b.modifiedUser.hospital.id = :id " +
//            "AND (:month IS NULL OR FUNCTION('MONTH', b.modifiedDate) = :month) " +
//            "AND (:year IS NULL OR FUNCTION('YEAR', b.modifiedDate) = :year)")
//    Double getTotalRevenueByHospitalIdAndDate(long id, int month, int year);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Billing b " +
            "JOIN b.appointment a " +
            "JOIN a.hospital h " +
            "WHERE h.id = :id " +
            "AND (:month IS NULL OR :month = 0 OR EXTRACT(MONTH FROM b.modifiedDate) = :month) " +
            "AND (:year IS NULL OR :year = 0 OR EXTRACT(YEAR FROM b.modifiedDate) = :year)")
    Double getTotalRevenueByHospitalIdAndDate(@Param("id") long id,
                                              @Param("month") int month,
                                              @Param("year") int year);


    // Calculate total revenue from bills
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Billing b WHERE b.modifiedUser.hospital.id = :id")
    Double getTotalRevenueByHospitalId(long id);

    //    boolean existsByAppointment_Id(long id);
    @Query("SELECT b FROM Billing b WHERE b.appointment.hospital.id = :hospitalId AND b.appointment.id = :appointmentId ")
    boolean existsByAppointment_Hospital_IdAndAppointment_Id(long hospitalId,long appointmentId);

    //    Optional<Billing> findByAppointment_Id(long appointmentId);
    @Query("SELECT b FROM Billing b WHERE b.appointment.hospital.id = :hospitalId AND b.appointment.id = :appointmentId ")
    Optional<Billing> findByAppointment_Hospital_IdAndAppointment_Id(long hospitalId,long appointmentId);

    //    @Query("SELECT b FROM Billing b WHERE b.dueDate >= CURRENT_DATE")
    @Query("SELECT b FROM Billing b WHERE b.dueDate IS NOT NULL")
    List<Billing> findBillsWithDueDates();

    @Query("SELECT b FROM Billing b " +
            "JOIN b.appointment a " +
            "JOIN a.hospital h " +
            "WHERE h.id = :id " +
            "AND b.status IN :status " +
            "ORDER BY b.createdDate DESC")
    List<Billing> findByHospitalIdAndStatusIn(@Param("id") long id, @Param("status") List<BillingStatus> status);


    @Query("SELECT b FROM Billing b " +
            "JOIN b.appointment a " +
            "JOIN a.hospital h " +
            "WHERE h.id = :id " +
            "AND (:minBalance IS NULL OR b.balanceAmount >= :minBalance) " +
            "AND (:maxBalance IS NULL OR b.balanceAmount <= :maxBalance)")
    List<Billing> findByBalanceAmountRange(@Param("id") long id, @Param("minBalance") BigDecimal minBalance, @Param("maxBalance") BigDecimal maxBalance);

//    @Query("SELECT b FROM Billing b " +
//            "JOIN b.appointment a " +
//            "JOIN a.hospital h " +
//            "WHERE h.id = :id " +
//            "AND (:month IS NULL OR MONTH(b.createdDate) = :month) " +
//            "AND (:year IS NULL OR YEAR(b.createdDate) = :year) " +
//            "ORDER BY b.createdDate DESC")
//    List<Billing> findBillsByDate(@Param("id") long id, @Param("month") int month, @Param("year") int year);

    @Query("SELECT b FROM Billing b " +
            "JOIN b.appointment a " +
            "JOIN a.hospital h " +
            "WHERE h.id = :id " +
            "AND (:month IS NULL OR :month = 0 OR EXTRACT(MONTH FROM b.createdDate) = :month) " +
            "AND (:year IS NULL OR :year = 0 OR EXTRACT(YEAR FROM b.createdDate) = :year) " +
            "ORDER BY b.createdDate DESC")
    List<Billing> findBillsByDate(@Param("id") long id,
                                  @Param("month") int month,
                                  @Param("year") int year);
//    @Query("SELECT b FROM Billing b WHERE b.appointment.hospital.hospitalId = :hospitalId AND b.balanceAmount > :balanceAmount")

    @Query("SELECT b FROM Billing b " +
            "JOIN b.appointment a " +
            "JOIN a.hospital h " +
            "WHERE h.id = :id " +
            "AND b.balanceAmount > :balanceAmount")
    List<Billing> findBillingsWithBalance(@Param("id") long id, @Param("balanceAmount") BigDecimal balanceAmount);

    @Query("SELECT b FROM Billing b " +
            "JOIN b.appointment a " +
            "JOIN a.hospital h " +
            "WHERE h.id = :id " +
            "AND b.createdDate BETWEEN :startDate AND :endDate")
    List<Billing> findByCreatedDateBetween(@Param("id") long id, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    @Query("SELECT MIN(b.createdDate) FROM Billing b")
    LocalDate findEarliestBillingDate();

    @Query("SELECT b FROM Billing b WHERE b.appointment.hospital.id = :hospitalId AND b.appointment.patient.name = :name")
    List<Billing> findByAppointment_Patient_Name(@Param("hospitalId") long hospitalId,@Param("name") String name);

    @Query("SELECT b FROM Billing b WHERE b.appointment.hospital.id = :hospitalId AND b.appointment.patient.id = :id")
    Billing findByPatientId(@Param("hospitalId") long hospitalId,@Param("id") long id);

    Optional<Billing> findByAppointment_Hospital_IdAndId(long hospitalId, long id);


}
