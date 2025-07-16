package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String Username);

    @Query("SELECT u FROM User u WHERE u.hospital.id = :id AND u.status = 1")
    List<User> findAllActiveUsersByHospitalId(@Param("id") long id);

    @Query("SELECT u FROM User u WHERE u.hospital.id = :id")
    List<User> findAllUsersByHospitalId(@Param("id") long id);
}
