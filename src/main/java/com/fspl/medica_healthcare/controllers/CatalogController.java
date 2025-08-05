package com.fspl.medica_healthcare.controllers;


import com.fspl.medica_healthcare.dtos.CatalogDTO;
import com.fspl.medica_healthcare.models.Catalog;
import com.fspl.medica_healthcare.models.Category;
import com.fspl.medica_healthcare.models.Hospital;
import com.fspl.medica_healthcare.models.User;
import com.fspl.medica_healthcare.services.CatalogService;
import com.fspl.medica_healthcare.services.CategoryService;
import com.fspl.medica_healthcare.services.UserService;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import jakarta.validation.Valid;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    private static final Logger log = Logger.getLogger(CatalogController.class);

    //-----------------------------------------Create Catalog---------------------------

    @PostMapping("/addCatalog")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> createCatalog(@ModelAttribute @Valid CatalogDTO catalogDTO, @RequestParam("images") MultipartFile images) {
        User loginUser = null;
        try {
            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }


            Catalog existingCatalog = catalogService.getCatalogByNameAndCategory(loginUser.getHospital().getId(), catalogDTO.getCategory(), catalogDTO.getName());
            if (existingCatalog != null )
            {
                return new ResponseEntity<>("Catalog already exists in this category.", HttpStatus.BAD_REQUEST);
            }

            // Validate the name of the catalog (it should only contain letters)
            if (catalogDTO.getName() == null || catalogDTO.getName().equalsIgnoreCase("null")) {
                return new ResponseEntity<>("Name field is required", HttpStatus.BAD_REQUEST);
            } else if (!catalogDTO.getName().matches("^[A-Za-z -]+$")) {
                return new ResponseEntity<>("Name must contain only letters and hyphens.", HttpStatus.BAD_REQUEST);
            }


//            // Validate the fees (it should be greater than 0)
//            if (catalogDTO.getFees() ==null) {
//                return new ResponseEntity<>("Fees must be greater than zero", HttpStatus.BAD_REQUEST);
//            }
            if (catalogDTO.getFees() ==null) {
                return new ResponseEntity<>("Fees cannot be null", HttpStatus.BAD_REQUEST);
            } else if (!catalogDTO.getFees().toString().matches("^\\d+(\\.\\d+)?$")) {
                return new ResponseEntity<>("Fees must be a numeric value", HttpStatus.BAD_REQUEST);
            }

            double fees = Double.parseDouble(catalogDTO.getFees());
            if(fees<=0)
            {
                return new ResponseEntity<>("Fees must be greater than zero", HttpStatus.BAD_REQUEST);
            }
            if (catalogDTO.getDescription() == null || catalogDTO.getDescription().equalsIgnoreCase("null")) {
                return new ResponseEntity<>("Description cannot be null", HttpStatus.BAD_REQUEST);
            } else if (catalogDTO.getDescription().matches(".*[\\p{So}\\p{Cn}].*") || (catalogDTO.getDescription().matches("^\\d+$"))) {
                return new ResponseEntity<>("Invalid characters in description.", HttpStatus.BAD_REQUEST);
            }


            // Validate image (if provided) - check if it's an image and size is under 2MB
            if (images != null && !images.isEmpty()) {
                if (!Objects.requireNonNull(images.getContentType()).startsWith("image/")) {
                    return new ResponseEntity<>("Invalid file type. Only image files are allowed", HttpStatus.BAD_REQUEST);
                }
                if (images.getSize() > 2 * 1024 * 1024) { // 2MB limit
                    return new ResponseEntity<>("File size must be less than 2MB", HttpStatus.BAD_REQUEST);
                }
            }

            // Create a new Catalog object to store the data
            Catalog catalog = new Catalog();
            // Find or create the Category entity
            Category category = categoryService.findOrCreateCategory(catalogDTO.getCategory().toUpperCase());

            // Set the catalog details
            catalog.setName(catalogDTO.getName().trim().replaceAll("\\s+", " "));
            catalog.setCategory(category);
            catalog.setFees(fees);
            catalog.setDescription(catalogDTO.getDescription().getBytes());
            catalog.setImages(images.getBytes());
            catalog.setCreatedUser(loginUser);
            catalog.setModifiedUser(loginUser);
            catalog.setCreatedDate(LocalDate.now());
            catalog.setModifiedDate(LocalDate.now());
            catalog.setStatus(1);

            // If the hospital is not set, assign the hospital of the logged-in user
            if (catalog.getHospital() == null) {
                catalog.setHospital(loginUser.getHospital());
            }

            Boolean isSaved = catalogService.saveCatalog(catalog);
            if (isSaved) {
//                return ResponseEntity.ok(catalog);
                return new ResponseEntity<>("Catalog created Successfully",HttpStatus.OK);
            } else {
                return new ResponseEntity<>("An error occurred while creating the catalog.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while creating the Catalog" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId() + " Requested Data: " + catalogDTO);
            return ResponseEntity.badRequest().body("something went wrong...");
        }
    }

    //----------------------------------------Get All Catalog-----------------------------
    //    Retrieves all catalogs for hospital.
    @GetMapping("/getAllCatalog")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllCatalog() {
        User loginUser = null;
        try {
            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }

            // Fetch all catalogs associated with the hospital of the authenticated user
            List<Catalog> catalogs = catalogService.findAllCatalogByHospitalId(loginUser.getHospital().getId());
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("No catalogs available.");
            }

            // Map the Catalog objects to CatalogDTO objects for the response
            List<CatalogDTO> catalogDTOs = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();

                // Set the properties of the CatalogDTO from the Catalog entity
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory().getName());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setImage("/catalog/images/" + catalog.getId());

                dto.setStatus(catalog.getStatus());
                return dto;

            }).collect(Collectors.toList()); // Collect the mapped DTOs into a list

            // Return the list of CatalogDTO objects.
            return ResponseEntity.ok(catalogDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting all Catalog" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.status(400).body("something went wrong...");
        }
    }

    //------------------------------------------------Get All By Hospital Id------------------

    //   This method is used to Retrieves all catalog entries for a specific hospital by hospital ID.
    @GetMapping("/getCatalogbyHospitalId/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public synchronized ResponseEntity<?> getAllbyHospitalId(@PathVariable long id) {
        User loginUser = null;
        try {
            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }
            List<Catalog> catalogs;

            if ("SUPER_ADMIN".equals(loginUser.getRoles())) {
                catalogs = catalogService.findAllCatalogByHospitalId(id);
            } else {
                Hospital userHospital = loginUser.getHospital();
                if (userHospital == null) {
                    return ResponseEntity.status(403).body("You don't have an associated hospital.");
                }
                long userHospitalId = userHospital.getId();

                // If the requested hospital ID doesn't match the admin's hospital, return forbidden
                if (id != userHospitalId) {
                    return ResponseEntity.status(403).body("You can only access catalogs for your associated hospital.");
                }
                catalogs = catalogService.findAllCatalogByHospitalId(userHospitalId);
            }

            // If no catalogs are found for the hospital, return error message
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("Catalog not available for this hospital.");
            }
            List<CatalogDTO> catalogDTO = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory().getName());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setImage("/catalog/images/" + catalog.getId());
                dto.setStatus(catalog.getStatus());
                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(catalogDTO);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting all by catalog by hospital Id" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.status(404).body("Something went wrong..." + e.getMessage());
        }
    }

    //-----------------------------------Get All By Service Name-------------------------

    //    This method is used Retrieves catalog entries by service name.
    @GetMapping("/getCatalogbyServiceName/{name}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getAllByServiceName(@PathVariable String name) {
        User loginUser = null;
        try {
            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }
            // Fetch all catalogs associated with the given service name
            List<Catalog> catalogs = catalogService.findCatalogByName(loginUser.getHospital().getId(), name);

            //If no catalogs are found for the given service name, it will return an error message.
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("Catalog not available for this hospital.");
            }
            List<CatalogDTO> catalogDTO = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory().getName());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setImage("/catalog/images/" + catalog.getId());
                dto.setStatus(catalog.getStatus());
                return dto;
            }).collect(Collectors.toList());// collect the map catalogDto object into a list

            // Return the list of CatalogDTOs
            return ResponseEntity.ok(catalogDTO);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting all by ServiceName" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.status(400).body("something went wrong...");
        }
    }

    //-------------------------------------Get Catalog By Catagory-----------------------

    //    This method is used to Retrieves catalog entries by category for hospital.
    @GetMapping("/getCatalogbyCategory/{category}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getCatalogByCategory(@PathVariable String category) {
        User loginUser = null;
        try {
            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }

            // Fetch all catalogs associated with the given catagory.
            List<Catalog> catalogs = catalogService.findCatalogByCategory(loginUser.getHospital().getId(), category);

            //If no catalogs are found for the given category, it will return an error message.
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body("Catalogs not found for category: " + category);
            }
            // Transform the list of Catalog objects into a list of CatalogDTO objects
            List<CatalogDTO> catalogDTOs = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();
                dto.setId(catalog.getId());
                dto.setName(catalog.getName());
                dto.setCategory(catalog.getCategory().getName());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setDescription(new String(catalog.getDescription()));
                dto.setImage("/catalog/images/" + catalog.getId());
                dto.setStatus(catalog.getStatus());
                return dto;
            }).collect(Collectors.toList());// collect the map catalogDto object into a list

            // Return the list of CatalogDTOs
            return ResponseEntity.ok(catalogDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting Catalog by Category" + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId());
            return ResponseEntity.badRequest().body("something went wrong...");
        }
    }


    //-----------------------------------------Get Catalog By Id-----------------------

    //    This method is used to Retrieves a specific catalog entry by its ID.
    @GetMapping("/getCatalogbyId/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> getCatalogById(@PathVariable long id) {
        User loginUser = null;
        try {

            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }

            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);

            //If no catalogs are found for the given catalog id, it will return an error message.
            if (catalog == null) {
                return new ResponseEntity<>("Catalog not found with ID:" + id,HttpStatus.NOT_FOUND);
            }
            CatalogDTO dto = new CatalogDTO();
            dto.setId(catalog.getId());
            dto.setName(catalog.getName());
            dto.setCategory(catalog.getCategory().getName());
            dto.setFees(String.valueOf(catalog.getFees()));
            dto.setDescription(new String(catalog.getDescription()));
            dto.setImage("/catalog/images/" + catalog.getId());
            dto.setStatus(catalog.getStatus());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting catalog by id" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId());
            return ResponseEntity.badRequest().body("something went wrong..."+id);
        }
    }
    //-----------------------------------Active and Deactive Catalog By Id----------------------------

    //This method is used to Active and De-active a catalog entry by its ID.
    @PutMapping("/activeAndDeactivateCatalogbyId/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<String> activeAndDeactivateCatalog(@PathVariable long id) {
        User loginUser = null;
        String status;
        try {
            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }
            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);

            // If no catalogs are found for the given catalog id, it will return an error message.
            if (catalog == null) {
                return ResponseEntity.badRequest().body("Catalog not found for this id:" + id);
            }
            // Check if the catalog is already de-active
            if (catalog.getStatus() == 0) {
                catalog.setStatus(1);
                status = "Activated";
            } else {
                catalog.setStatus(0);
                status = "Deactivated";
            }
            // Update the catalog's status to de-active
            catalog.setModifiedDate(LocalDate.now());

            // Save the updated catalog
            catalogService.saveCatalog(catalog);
            return ResponseEntity.ok("Catalog has been " + status + ".");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred." + ExceptionUtils.getStackTrace(e) + "Logged User:" + loginUser.getId() + " Requested Data: " + id);
            return ResponseEntity.badRequest().body("something went wrong...");
        }
    }
    //--------------------------------------Update Catalog By Id----------------------------//
