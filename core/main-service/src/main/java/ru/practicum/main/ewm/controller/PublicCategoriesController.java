package ru.practicum.main.ewm.controller;

import java.util.List;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.ewm.dto.Dtos.CategoryDto;
import ru.practicum.main.ewm.service.EwmService;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class PublicCategoriesController {
    private final EwmService service;

    @GetMapping
    public List<CategoryDto> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getCategories(from, size);
    }

    @GetMapping("/{catId}")
    public CategoryDto getOne(@PathVariable long catId) {
        return service.getCategory(catId);
    }
}

