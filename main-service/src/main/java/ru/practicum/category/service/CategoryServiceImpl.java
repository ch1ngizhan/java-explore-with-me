package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        checkCategoryNameUnique(newCategoryDto.getName(), null);

        Category category = categoryMapper.toEntity(newCategoryDto);

        Category savedCategory = categoryRepository.save(category);

        log.info("Создана категория: {}", savedCategory);
        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = getCategoryByIdOrThrow(categoryId);

        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("Невозможно удалить категорию с существующими событиями");
        }

        categoryRepository.delete(category);
        log.info("Удалена категория с ID: {}", categoryId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto) {
        Category category = getCategoryByIdOrThrow(categoryId);

        checkCategoryNameUnique(newCategoryDto.getName(), categoryId);

        categoryMapper.updateCategoryFromDto(newCategoryDto, category);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Обновлена категория: {}", updatedCategory);
        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    public List<CategoryDto> getCategories(Pageable pageable) {
        List<Category> categoriesPage = categoryRepository.findAllList(pageable);

        if (categoriesPage.isEmpty()) {
            return List.of();
        }

        List<CategoryDto> result = categoriesPage.stream()
                .map(categoryMapper::toDto)
                .toList();

        log.info("Найдено {} категорий", result.size());
        return result;
    }

    @Override
    public CategoryDto getCategoryById(Long categoryId) {
        Category category = getCategoryByIdOrThrow(categoryId);
        return categoryMapper.toDto(category);
    }

    @Override
    public Category getCategoryByIdOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + categoryId + " не найдена"));
    }

    private void checkCategoryNameUnique(String name, Long excludedId) {
        Optional<Category> existingCategory = (excludedId == null)
                ? categoryRepository.findByName(name)
                : categoryRepository.findByNameAndIdNot(name, excludedId);

        existingCategory.ifPresent(category -> {
            throw new ConflictException("Категория с названием '" + name + "' уже существует");
        });
    }
}
