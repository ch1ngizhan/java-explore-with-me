package ru.practicum.category.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;

@Slf4j
@Validated
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@RequestBody @Valid NewCategoryDto newCategoryDto) {
        log.info(" POST /admin/categories - Создание новой категории: name='{}'",
                newCategoryDto.getName());
        log.debug(" Данные для создания категории: {}", newCategoryDto);

        CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);

        log.info("Категория успешно создана: id={}, name='{}'",
                createdCategory.getId(), createdCategory.getName());
        return createdCategory;
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable @Positive Long categoryId) {
        log.warn("DELETE /admin/categories/{} - Запрос на удаление категории", categoryId);
        log.debug("ID категории для удаления: {}", categoryId);

        categoryService.deleteCategory(categoryId);

        log.info("Категория id={} успешно удалена", categoryId);
    }

    @PatchMapping("/{categoryId}")
    public CategoryDto updateCategory(@PathVariable @Positive Long categoryId,
                                      @RequestBody @Valid NewCategoryDto newCategoryDto) {
        log.info("PATCH /admin/categories/{} - Обновление категории. Новое имя: '{}'",
                categoryId, newCategoryDto.getName());
        log.debug("Данные для обновления категории {}: {}", categoryId, newCategoryDto);

        CategoryDto updatedCategory = categoryService.updateCategory(categoryId, newCategoryDto);

        log.info("Категория id={} успешно обновлена. Новое имя: '{}'",
                categoryId, updatedCategory.getName());
        return updatedCategory;
    }
}