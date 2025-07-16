package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Appointment;
import com.fspl.medica_healthcare.models.HospitalizationInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalizationInfoRepository extends JpaRepository<HospitalizationInfo,Long> {

    List<HospitalizationInfo> findByDateOfDischargeIsNull();

}
