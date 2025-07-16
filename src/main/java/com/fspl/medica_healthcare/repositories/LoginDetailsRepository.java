package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.LoginDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginDetailsRepository extends JpaRepository<LoginDetails, Long> {
    Optional<LoginDetails> findByUsername(String username);
    Optional<LoginDetails> findByToken(String token);
}
