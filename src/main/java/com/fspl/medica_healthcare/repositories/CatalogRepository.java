package com.fspl.medica_healthcare.repositories;

import com.fspl.medica_healthcare.models.Catalog;
import com.fspl.medica_healthcare.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatalogRepository extends JpaRepository<Catalog, Long> {

    List<Catalog> findByHospital_IdAndNameContainingIgnoreCase(long id, String name);

    @Query("SELECT c FROM Catalog c WHERE c.hospital.id = :hospitalId AND c.id = :id")
    Catalog findByHospital_IdAndId(long hospitalId, long id);

    List<Catalog> findByHospital_IdAndCategory(long id, Category category);

    @Query("SELECT c FROM Catalog c WHERE c.hospital.id = :id")
    List<Catalog> findAllCatalogByHospitalId(long id);

    List<Catalog> findByHospital_IdAndCategory_NameContainingIgnoreCase(long id, String categoryName);

    @Query("SELECT c FROM Catalog c WHERE c.status = 1")
    List<Catalog> findActiveCatalogs(@Param("id") int status);

    @Query("SELECT c FROM Catalog c WHERE c.status = 0")
    List<Catalog> findDeactiveCatalogs(@Param("id") int status);

    @Query("SELECT c FROM Catalog c WHERE c.hospital.id = :hospitalId AND LOWER(c.category.name) = LOWER(:categoryName) AND LOWER(c.name) = LOWER(:serviceName)")
    Catalog findByHospitalIdAndCategoryNameAndServiceName(@Param("hospitalId") long hospitalId,
                                                          @Param("categoryName") String categoryName,
                                                          @Param("serviceName") String serviceName);

}




