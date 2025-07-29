package com.fspl.medica_healthcare.repositories;


import com.fspl.medica_healthcare.models.Leaves;
import com.fspl.medica_healthcare.models.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leaves,Long> {

    List<Leaves> findAllByDate(LocalDate date);


    @Query(value = "SELECT * FROM leaves WHERE date BETWEEN :fromDate AND :toDate", nativeQuery = true)
    List<Leaves> findLeavesInRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("SELECT l FROM Leaves l WHERE l.user.id = :id AND l.date = :date")
    List<Leaves> findDoctorLeaveByDate(@Param("id") long id, @Param("date") LocalDate date);



}
