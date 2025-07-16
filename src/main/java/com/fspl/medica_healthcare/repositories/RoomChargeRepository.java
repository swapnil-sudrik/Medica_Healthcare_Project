package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.enums.RoomType;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.RoomCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomChargeRepository extends JpaRepository<RoomCharge,Long> {
    Optional<RoomCharge> findByHospitalAndRoomType(Hospital hospital,RoomType roomType);

}
