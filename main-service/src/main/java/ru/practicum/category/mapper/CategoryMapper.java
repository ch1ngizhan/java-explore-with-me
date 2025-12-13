package ru.practicum.category.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;
import ru.practicum.exception.ValidationException;

@Slf4j
@UtilityClass
public class CategoryMapper {

    public static Category toEntity(NewCategoryDto newCategoryDto) {
        validateNewCategoryDto(newCategoryDto);

        Category category = new Category();
        category.setName(newCategoryDto.getName().trim());
        log.debug("Создана сущность Category из NewCategoryDto: {}", category);
        return category;
    }

    public static CategoryDto toDto(Category category) {
        if (category == null) {
            log.warn("Попытка преобразования null Category в DTO");
            return null;
        }

        CategoryDto dto = CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();

        log.debug("Преобразована Category в CategoryDto: {}", dto);
        return dto;
    }

    public static void updateCategoryFromDto(NewCategoryDto newCategoryDto, Category category) {
        if (newCategoryDto == null || category == null) {
            log.warn("Попытка обновления Category из null DTO или null Category");
            return;
        }

        String newName = newCategoryDto.getName();
        if (newName != null && !newName.isBlank()) {
            String trimmedName = newName.trim();
            if (!trimmedName.equals(category.getName())) {
                log.debug("Обновление названия категории с '{}' на '{}'",
                        category.getName(), trimmedName);
                category.setName(trimmedName);
            }
        }
    }

    public static NewCategoryDto toNewCategoryDto(Category category) {
        if (category == null) {
            return null;
        }

        return NewCategoryDto.builder()
                .name(category.getName())
                .build();
    }

    private static void validateNewCategoryDto(NewCategoryDto dto) {
        if (dto == null) {
            throw new ValidationException("NewCategoryDto не может быть null");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("Название категории не может быть пустым");
        }

        String trimmedName = dto.getName().trim();
        if (trimmedName.length() < 1 || trimmedName.length() > 50) {
            throw new ValidationException(
                    "Название категории должно содержать от 1 до 50 символов. Получено: " + trimmedName.length()
            );
        }
    }
}