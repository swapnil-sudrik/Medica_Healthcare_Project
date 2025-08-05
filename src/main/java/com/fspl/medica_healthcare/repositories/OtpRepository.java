package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findByEmail(String email);

    List<Otp> findByCreatedDateTimeBefore(LocalDateTime time);

    void deleteByCreatedDateTimeBefore(LocalDateTime time);

}
