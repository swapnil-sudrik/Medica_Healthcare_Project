package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings , Long> {


//    @Query(value="select hospital_letter_head from settings where setting_id =: setting_id", nativeQuery = true)
//    Optional<Settings> findByLetterHead(@Param("setting_id") long setting_id);

    @Query(value = "SELECT hospital_letter_head FROM settings WHERE hospital_id = :hospital_id", nativeQuery = true)
    Optional<byte []> findLetterHead(@Param("hospital_id") long hospital_id);

    @Query(value = "SELECT hospital_logo FROM settings WHERE hospital_id = :hospital_id", nativeQuery = true)
    Optional<byte []> findLetterLogo(@Param("hospital_id") long hospital_id);

    boolean existsByHospital(Hospital hospital);



    //Optional<Settings> findByHospitalId(Long hospitalId);

    Optional<Settings> findByHospital(Hospital hospital);
    Optional<Settings> findByHospital_Id(Long hospitalId);

}