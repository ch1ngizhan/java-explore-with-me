package ru.practicum.category.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.service.CategoryService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getCategories(
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);

        log.info("GET /categories - Получение списка категорий (пагинация: from={}, size={})", from, size);
        log.debug("Параметры пагинации: page={}, size={}", from / size, size);

        List<CategoryDto> categories = categoryService.getCategories(pageable);

        log.info("Найдено {} категорий (from={}, size={})", categories.size(), from, size);
        log.debug("Список ID найденных категорий: {}",
                categories.stream().map(CategoryDto::getId).toList());

        return categories;
    }

    @GetMapping("/{categoryId}")
    public CategoryDto getCategoryById(@PathVariable @Positive Long categoryId) {
        log.info("GET /categories/{} - Поиск категории по ID", categoryId);

        CategoryDto category = categoryService.getCategoryById(categoryId);

        if (category != null) {
            log.info("Найдена категория: id={}, name='{}'",
                    category.getId(), category.getName());
        } else {
            log.warn("Категория с id={} не найдена", categoryId);
        }

        return category;
    }
}