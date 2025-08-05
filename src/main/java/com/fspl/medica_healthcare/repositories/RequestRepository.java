package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
}
