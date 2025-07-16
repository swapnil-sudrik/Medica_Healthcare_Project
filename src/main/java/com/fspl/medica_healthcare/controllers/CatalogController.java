package com.fspl.medica_healthcare.controllers;


import com.fspl.medica_healthcare.dtos.CatalogDTO;
import com.fspl.medica_healthcare.exceptions.RecordNotFoundException;
import com.fspl.medica_healthcare.models.Catalog;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.CatalogService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private UserService userService;


    private static final Logger log = Logger.getLogger(CatalogController.class);

      //-----------------------------------------Create Catalog---------------------------

    @PostMapping("/addCatalog")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> createCatalog(@ModelAttribute CatalogDTO catalogDTO) {
        User loginUser= null;
        try {
            loginUser = userService.getAuthenticateUser();

            if (!catalogDTO.getCategory().toUpperCase().matches("^(CONSULTATION_FEES|LABORATORY|GENERAL_SERVICE|SPECIALIST_SERVICE|ROOM)$")) {
                return new ResponseEntity<>("Category is not correct. Allowed values are: CONSULTATION_FEES, LABORATORY, GENERAL_SERVICE, SPECIALIST_SERVICE, ROOM", HttpStatus.BAD_REQUEST);
            }

            if (catalogDTO.getName() == null || !catalogDTO.getName().matches("^[A-Za-z ]+$")) {
                return new ResponseEntity<>("Name must contain only letters", HttpStatus.BAD_REQUEST);
            }

            if (catalogDTO.getFees() == null || !catalogDTO.getFees().toString().matches("^\\d+(\\.\\d+)?$")) {
                return new ResponseEntity<>("Fees must be a numeric value", HttpStatus.BAD_REQUEST);
            }

            Catalog catalog1 = new Catalog();

            catalog1.setName(catalogDTO.getName().trim().replaceAll("\\s+", " "));
            catalog1.setCategory(catalogDTO.getCategory().toUpperCase());
            catalog1.setFees(Double.valueOf(catalogDTO.getFees()));
            catalog1.setDescription(catalogDTO.getDescription().getBytes());
            catalog1.setCreated(loginUser);
            catalog1.setModified(loginUser);
            catalog1.setCreatedDate(LocalDate.now());
            catalog1.setModifiedDate(LocalDate.now());
            catalog1.setStatus(1);

            if (catalog1.getHospital() == null) {
                catalog1.setHospital(loginUser.getHospital());
            }

            Catalog savedCatalog = catalogService.saveCatalog(catalog1);
            return new ResponseEntity<>(savedCatalog, HttpStatus.OK);
        } catch (Exception e) {
            log.error("An unexpected error occurred while createCatalog" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.badRequest().body("Catalog not found");
        }
    }

    //----------------------------------------Get All Catalog-----------------------------

    @GetMapping("/getAllCatalog")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllCatalog() {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            List<Catalog> catalogs = catalogService.findAllCatalogByHospitalId(loginUser.getHospital().getId());
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("error: No catalogs available.");
            }

            List<CatalogDTO> catalogDTOs = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setStatus(catalog.getStatus());
                return dto;

            }).collect(Collectors.toList());
            return ResponseEntity.ok(catalogDTOs);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllCatalog" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.status(404).body("error: No catalogs available.");
        }
    }


    //------------------------------------------------Get All By Hospital Id------------------

    @GetMapping("/ByHospital/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllByHospitalId(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();
            List<Catalog> catalogs = catalogService.findAllCatalogByHospitalId(id);

            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("error: Catalog not available for this hospital.");
            }
            List<CatalogDTO> catalogDTOs = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setStatus(catalog.getStatus());
                return dto;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(catalogDTOs);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllByHospitalId" + ExceptionUtils.getStackTrace(e) + "Logged User: " + loginUser.getId());
            return ResponseEntity.status(404).body("error: Catalogs not found for this hospital");
        }
    }

    //-----------------------------------Get All By Service Name-------------------------

    @GetMapping("/{name}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllByServiceName(@PathVariable String name) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            List<Catalog> catalogs = catalogService.findCatalogByName(loginUser.getHospital().getId(), name);
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("error: Catalog not available for this hospital.");
            }
            List<CatalogDTO> catalogDTO = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setStatus(catalog.getStatus());
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(catalogDTO);

        } catch (Exception e) {
            log.error("An unexpected error occurred while getAllByServiceName" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.status(404).body("error: Catalogs not found for service name: " + name);
        }
    }

    //-------------------------------------Get Catalog By Catagory-----------------------

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getCatalogByCategory(@PathVariable String category) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            List<Catalog> catalogs = catalogService.findCatalogByCategory(loginUser.getHospital().getId(), category);

            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("error: Catalogs not found for category: " + category);
            }

            List<CatalogDTO> catalogDTOs = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setStatus(catalog.getStatus());
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(catalogDTOs);
        } catch (Exception e) {
            log.error("An unexpected error occurred while getCatalogByCategory" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.badRequest().body("Catalog not found.");
        }
    }


    //-----------------------------------------Get Catalog By Id-----------------------

    @GetMapping("/getById/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getCatalogById(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);

            if (catalog == null) {
                return ResponseEntity.badRequest().body("Catalog not found with ID: " + id);
            }
            CatalogDTO dto = new CatalogDTO();
            dto.setId(catalog.getId());
            dto.setName(catalog.getName());
            dto.setCategory(catalog.getCategory());
            dto.setFees(String.valueOf(catalog.getFees()));
            dto.setDescription(new String(catalog.getDescription()));
            dto.setStatus(catalog.getStatus());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {

            log.error("An unexpected error occurred while getCatalogById" + ExceptionUtils.getStackTrace(e
            ) + "Logged User :" + loginUser.getId());
            return ResponseEntity.badRequest().body("Catalog not found.");
        }
    }

    //-----------------------------------Delete Catalog By Id----------------------------

    @DeleteMapping("/deleteCatalog/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<String> deleteCatalog(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);
            if (catalog == null) {
                return ResponseEntity.badRequest().body("Catalog not found for this id:" + id);
            }
            if (catalog.getStatus() == 0) {
                return ResponseEntity.badRequest().body("Catalog is already deactivated.");
            }
            catalog.setStatus(0);
            catalog.setModifiedDate(LocalDate.now());
            catalogService.saveCatalog(catalog);
            return ResponseEntity.ok("Catalog has been deactivated.");
        } catch (Exception e) {
            log.error("An unexpected error occurred while deleteCatalog" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.badRequest().body("Catalog not found.");
        }
    }
    //------------------------------------------Reactive Catalog-------------------------

    @PutMapping("/reactivate/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<String> reactivateCatalog(@PathVariable long id) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);
            if (catalog == null) {
                return new ResponseEntity<>("Catalog not found for this id:" + id, HttpStatus.BAD_REQUEST);
            }
            if (catalog.getStatus() == 1) {
                return ResponseEntity.badRequest().body("Catalog is already active.");
            }
            catalog.setStatus(1);
            catalog.setModifiedDate(LocalDate.now());
            catalogService.saveCatalog(catalog);
            return ResponseEntity.ok("Catalog has been reactivated successfully.");
        } catch (Exception e) {
            log.error("An unexpected error occurred while reactiveCatalog" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return ResponseEntity.badRequest().body("Catalog not found.");
        }
    }
    //--------------------------------------Update Catalog By Id----------------------------

    @PutMapping("/updateCatalog/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> updateCatalog(@PathVariable long id, @Valid @ModelAttribute CatalogDTO catalogDTO) {
        User loginUser = null;
        try {
            loginUser = userService.getAuthenticateUser();

            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);
            if (catalog == null) {
                return new ResponseEntity<>("Catalog not found with Id:" + id, HttpStatus.UNAUTHORIZED);
            }

            if (!catalogDTO.getCategory().toUpperCase().matches("^(CONSULTATION_FEES|LABORATORY|GENERAL_SERVICE|SPECIALIST_SERVICE|ROOM)$")) {
                return new ResponseEntity<>("Category is not correct. Allowed values are: CONSULTATION_FEES, LABORATORY, GENERAL_SERVICE, SPECIALIST_SERVICE, ROOM", HttpStatus.BAD_REQUEST);
            }
            if (catalogDTO.getName() == null || !catalogDTO.getName().matches("^[A-Za-z ]+$")) {
                return new ResponseEntity<>("Name must contain only letters", HttpStatus.BAD_REQUEST);
            }

            if (catalogDTO.getFees() == null || !catalogDTO.getFees().toString().matches("^\\d+(\\.\\d+)?$")) {
                return new ResponseEntity<>("Fees must be a numeric value", HttpStatus.BAD_REQUEST);
            }

            if (catalog.getHospital() == null) {
                catalog.setHospital(loginUser.getHospital());
            }

            catalog.setCategory(catalogDTO.getCategory().toUpperCase());
            catalog.setName(catalogDTO.getName().trim().replaceAll("\\s+", " "));
            catalog.setFees(Double.valueOf(catalogDTO.getFees()));
            catalog.setDescription(catalogDTO.getDescription().getBytes());
            catalog.setModified(loginUser);
            catalog.setModifiedDate(LocalDate.now());
            catalog.setStatus(1);
            Catalog updatedCatalog = catalogService.saveCatalog(catalog);

            return ResponseEntity.ok(updatedCatalog);

        } catch (Exception e) {
            log.error("An unexpected error occurred while updateCatalog" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return new ResponseEntity<>("Error updating catalog: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}


//---------------------------------------------------------------------------------------------------------
