package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Reports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportsRepository extends JpaRepository<Reports, Long> {

    List<Reports> findByPatientId(long id);

    Optional<Reports> findByIdAndPatientId(long reportId, long patientId);

}