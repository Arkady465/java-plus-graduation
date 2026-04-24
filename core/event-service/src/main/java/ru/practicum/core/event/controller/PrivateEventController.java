package ru.practicum.core.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.event.EventInitiatorIdFilter;
import ru.practicum.core.common.dto.event.EventShortDto;
import ru.practicum.core.common.dto.event.NewEventDto;
import ru.practicum.core.common.dto.event.UpdateEventRequest;
import ru.practicum.core.common.dto.page.PageRequestDto;
import ru.practicum.core.event.service.EventService;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
@RestController
public class PrivateEventController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@PathVariable Long userId,
                               @Valid @RequestBody NewEventDto newEventDto) {
        log.debug("POST /users/{}/events", userId);
        log.debug("Body: {}", newEventDto);

        return eventService.create(userId, newEventDto);
    }

    @GetMapping
    public Collection<EventShortDto> getEventsOfUser(@PathVariable Long userId,
                                                     EventInitiatorIdFilter eventInitiatorIdFilter,
                                                     PageRequestDto pageRequestDto) {
        log.debug("GET /users/{}/events", userId);
        log.debug("EventInitiatorIdFilter: {}", eventInitiatorIdFilter);
        log.debug("PageRequestDto: {}", pageRequestDto);

        Collection<EventShortDto> events = eventService.getEventsByUserId(eventInitiatorIdFilter,
                pageRequestDto.toPageable());
        log.debug("Found events: {}", events.size());

        return events;
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventFullDescription(@PathVariable Long userId,
                                                @PathVariable Long eventId) {
        log.debug("GET /users/{}/events/{}", userId, eventId);

        return eventService.getEventFullDescription(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByCreator(@PathVariable Long userId,
                                             @PathVariable Long eventId,
                                             @Valid @RequestBody UpdateEventRequest updateEventRequest) {
        log.debug("PATCH /users/{}/events/{}", userId, eventId);
        log.debug("Body: {}", updateEventRequest);

        return eventService.updateEventByCreator(userId, eventId, updateEventRequest);
    }
}