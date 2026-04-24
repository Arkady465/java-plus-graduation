package ru.practicum.core.request.service;

import ru.practicum.core.common.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.core.common.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.core.common.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest request);

    Map<Long, Long> countConfirmedRequestsForEvents(List<Long> eventIds);

    List<ParticipationRequestDto> getByEventAndRequester(Long eventId, Long requesterId);
}