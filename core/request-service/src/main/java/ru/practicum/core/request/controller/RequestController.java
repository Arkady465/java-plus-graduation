package ru.practicum.core.request.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.practicum.core.common.api.contract.RequestApiContract;
import ru.practicum.core.common.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.core.common.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.core.common.dto.request.ParticipationRequestDto;
import ru.practicum.core.request.service.RequestService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class RequestController implements RequestApiContract {
    private final RequestService requestService;

    @GetMapping("/users/{userId}/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId) {
        log.debug("GET /users/{}/requests", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addParticipationRequest(@PathVariable @Positive Long userId,
                                                           @RequestParam @Positive Long eventId) {
        log.debug("POST /users/{}/requests?eventId={}", userId, eventId);
        return requestService.addParticipationRequest(userId, eventId);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long requestId) {
        log.debug("PATCH /users/{}/requests/{}/cancel", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> checkUserEventParticipation(@PathVariable Long userId,
                                                                     @PathVariable Long eventId) {
        log.debug("GET /users/{}/events/{}/requests", userId, eventId);

        return requestService.getEventParticipants(userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeStatusRequest(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @Valid @RequestBody EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        log.debug("PATCH /users/{}/events/{}/requests", userId, eventId);
        log.debug("Body: {}", eventRequestStatusUpdateRequest);

        return requestService.changeRequestStatus(userId,
                eventId,
                eventRequestStatusUpdateRequest);
    }

    @Override
    @GetMapping("/requests/countConfirmed")
    public Map<Long, Long> countConfirmedRequests(@RequestParam List<Long> eventIds) {
        log.debug("GET /requests/countConfirmed for events: {}", eventIds);

        return requestService.countConfirmedRequestsForEvents(eventIds);
    }
}