package ru.practicum.core.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.core.common.api.client.UserClient;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.event.dto.compilation.CompilationDto;
import ru.practicum.core.event.dto.compilation.CompilationParam;
import ru.practicum.core.event.dto.compilation.NewCompilationDto;
import ru.practicum.core.event.dto.compilation.UpdateCompilationRequest;
import ru.practicum.core.common.dto.event.EventShortDto;
import ru.practicum.core.event.mapper.EventMapper;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.mapper.CompilationMapper;
import ru.practicum.core.event.model.Compilation;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.repository.CompilationRepository;
import ru.practicum.core.common.api.client.CommentClient;
import ru.practicum.core.common.api.client.RequestClient;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    private final EventMapper eventMapper;

    private final CommentClient commentClient;
    private final UserClient userClient;
    private final RequestClient requestClient;

    @Override
    public CompilationDto create(NewCompilationDto request) {
        log.debug("Create compilation request: {}", request);

        Set<Event> events = new HashSet<>();
        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
        }

        Compilation compilation = compilationRepository.save(CompilationMapper.toNewCompilation(request, events));
        log.info("Compilation created: {}", compilation);

        Set<EventShortDto> eventShortDtos = buildEventShortDtos(events);
        return CompilationMapper.toDto(compilation, eventShortDtos);
    }

    @Override
    public void deleteById(Long compId) {
        log.debug("Delete compilation with id = {}", compId);

        if (!compilationRepository.existsById(compId)) {
            log.warn("Compilation with id = {} not found", compId);
            throw new NotFoundException(String.format("Compilation with id = %d not found", compId));
        }

        compilationRepository.deleteById(compId);
        log.info("Compilation with id = {} deleted", compId);
    }

    @Override
    public CompilationDto update(Long compId, UpdateCompilationRequest request) {
        log.debug("Update compilation with id = {}: {}", compId, request);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id = %d not found", compId)));

        Set<Event> events = compilation.getEvents();
        if (request.hasEvents()) {
            if (request.getEvents() != null && !request.getEvents().isEmpty()) {
                events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
            } else {
                events = new HashSet<>();
            }
        }

        CompilationMapper.updateFields(compilation, request, events);
        compilationRepository.save(compilation);
        log.info("Compilation updated: {}", compilation);

        Set<EventShortDto> eventShortDtos = buildEventShortDtos(events);
        return CompilationMapper.toDto(compilation, eventShortDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(CompilationParam param) {
        log.debug("Get compilations: {}", param);

        int page = param.from() / param.size();
        Pageable pageable = PageRequest.of(page, param.size());

        List<Compilation> compilations;
        if (param.pinned() != null) {
            compilations = compilationRepository.findByPinned(param.pinned(), pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return compilations.stream()
                .map(compilation -> {
                    Set<EventShortDto> eventShortDtos = buildEventShortDtos(compilation.getEvents());
                    return CompilationMapper.toDto(compilation, eventShortDtos);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getById(Long compId) {
        log.debug("Get compilation with id = {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id = %d not found", compId)));

        Set<EventShortDto> eventShortDtos = buildEventShortDtos(compilation.getEvents());
        return CompilationMapper.toDto(compilation, eventShortDtos);
    }

    // TODO: GET OR DEFAULT
    private Set<EventShortDto> buildEventShortDtos(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return new HashSet<>();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> confirmedRequestsMap = requestClient.countConfirmedRequests(eventIds);
        log.debug("Confirmed requests map: {}", confirmedRequestsMap);

        Map<Long, Long> commentsMap = commentClient.countByEvents(events.stream()
                        .map(Event::getId).toList());
        log.debug("Comments map: {}", commentsMap);

        Map<Long, UserShortDto> initiatorsMap = userClient.getShortByIdIn(events.stream()
                .map(Event::getInitiator).toList()).stream().collect(Collectors.toMap(UserShortDto::getId, user -> user));
        Map<Long, UserShortDto> eventInitiatorsMap = events.stream()
                .collect(Collectors.toMap(Event::getId, event -> initiatorsMap.get(event.getInitiator())));

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    Double rating = 0.0;
                    Long comments = commentsMap.getOrDefault(event.getId(), 0L);
                    UserShortDto initiator = eventInitiatorsMap.get(event.getInitiator());
                    return eventMapper.toShortDto(event, confirmedRequests, rating, comments, initiator);
                })
                .collect(Collectors.toSet());
    }
}