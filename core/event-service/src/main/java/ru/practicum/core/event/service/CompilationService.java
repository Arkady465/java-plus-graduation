package ru.practicum.core.event.service;

import ru.practicum.core.event.dto.compilation.CompilationDto;
import ru.practicum.core.event.dto.compilation.CompilationParam;
import ru.practicum.core.event.dto.compilation.NewCompilationDto;
import ru.practicum.core.event.dto.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto create(NewCompilationDto request);

    void deleteById(Long compId);

    CompilationDto update(Long compId, UpdateCompilationRequest request);

    List<CompilationDto> getCompilations(CompilationParam param);

    CompilationDto getById(Long compId);
}