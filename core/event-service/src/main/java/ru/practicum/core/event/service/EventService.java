package ru.practicum.core.event.service;

import org.springframework.data.domain.Pageable;

import ru.practicum.core.common.dto.event.EventAdminFilter;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.event.EventInitiatorIdFilter;
import ru.practicum.core.common.dto.event.EventPublicFilter;
import ru.practicum.core.common.dto.event.EventShortDto;
import ru.practicum.core.common.dto.event.NewEventDto;
import ru.practicum.core.common.dto.event.UpdateEventRequest;
import ru.practicum.core.common.dto.page.PageRequestDto;

import java.util.Collection;
import java.util.List;

public interface EventService {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    Collection<EventShortDto> getEventsByUserId(EventInitiatorIdFilter eventInitiatorIdFilter, Pageable pageable);

    EventFullDto getEventFullDescription(Long userId, Long eventId);

    EventFullDto updateEventByCreator(Long userId, Long eventId, UpdateEventRequest updateEventRequest);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest updateEventRequest);

    List<EventFullDto> adminSearchEvents(EventAdminFilter eventAdminFilter, PageRequestDto pageRequestDto);

    List<EventShortDto> publicSearchEvents(EventPublicFilter eventPublicFilter, PageRequestDto pageRequestDto);

    EventFullDto getPublishedEvent(Long userId, Long eventId);

    EventFullDto getEvent(Long eventId);

    List<EventShortDto> getRecommendations(Long userId);

    void putLike(Long userId, Long eventId);
}