package ru.practicum.core.event.service;

import ru.practicum.core.common.dto.event.CategoryDto;
import ru.practicum.core.event.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto create(NewCategoryDto request);

    void deleteById(Long categoryId);

    CategoryDto update(Long categoryId, CategoryDto request);

    List<CategoryDto> find(int from, int size);

    CategoryDto findById(Long categoryId);
}