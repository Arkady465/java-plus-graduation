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
import ru.practicum.main.ewm.dto.compilation.CompilationDto;
import ru.practicum.main.ewm.service.CompilationService;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
public class PublicCompilationsController {
    private final CompilationService service;

    @GetMapping
    public List<CompilationDto> getAll(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getCompilations(pinned, from, size);
    }

    @GetMapping("/{compId}")
    public CompilationDto getOne(@PathVariable long compId) {
        return service.getCompilation(compId);
    }
}

