package com.fspl.medica_healthcare.repositories;

import java.util.List;

import com.fspl.medica_healthcare.models.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    boolean existsByEmailId(String emailId);

    boolean existsByName(String name);

    List<Hospital> findByNameContainingIgnoreCase(String name);

    @Query("SELECT h FROM Hospital h WHERE h.status = 1")
    List<Hospital> findActiveHospitals(@Param("id") int status);

    @Query("SELECT h FROM Hospital h WHERE h.status = 0")
    List<Hospital> findDeactiveHospitals(@Param("id") int status);

}

