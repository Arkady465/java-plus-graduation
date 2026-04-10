package ru.practicum.main.ewm.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.main.ewm.service.RequestService;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class PrivateRequestsController {
    private final RequestService service;

    @GetMapping
    public List<ParticipationRequestDto> getUserRequests(@PathVariable long userId) {
        return service.getUserRequests(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto add(@PathVariable long userId, @RequestParam long eventId) {
        return service.addParticipationRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancel(@PathVariable long userId, @PathVariable long requestId) {
        return service.cancelRequest(userId, requestId);
    }
}

