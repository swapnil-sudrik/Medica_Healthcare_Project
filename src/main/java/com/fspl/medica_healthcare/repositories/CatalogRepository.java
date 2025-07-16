package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CatalogRepository extends JpaRepository<Catalog, Long> {

    List<Catalog> findByHospital_IdAndNameContainingIgnoreCase(long id, String name);

    @Query("SELECT c FROM Catalog c WHERE c.hospital.id = :hospitalId AND c.id = :id")
    Catalog findByHospital_IdAndId(long hospitalId, long id);

    List<Catalog> findByHospital_IdAndCategoryContainingIgnoreCase(long id, String category);

    @Query("SELECT c FROM Catalog c WHERE c.hospital.id = :id")
    List<Catalog> findAllCatalogByHospitalId(long id);


}
