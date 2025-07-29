package com.fspl.medica_healthcare.services;
import com.fspl.medica_healthcare.models.Catalog;
import com.fspl.medica_healthcare.models.Category;
import com.fspl.medica_healthcare.repositories.CatalogRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.fspl.medica_healthcare.services.HospitalService.status;

@Service
public class CatalogService {

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    private static final Logger log = Logger.getLogger(CatalogService.class);

    public Boolean saveCatalog(Catalog catalog) {
        try {
            catalogRepository.save(catalog);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while saveCatalog" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return false;
        }
    }

    public Catalog getCatalogById(long hospitalId,long id) {
        try {
            Catalog catalog = catalogRepository.findByHospital_IdAndId(hospitalId,id);
            return catalog;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getCatalogById" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> findCatalogByName(long hospitalId,String name) {
        try {
            List<Catalog> catalogs = catalogRepository.findByHospital_IdAndNameContainingIgnoreCase(hospitalId,name);
            return catalogs;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findCatalogByName" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

//    public List<Catalog> findCatalogByCategory(long hospitalId,Category category) {
//        try {
//            List<Catalog> catalogs = catalogRepository.findByHospital_IdAndCategory(hospitalId,category);
//            return catalogs;
//        } catch (Exception e) {
//    e.printStackTrace();
//            log.error("An unexpected error occurred while findCatalogByCategory" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
//            return null;
//        }
//    }

    public List<Catalog> findAllCatalogByHospitalId(long id) {
        try {
            List<Catalog> catalogs = catalogRepository.findAllCatalogByHospitalId(id);
            return catalogs;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findAllCatalogByHospitalId" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> findCatalogByCategory(long hospitalId, String categoryName) {
        try {
            Category category = categoryService.getCategoryByName(categoryName);
            if (category != null) {
                return catalogRepository.findByHospital_IdAndCategory(hospitalId, category);
            } else {
                return catalogRepository.findByHospital_IdAndCategory_NameContainingIgnoreCase(hospitalId, categoryName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findCatalogByCategory" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> findActiveCatalogs() {
        try {
            status = 1;
            return catalogRepository.findActiveCatalogs(status);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findActiveCatalogs" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public List<Catalog> findDeactiveCatalogs() {
        try {
            status = 0;
            return catalogRepository.findDeactiveCatalogs(status);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while finding de-Active Catalogs" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Object getImageById(long hospitalId, long id) {
        try {
//            Catalog catalog = catalogRepository.findById(id)
//                    .orElseThrow(() -> new RuntimeException("Catalog not found"));
            Catalog catalog = catalogRepository.findByHospital_IdAndId(hospitalId,id);
            byte[] imageData = catalog.getImages();
            if (imageData == null || imageData.length == 0) {
                throw new RuntimeException("Image not found. ");
            }
            return imageData;
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting image" + ExceptionUtils.getStackTrace(e) +"Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

    public Catalog getCatalogByNameAndCategory(long hospitalId, String categoryName, String serviceName) {
        try {
            return catalogRepository.findByHospitalIdAndCategoryNameAndServiceName(hospitalId, categoryName, serviceName);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while checking existing catalog by name and category: " + ExceptionUtils.getStackTrace(e) + "Logged User " + userService.getAuthenticateUser());
            return null;
        }
    }
}