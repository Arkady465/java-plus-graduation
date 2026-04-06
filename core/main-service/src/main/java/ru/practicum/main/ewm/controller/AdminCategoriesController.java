package ru.practicum.main.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.ewm.dto.Dtos.CategoryDto;
import ru.practicum.main.ewm.dto.Dtos.NewCategoryDto;
import ru.practicum.main.ewm.service.EwmService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoriesController {
    private final EwmService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto add(@Valid @RequestBody NewCategoryDto body) {
        return service.addCategory(body);
    }

    @PatchMapping("/{catId}")
    public CategoryDto update(@PathVariable long catId, @Valid @RequestBody CategoryDto body) {
        return service.updateCategory(catId, body);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long catId) {
        service.deleteCategory(catId);
    }
}

