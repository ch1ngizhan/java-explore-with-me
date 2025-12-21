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
        log.debug("Запрос на создание категории: name={}", newCategoryDto.getName());

        checkCategoryNameUnique(newCategoryDto.getName(), null);

        Category category = categoryMapper.toEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Категория успешно создана: id={}, name={}",
                savedCategory.getId(), savedCategory.getName());

        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.debug("Запрос на удаление категории: id={}", categoryId);

        Category category = getCategoryByIdOrThrow(categoryId);

        if (eventRepository.existsByCategoryId(categoryId)) {
            log.warn("Попытка удалить категорию с событиями: id={}", categoryId);
            throw new ConflictException("Невозможно удалить категорию с существующими событиями");
        }

        categoryRepository.delete(category);
        log.info("Категория удалена: id={}", categoryId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto) {
        log.debug("Запрос на обновление категории: id={}, newName={}",
                categoryId, newCategoryDto.getName());

        Category category = getCategoryByIdOrThrow(categoryId);

        checkCategoryNameUnique(newCategoryDto.getName(), categoryId);

        categoryMapper.updateCategoryFromDto(newCategoryDto, category);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Категория обновлена: id={}, name={}",
                updatedCategory.getId(), updatedCategory.getName());

        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    public List<CategoryDto> getCategories(Pageable pageable) {
        log.debug("Запрос списка категорий: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        List<Category> categories = categoryRepository.findAllList(pageable);

        if (categories.isEmpty()) {
            log.debug("Категории не найдены");
            return List.of();
        }

        List<CategoryDto> result = categories.stream()
                .map(categoryMapper::toDto)
                .toList();

        log.info("Найдено категорий: {}", result.size());
        return result;
    }

    @Override
    public CategoryDto getCategoryById(Long categoryId) {
        log.debug("Запрос категории по id={}", categoryId);

        Category category = getCategoryByIdOrThrow(categoryId);
        return categoryMapper.toDto(category);
    }

    @Override
    public Category getCategoryByIdOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Категория не найдена: id={}", categoryId);
                    return new NotFoundException("Категория с id " + categoryId + " не найдена");
                });
    }

    private void checkCategoryNameUnique(String name, Long excludedId) {
        log.debug("Проверка уникальности имени категории: name={}, excludedId={}", name, excludedId);

        Optional<Category> existingCategory = (excludedId == null)
                ? categoryRepository.findByName(name)
                : categoryRepository.findByNameAndIdNot(name, excludedId);

        existingCategory.ifPresent(category -> {
            log.warn("Конфликт имени категории: name={}, existingId={}",
                    name, category.getId());
            throw new ConflictException("Категория с названием '" + name + "' уже существует");
        });
    }
}
