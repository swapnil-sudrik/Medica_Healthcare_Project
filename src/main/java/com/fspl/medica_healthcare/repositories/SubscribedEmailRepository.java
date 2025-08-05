package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Subscription;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository


public interface SubscribedEmailRepository extends JpaRepository<Subscription, String> {
    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    Subscription findByEmail(String email);
}
