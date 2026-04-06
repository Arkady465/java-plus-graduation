package ru.practicum.main.ewm.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.ewm.dto.Dtos.EventRequestStatusUpdateRequest;
import ru.practicum.main.ewm.dto.Dtos.EventRequestStatusUpdateResult;
import ru.practicum.main.ewm.dto.Dtos.ParticipationRequestDto;
import ru.practicum.main.ewm.service.EwmService;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class PrivateEventParticipantsController {
    private final EwmService service;

    @GetMapping
    public List<ParticipationRequestDto> getParticipants(@PathVariable long userId, @PathVariable long eventId) {
        return service.getEventParticipants(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult changeStatus(
            @PathVariable long userId,
            @PathVariable long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest body
    ) {
        return service.changeRequestStatus(userId, eventId, body);
    }
}

