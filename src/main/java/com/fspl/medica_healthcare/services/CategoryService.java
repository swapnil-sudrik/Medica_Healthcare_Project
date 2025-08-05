package com.fspl.medica_healthcare.services;

import com.fspl.medica_healthcare.models.Category;
import com.fspl.medica_healthcare.repositories.CategoryRepository;
import com.fspl.medica_healthcare.utils.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    private static final Logger log = Logger.getLogger(CategoryService.class);


    public Category getCategoryByName(String name) {
        try {
            List<Category> categories = categoryRepository.findByName(name);
            return categories.isEmpty() ? null : categories.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while getCategoryByName" + ExceptionUtils.getStackTrace(e) +
                    "Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }
    // Find or create a category by name
    public Category findOrCreateCategory(String categoryName) {
        try {
            List<Category> existingCategory = categoryRepository.findByName(categoryName);
            if (!existingCategory.isEmpty()) {
                return existingCategory.get(0);
            } else {
                Category newCategory = new Category();
                newCategory.setName(categoryName);
                return categoryRepository.save(newCategory);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred while findOrCreateCategory" + ExceptionUtils.getStackTrace(e) +
                    "Logged User" + userService.getAuthenticateUser().getId());
            return null;
        }
    }

}