package ru.practicum.core.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import ru.practicum.core.common.dto.event.CategoryDto;
import ru.practicum.core.event.dto.category.NewCategoryDto;
import ru.practicum.core.event.model.Category;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CategoryMapper {
    public static Category toNewCategory(NewCategoryDto request) {
        Category category = new Category();

        category.setName(request.getName());

        return category;
    }

    public static CategoryDto toCategoryDto(Category category) {
        CategoryDto dto = new CategoryDto();

        dto.setId(category.getId());
        dto.setName(category.getName());

        return dto;
    }

    public static void updateFields(Category category, CategoryDto dto) {
        category.setName(dto.getName());
    }
}