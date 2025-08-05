package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.HospitalExtraCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HospitalExtraChargeRepository extends JpaRepository<HospitalExtraCharge, Long> {

    // Calculate total yearly charges for a specific hospital based on paymentDate
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM HospitalExtraCharge e WHERE e.hospital.id = :hospitalId AND FUNCTION('YEAR', e.paymentDate) = :year")
    double calculateTotalChargesForHospitalByYear(@Param("hospitalId") long hospitalId, @Param("year") int year);

    // Calculate total charges in a date range using paymentDate (not a non-existent field `date`)
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM HospitalExtraCharge h WHERE h.hospital = :hospital AND h.paymentDate BETWEEN :startDate AND :endDate")
    double calculateTotalChargesForHospital(@Param("hospital") Hospital hospital,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // Fetch extra charges using paymentDate, not createdDate
    List<HospitalExtraCharge> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    // Optional: Fetch by hospital ID
    List<HospitalExtraCharge> findByHospital_Id(long hospitalId);
}
