package ru.practicum.main.ewm.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.ewm.dto.event.EventFullDto;
import ru.practicum.main.ewm.dto.event.EventShortDto;
import ru.practicum.main.ewm.dto.event.NewEventDto;
import ru.practicum.main.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.main.ewm.service.EventService;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
public class PrivateEventsController {
    private final EventService service;

    @GetMapping
    public List<EventShortDto> getUserEvents(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getUserEvents(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable long userId, @Valid @RequestBody NewEventDto body) {
        return service.addUserEvent(userId, body);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable long userId, @PathVariable long eventId) {
        return service.getUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable long userId, @PathVariable long eventId,
                                    @Valid @RequestBody UpdateEventUserRequest body) {
        return service.updateUserEvent(userId, eventId, body);
    }
}

