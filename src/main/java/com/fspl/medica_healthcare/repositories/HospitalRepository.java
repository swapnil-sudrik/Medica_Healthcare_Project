package com.fspl.medica_healthcare.repositories;

import java.util.List;

import com.fspl.medica_healthcare.models.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    //    @Query("SELECT h.emailId FROM Hospital h WHERE h.emailId = :emailId AND h.status = 1")
    boolean existsByEmailId(String emailId);


//    boolean existsByName(String name);

    List<Hospital> findByNameContainingIgnoreCase(String name);

    @Query("SELECT h FROM Hospital h WHERE h.status = 1")
    List<Hospital> findActiveHospitals(int status);

    @Query("SELECT h FROM Hospital h WHERE h.status = 0")
    List<Hospital> findDeactiveHospitals(int status);

    @Query("SELECT h FROM Hospital h WHERE h.id = :id")
    Hospital getBranchesByHospitalId(long id);
}

