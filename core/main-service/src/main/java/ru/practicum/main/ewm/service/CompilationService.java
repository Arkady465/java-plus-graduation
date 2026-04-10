package ru.practicum.main.ewm.service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.ewm.api.NotFoundException;
import ru.practicum.main.ewm.domain.CompilationEntity;
import ru.practicum.main.ewm.domain.EventEntity;
import ru.practicum.main.ewm.dto.compilation.CompilationDto;
import ru.practicum.main.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.main.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.main.ewm.dto.event.EventShortDto;
import ru.practicum.main.ewm.repository.CompilationRepository;
import ru.practicum.main.ewm.service.mapper.EventDtoMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventService eventService;
    private final EventDtoMapper eventDtoMapper;

    public CompilationDto addCompilation(NewCompilationDto req) {
        CompilationEntity c = new CompilationEntity();
        c.setTitle(req.getTitle().trim());
        c.setPinned(Boolean.TRUE.equals(req.getPinned()));
        if (req.getEvents() != null) {
            for (Long id : req.getEvents()) {
                eventService.requireEvent(id);
            }
            c.getEventIds().addAll(req.getEvents());
        }
        compilationRepository.save(c);
        return toDto(c);
    }

    public void deleteCompilation(long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest req) {
        CompilationEntity c = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        if (req.getTitle() != null) {
            c.setTitle(req.getTitle().trim());
        }
        if (req.getPinned() != null) {
            c.setPinned(req.getPinned());
        }
        if (req.getEvents() != null) {
            for (Long id : req.getEvents()) {
                eventService.requireEvent(id);
            }
            c.getEventIds().clear();
            c.getEventIds().addAll(req.getEvents());
        }
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        return compilationRepository.findAll().stream()
                .filter(c -> pinned == null || c.isPinned() == pinned)
                .sorted(Comparator.comparing(CompilationEntity::getId))
                .skip(from)
                .limit(size)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompilationDto getCompilation(long compId) {
        CompilationEntity c = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return toDto(c);
    }

    private CompilationDto toDto(CompilationEntity c) {
        List<EventEntity> entities = c.getEventIds().stream().map(eventService::requireEvent).toList();
        Set<EventShortDto> events = new LinkedHashSet<>(eventDtoMapper.toShortDtoList(entities));
        return CompilationDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .pinned(c.isPinned())
                .events(events)
                .build();
    }
}