//    @PutMapping("/updateCatalogbyId/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    public synchronized ResponseEntity<?> updateCatalog(@PathVariable long id, @ModelAttribute @Valid CatalogDTO catalogDTO, @RequestParam("images") MultipartFile images) {
//        User loginUser = null;
//        try {
//
//            // Get the currently authenticated user
//            loginUser = userService.getAuthenticateUser();
//            if (loginUser == null) {
//                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
//            }
//
//            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);
//
//            // If no catalogs are found for the given catalog id, it will return an error message.
//            if (catalog == null) {
//                return new ResponseEntity<>("Catalog not found with Id:" + id, HttpStatus.NOT_FOUND);
//            }
//
//            Catalog existingCatalog = catalogService.getCatalogByNameAndCategory(loginUser.getHospital().getId(), catalogDTO.getCategory(), catalog.getName());
//
//            if (existingCatalog != null && existingCatalog.getId() != id)
//            {
//                return new ResponseEntity<>("A service with the same name already exists in this category.", HttpStatus.BAD_REQUEST);
//
//            }
//            if (catalog.getStatus() != 1) {
//                return new ResponseEntity<>("This Catalog is de-active! please active this catalog before update:", HttpStatus.BAD_REQUEST);
//            }
//            // Validate the name of the catalog (it should only contain letters)
//            if (catalogDTO.getName() == null || catalogDTO.getName().trim().isEmpty() || catalogDTO.getName().equalsIgnoreCase("null")) {
//                return new ResponseEntity<>("Name is required", HttpStatus.BAD_REQUEST);
//            } else if (catalogDTO.getName().trim().length() < 3) {
//                return new ResponseEntity<>("Name must be at least 3 characters long", HttpStatus.BAD_REQUEST);
//
//            } else if (!catalogDTO.getName().matches("^[A-Za-z ]+$")) {
//                return new ResponseEntity<>("Name must contain only letters", HttpStatus.BAD_REQUEST);
//            }
//            // Validate and parse fees from String
//            if (catalogDTO.getFees() == null || catalogDTO.getFees().isEmpty()) {
//                return new ResponseEntity<>("Fees are required", HttpStatus.BAD_REQUEST);
//            } else if (!catalogDTO.getFees().matches("^\\d+(\\.\\d+)?$")) {
//                return new ResponseEntity<>("Fees must be a valid numeric value", HttpStatus.BAD_REQUEST);
//            }
//
//            double fees = Double.parseDouble(catalogDTO.getFees());
//            if (fees <= 0) {
//                return new ResponseEntity<>("Fees must be greater than 0", HttpStatus.BAD_REQUEST);
//            }
//
//
//            // Validate images (if provided) - check if it's an image and size is under 2MB
//            if (images != null && !images.isEmpty()) {
//                if (!Objects.requireNonNull(images.getContentType()).startsWith("image/")) {
//                    return new ResponseEntity<>("Invalid file type. Only image files are allowed", HttpStatus.BAD_REQUEST);
//                }
//                if (images.getSize() > 2 * 1024 * 1024) { // 2MB limit
//                    return new ResponseEntity<>("File size must be less than 2MB", HttpStatus.BAD_REQUEST);
//                }
//            }
//            // If the catalog's hospital is not set, assign the logged-in user's hospital
//            if (catalog.getHospital() == null) {
//                catalog.setHospital(loginUser.getHospital());
//            }
//
//            if (catalogDTO.getCategory() == null || catalogDTO.getCategory().trim().isEmpty() || catalogDTO.getCategory().equalsIgnoreCase("null")) {
//                return new ResponseEntity<>("Category are required", HttpStatus.BAD_REQUEST);
//            } else if (!catalogDTO.getCategory().matches("^[A-Za-z ]+$")) {
//                return new ResponseEntity<>("Category must contain only letters", HttpStatus.BAD_REQUEST);
//            }
//
//            if (catalogDTO.getDescription() == null || catalogDTO.getDescription().equalsIgnoreCase("null") || catalogDTO.getDescription().trim().isEmpty()) {
//                return new ResponseEntity<>("Description is required", HttpStatus.BAD_REQUEST);
//            } else if (!catalogDTO.getDescription().matches("^[A-Za-z0-9 ]+$")) {
//                return new ResponseEntity<>("Description must contain only letters, numbers, and spaces", HttpStatus.BAD_REQUEST);
//            }
//
//
//            // Find or create the Category entity
//            Category category = categoryService.findOrCreateCategory(catalogDTO.getCategory().toUpperCase());
//            if (category == null) {
//                return new ResponseEntity<>("Category not found", HttpStatus.NOT_FOUND);
//            }
//
//            // Set the catalog's updated values from the request DTO
//            catalog.setCategory(category);
//            catalog.setName(catalogDTO.getName().trim().replaceAll("\\s+", " "));
//            catalog.setFees(fees);
//            catalog.setDescription(catalogDTO.getDescription().getBytes());
//            catalog.setImages(images.getBytes());
//            catalog.setModifiedUser(loginUser);
//            catalog.setModifiedDate(LocalDate.now());
//            catalog.setStatus(1);
//
//            //Saved updated catalog
//            Boolean isUpdated = catalogService.saveCatalog(catalog);
//            if (isUpdated) {
//                return ResponseEntity.ok(catalog);
//            } else {
//                return new ResponseEntity<>("An error occurred while updating the catalog.", HttpStatus.BAD_REQUEST);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("An unexpected error occurred while updating the Catalog" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId() + " Requested Data: " + catalogDTO);
//            return new ResponseEntity<>("something went wrong..." + e.getMessage(), HttpStatus.BAD_REQUEST);
//        }
//    }

    //    This method is used to Updates an existing catalog entry by its ID.
    @PutMapping("/updateCatalogbyId/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized ResponseEntity<?> updateCatalog(@PathVariable long id, @ModelAttribute @Valid CatalogDTO catalogDTO, @RequestParam("images") MultipartFile images) {
        User loginUser = null;
        try {

            // Get the currently authenticated user
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }

            Catalog catalog = catalogService.getCatalogById(loginUser.getHospital().getId(), id);

            // If no catalogs are found for the given catalog id, it will return an error message.
            if (catalog == null) {
                return new ResponseEntity<>("Catalog not found with Id:" + id, HttpStatus.NOT_FOUND);
            }

            Catalog existingCatalog = catalogService.getCatalogByNameAndCategory(loginUser.getHospital().getId(), catalogDTO.getCategory(), catalog.getName());

            if (existingCatalog != null && existingCatalog.getId() != id)
            {
                return new ResponseEntity<>("A service with the same name already exists in this category.", HttpStatus.BAD_REQUEST);

            }
            if (catalog.getStatus() != 1) {
                return new ResponseEntity<>("This Catalog is de-active! please active this catalog before update:", HttpStatus.BAD_REQUEST);
            }
            // Validate the name of the catalog (it should only contain letters)
            if (catalogDTO.getName() == null || catalogDTO.getName().trim().isEmpty() || catalogDTO.getName().equalsIgnoreCase("null")) {
                return new ResponseEntity<>("Name is required", HttpStatus.BAD_REQUEST);
            } else if (catalogDTO.getName().trim().length() < 3) {
                return new ResponseEntity<>("Name must be at least 3 characters long", HttpStatus.BAD_REQUEST);

            } else if (!catalogDTO.getName().matches("^[A-Za-z &-]+$")) {
                return new ResponseEntity<>("Name must contain only letters, hyphen and ampersand", HttpStatus.BAD_REQUEST);
            }
            // Validate and parse fees from String
            if (catalogDTO.getFees() == null || catalogDTO.getFees().isEmpty()) {
                return new ResponseEntity<>("Fees are required", HttpStatus.BAD_REQUEST);
            } else if (!catalogDTO.getFees().matches("^\\d+(\\.\\d+)?$")) {
                return new ResponseEntity<>("Fees must be a valid numeric value", HttpStatus.BAD_REQUEST);
            }

            double fees = Double.parseDouble(catalogDTO.getFees());
            if (fees <= 0) {
                return new ResponseEntity<>("Fees must be greater than 0", HttpStatus.BAD_REQUEST);
            }


            // Validate images (if provided) - check if it's an image and size is under 2MB
            if (images != null && !images.isEmpty()) {
                if (!Objects.requireNonNull(images.getContentType()).startsWith("image/")) {
                    return new ResponseEntity<>("Invalid file type. Only image files are allowed", HttpStatus.BAD_REQUEST);
                }
                if (images.getSize() > 2 * 1024 * 1024) { // 2MB limit
                    return new ResponseEntity<>("File size must be less than 2MB", HttpStatus.BAD_REQUEST);
                }
            }
            // If the catalog's hospital is not set, assign the logged-in user's hospital
            if (catalog.getHospital() == null) {
                catalog.setHospital(loginUser.getHospital());
            }

            if (catalogDTO.getCategory() == null || catalogDTO.getCategory().trim().isEmpty() || catalogDTO.getCategory().equalsIgnoreCase("null")) {
                return new ResponseEntity<>("Category are required", HttpStatus.BAD_REQUEST);
            } else if (!catalogDTO.getCategory().matches("^[A-Za-z &-]+$")) {
                return new ResponseEntity<>("Category must contain only letters, hyphen and ampersand", HttpStatus.BAD_REQUEST);
            }

            if (catalogDTO.getDescription() == null || catalogDTO.getDescription().equalsIgnoreCase("null") || catalogDTO.getDescription().trim().isEmpty()) {
                return new ResponseEntity<>("Description is required", HttpStatus.BAD_REQUEST);
            } else if (!catalogDTO.getDescription().matches("^[A-Za-z0-9 ]+$")) {
                return new ResponseEntity<>("Description must contain only letters, numbers, and spaces", HttpStatus.BAD_REQUEST);
            }


            // Find or create the Category entity
            Category category = categoryService.findOrCreateCategory(catalogDTO.getCategory().toUpperCase());
            if (category == null) {
                return new ResponseEntity<>("Category not found", HttpStatus.NOT_FOUND);
            }

            // Set the catalog's updated values from the request DTO
            catalog.setCategory(category);
            catalog.setName(catalogDTO.getName().trim().replaceAll("\\s+", " "));
            catalog.setFees(fees);
            catalog.setDescription(catalogDTO.getDescription().getBytes());
            catalog.setImages(images.getBytes());
            catalog.setModifiedUser(loginUser);
            catalog.setModifiedDate(LocalDate.now());
            catalog.setStatus(1);

            //Saved updated catalog
            Boolean isUpdated = catalogService.saveCatalog(catalog);
            if (isUpdated) {
                return ResponseEntity.ok(catalog);
            } else {
                return new ResponseEntity<>("An error occurred while updating the catalog.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred while updating the Catalog" + ExceptionUtils.getStackTrace(e) + "Logged User :" + loginUser.getId() + " Requested Data: " + catalogDTO);
            return new ResponseEntity<>("something went wrong..." + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    //--------------------------------------Get all active Catalogs ----------------------------//
    //This method is used for get the catalog list of active catalogs.
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/getAllActiveCatalogs")
    public synchronized ResponseEntity<?> getAllActiveCatalogs() {
        User loginUser = null;
        try {

            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();

            // Get the list of active catalogs from the catalog service
            List<Catalog> catalogs = catalogService.findActiveCatalogs();
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body(" No catalogs available.");
            }

            // Map the list of catalogs entities to catalogDto objects
            List<CatalogDTO> catalogDTO = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();

                dto.setId((catalog.getId()));
                dto.setCategory(catalog.getCategory().getName());
                dto.setName(catalog.getName());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setStatus(catalog.getStatus());
                dto.setDescription(new String(catalog.getDescription()));
                dto.setImage("/catalog/images/" + catalog.getId());
                return dto;
            }).collect(Collectors.toList()); // Collect the mapped DTOs into a list

            // If the list of active catalog is empty, return a response indicating no active catalog are available
            if (catalogs.isEmpty()) {
                return new ResponseEntity<>("Active catalogs are not available", HttpStatus.BAD_REQUEST);
            }

            // Return the list of catalog DTOs if there are active catalog
            return ResponseEntity.ok(catalogDTO);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting all Active Catalogs" + ExceptionUtils.getStackTrace(e) + "Logged User : " + loginUser.getId());
            return ResponseEntity.badRequest().body("something went wrong...");
        }
    }

    //--------------------------------------Get all deactive catalogs ----------------------------//
    //This method is used for get the catalog list of active catalogs.
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/getAllDeactiveCatalogs")
    public synchronized ResponseEntity<?> getAllDeactivatedCatalogs() {
        User loginUser = null;
        try {
            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            // Get the list of de-active catalogs from the catalog service
            List<Catalog> catalogs = catalogService.findDeactiveCatalogs();
            if (catalogs.isEmpty()) {
                return ResponseEntity.status(404).body(" No catalogs available.");
            }
            // Map the list of catalogs entities to catalogDto objects
            List<CatalogDTO> catalogDTO = catalogs.stream().map(catalog -> {
                CatalogDTO dto = new CatalogDTO();

                dto.setId((catalog.getId()));
                dto.setCategory(catalog.getName());
                dto.setName(catalog.getName());
                dto.setFees(String.valueOf(catalog.getFees()));
                dto.setStatus(catalog.getStatus());
                dto.setDescription(new String(catalog.getDescription()));
                dto.setImage("/catalog/images/" + catalog.getId());
                return dto;
            }).collect(Collectors.toList()); // Collect the mapped DTOs into a list

            // If the list of de-active catalog is empty, return a response indicating no de-active catalog are available
            if (catalogs.isEmpty()) {
                return new ResponseEntity<>("De-active catalogs are not available", HttpStatus.BAD_REQUEST);
            }

            // Return the list of catalog DTOs if there are de-active catalog
            return ResponseEntity.ok(catalogDTO);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting all De-Active Catalogs" + ExceptionUtils.getStackTrace(e) + "Logged User : " + loginUser.getId());
            return ResponseEntity.badRequest().body("something went wrong...");
        }
    }


    //--------------------------------------Get catalog image by id----------------------------//

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    @GetMapping("/images/{id}")
    public Object getCatalogImage(@PathVariable long id) {
        User loginUser = null;
        try {
            // Fetch the authenticated user from the user service
            loginUser = userService.getAuthenticateUser();
            if (loginUser == null) {
                return new ResponseEntity<>("User is not authenticated", HttpStatus.UNAUTHORIZED);
            }

            byte[] imageData = (byte[]) catalogService.getImageById(loginUser.getHospital().getId(),id);

            if(imageData == null)
            {
                return new ResponseEntity<>("Image not found. ", HttpStatus.NOT_FOUND);
            }
            // You can set the content type based on your image format, e.g., JPEG or PNG
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(imageData.length);

            return ResponseEntity.ok().headers(headers).body(imageData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getting catalog images" + ExceptionUtils.getStackTrace(e) + "Logged User : " + loginUser.getId());
            return ResponseEntity.badRequest().body("something went wrong...");
        }
    }

}

//---------------------------------------------------------------------------------------------------------



