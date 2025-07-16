package com.fspl.medica_healthcare.services;
import com.fspl.medica_healthcare.models.Catalog;
import com.fspl.medica_healthcare.repositories.CatalogRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Service
public class CatalogService {

    @Autowired
    private CatalogRepository catalogRepository;

     @Autowired
     private UserService userService;

    private static final Logger log = Logger.getLogger(CatalogService.class);

    public Catalog saveCatalog(Catalog catalog) {
        try {
            return catalogRepository.save(catalog);
        } catch (Exception e) {
            log.error("An unexpected error occurred while saveCatalog" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> getAllCatalog() {
        try {
            return catalogRepository.findAll();
        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllCatalog" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Catalog getCatalogById(long hospitalId,long id) {
        try {
            return catalogRepository.findByHospital_IdAndId(hospitalId,id);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getCatalogById" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> findCatalogByName(long hospitalId,String name) {
        try {
            return catalogRepository.findByHospital_IdAndNameContainingIgnoreCase(hospitalId,name);
        } catch (Exception e) {
            log.error("An unexpected error occurred while findCatalogByName" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> findCatalogByCategory(long hospitalId,String category) {
        try {
            return catalogRepository.findByHospital_IdAndCategoryContainingIgnoreCase(hospitalId,category);
        } catch (Exception e) {
            log.error("An unexpected error occurred while findCatalogByCategory" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> findAllCatalogByHospitalId(long id) {
        try {
            return catalogRepository.findAllCatalogByHospitalId(id);
        } catch (Exception e) {
            log.error("An unexpected error occurred while findAllCatalogByHospitalId" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }
}