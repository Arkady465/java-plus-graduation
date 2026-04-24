package ru.practicum.core.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import ru.practicum.core.common.dto.event.EventAdminFilter;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.event.UpdateEventRequest;
import ru.practicum.core.common.dto.page.PageRequestDto;
import ru.practicum.core.event.service.EventService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/events")
public class AdminEventController {
    private final EventService eventService;

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByAdmin(@PathVariable Long eventId,
                                           @Valid @RequestBody UpdateEventRequest updateEventRequest) {
        log.debug("PATCH /admin/events/{}", eventId);
        log.debug("Body: {}", updateEventRequest);

        EventFullDto eventFullDto = eventService.updateEventByAdmin(eventId, updateEventRequest);
        log.debug("Events: {}", eventFullDto);

        return eventFullDto;
    }

    @GetMapping
    public List<EventFullDto> getEventsAdmin(EventAdminFilter eventAdminFilter,
                                             PageRequestDto pageRequestDto) {
        log.debug("GET /admin/events");
        log.debug("EventAdminFilter: {}", eventAdminFilter);
        log.debug("PageRequestDto: {}", pageRequestDto);

        return eventService.adminSearchEvents(eventAdminFilter, pageRequestDto);
    }
}