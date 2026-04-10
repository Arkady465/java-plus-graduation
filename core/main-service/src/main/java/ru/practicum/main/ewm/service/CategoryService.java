package ru.practicum.main.ewm.service;

import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.ewm.api.ConflictException;
import ru.practicum.main.ewm.api.NotFoundException;
import ru.practicum.main.ewm.domain.CategoryEntity;
import ru.practicum.main.ewm.dto.category.CategoryDto;
import ru.practicum.main.ewm.dto.category.NewCategoryDto;
import ru.practicum.main.ewm.repository.CategoryRepository;
import ru.practicum.main.ewm.repository.EventRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public CategoryDto addCategory(NewCategoryDto req) {
        String name = req.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Integrity constraint has been violated.", "Category name must be unique");
        }
        CategoryEntity c = new CategoryEntity();
        c.setName(name);
        categoryRepository.save(c);
        return toDto(c);
    }

    public CategoryDto updateCategory(long catId, CategoryDto dto) {
        CategoryEntity c = requireCategory(catId);
        String name = dto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, catId)) {
            throw new ConflictException("Integrity constraint has been violated.", "Category name must be unique");
        }
        c.setName(name);
        return toDto(c);
    }

    public void deleteCategory(long catId) {
        requireCategory(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("For the requested operation the conditions are not met.", "The category is not empty");
        }
        categoryRepository.deleteById(catId);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(int from, int size) {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(CategoryEntity::getId))
                .skip(from)
                .limit(size)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(long catId) {
        return toDto(requireCategory(catId));
    }

    @Transactional(readOnly = true)
    public CategoryEntity requireCategory(long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    private CategoryDto toDto(CategoryEntity c) {
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        return dto;
    }
}
