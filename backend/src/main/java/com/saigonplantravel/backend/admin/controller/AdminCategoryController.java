package com.saigonplantravel.backend.admin.controller;

import com.saigonplantravel.backend.place.dto.AdminCategoryResponse;
import com.saigonplantravel.backend.place.dto.CategoryCreateRequest;
import com.saigonplantravel.backend.place.dto.CategoryUpdateRequest;
import com.saigonplantravel.backend.place.exception.CategoryAlreadyExistsException;
import com.saigonplantravel.backend.place.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    String categoriesPage(Model model) {
        model.addAttribute("categories", categoryService.getCategoriesForAdministration());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    String newCategoryPage(Model model) {
        model.addAttribute("categoryForm", new CategoryCreateRequest(null, null, null));
        return "admin/categories/create-form";
    }

    @PostMapping
    String createCategory(
            @Valid @ModelAttribute("categoryForm") CategoryCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin/categories/create-form";
        }

        try {
            AdminCategoryResponse category = categoryService.createCategory(request);
            return "redirect:/admin/categories#category-" + category.slug();
        } catch (CategoryAlreadyExistsException exception) {
            if (exception.getFieldName() == null) {
                bindingResult.reject("category.duplicate", exception.getMessage());
            } else {
                bindingResult.rejectValue(exception.getFieldName(), "category.duplicate", exception.getMessage());
            }
            return "admin/categories/create-form";
        }
    }

    @GetMapping("/{slug}/edit")
    String editCategoryPage(@PathVariable String slug, Model model) {
        AdminCategoryResponse category = categoryService.getCategoryForAdministrationBySlug(slug);
        model.addAttribute("categoryForm", new CategoryUpdateRequest(category.name(), category.description()));
        model.addAttribute("categorySlug", category.slug());
        return "admin/categories/edit-form";
    }

    @PostMapping("/{slug}")
    String updateCategory(
            @PathVariable String slug,
            @Valid @ModelAttribute("categoryForm") CategoryUpdateRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categorySlug", slug);
            return "admin/categories/edit-form";
        }

        try {
            AdminCategoryResponse category = categoryService.updateCategory(slug, request);
            return "redirect:/admin/categories#category-" + category.slug();
        } catch (CategoryAlreadyExistsException exception) {
            bindingResult.rejectValue("name", "category.duplicate", exception.getMessage());
            model.addAttribute("categorySlug", slug);
            return "admin/categories/edit-form";
        }
    }
}
