package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepositRepository extends JpaRepository<Deposit,Long> {

    @Query("SELECT d FROM Deposit d WHERE d.appointment.id = :appointmentId")
    List<Deposit> findAllDepositByAppointmentId(long appointmentId);
}
