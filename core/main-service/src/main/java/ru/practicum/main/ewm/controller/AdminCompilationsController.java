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
import ru.practicum.main.ewm.dto.Dtos.CompilationDto;
import ru.practicum.main.ewm.dto.Dtos.NewCompilationDto;
import ru.practicum.main.ewm.dto.Dtos.UpdateCompilationRequest;
import ru.practicum.main.ewm.service.EwmService;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationsController {
    private final EwmService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto add(@Valid @RequestBody NewCompilationDto body) {
        return service.addCompilation(body);
    }

    @PatchMapping("/{compId}")
    public CompilationDto update(@PathVariable long compId, @Valid @RequestBody UpdateCompilationRequest body) {
        return service.updateCompilation(compId, body);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long compId) {
        service.deleteCompilation(compId);
    }
}

