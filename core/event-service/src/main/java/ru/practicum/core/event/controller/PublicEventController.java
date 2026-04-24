package ru.practicum.core.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.practicum.client.StatsClient;
import ru.practicum.core.common.api.contract.EventApiContract;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.event.EventPublicFilter;
import ru.practicum.core.common.dto.event.EventShortDto;
import ru.practicum.core.common.dto.page.PageRequestDto;
import ru.practicum.core.event.service.EventService;
import ru.practicum.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/events")
@RestController
public class PublicEventController implements EventApiContract {
    private final EventService eventService;
    @Lazy
    private final StatsClient statsClient;

    private void saveHit(HttpServletRequest request) {
        statsClient.hit(new EndpointHitDto(
                "event-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        ));
    }

    @GetMapping
    public List<EventShortDto> getEvents(@Valid EventPublicFilter eventPublicFilter,
                                         PageRequestDto pageRequestDto,
                                         HttpServletRequest request) {
        log.debug("GET /events");
        log.debug("EventPublicFilter: {}", eventPublicFilter);
        log.debug("PageRequestDto: {}", pageRequestDto);
        log.debug("Client ip: {}", request.getRemoteAddr());

        saveHit(request);

        return eventService.publicSearchEvents(eventPublicFilter, pageRequestDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable Long eventId,
                                 HttpServletRequest request) {
        log.debug("GET /events/{}", eventId);
        log.debug("Client ip: {}", request.getRemoteAddr());

        saveHit(request);

        return eventService.getPublishedEvent(eventId);
    }

    // внутренний API без HttpServletRequest
    @Override
    @GetMapping("/inner/{eventId}")
    public EventFullDto getEventInner(@PathVariable Long eventId) {
        log.debug("GET /events/inner/{}", eventId);

        return eventService.getEvent(eventId);
    }
}