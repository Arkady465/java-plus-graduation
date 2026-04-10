package ru.practicum.main.ewm.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.ewm.api.ConflictException;
import ru.practicum.main.ewm.api.NotFoundException;
import ru.practicum.main.ewm.domain.EventEntity;
import ru.practicum.main.ewm.domain.ParticipationRequestEntity;
import ru.practicum.main.ewm.domain.UserEntity;
import ru.practicum.main.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.main.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.main.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.main.ewm.dto.request.RequestStatusAction;
import ru.practicum.main.ewm.model.Enums.EventState;
import ru.practicum.main.ewm.model.Enums.RequestStatus;
import ru.practicum.main.ewm.repository.ParticipationRequestRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class RequestService {

    private final ParticipationRequestRepository participationRequestRepository;
    private final UserService userService;
    private final EventService eventService;

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(long userId) {
        userService.requireUser(userId);
        return participationRequestRepository.findByRequesterIdOrderByIdAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public ParticipationRequestDto addParticipationRequest(long userId, long eventId) {
        UserEntity requester = userService.requireUser(userId);
        EventEntity e = eventService.requireEvent(eventId);

        if (e.getInitiator().getId().equals(userId)) {
            throw new ConflictException("For the requested operation the conditions are not met.",
                    "Initiator cannot participate in own event");
        }
        if (e.getState() != EventState.PUBLISHED) {
            throw new ConflictException("For the requested operation the conditions are not met.", "Event must be published");
        }
        if (participationRequestRepository.existsByRequesterIdAndEventIdAndStatusNot(userId, eventId, RequestStatus.CANCELED)) {
            throw new ConflictException("Integrity constraint has been violated.", "Duplicate request");
        }

        long confirmed = participationRequestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (e.getParticipantLimit() > 0 && confirmed >= e.getParticipantLimit()) {
            throw new ConflictException("For the requested operation the conditions are not met.",
                    "The participant limit has been reached");
        }

        ParticipationRequestEntity r = new ParticipationRequestEntity();
        r.setRequester(requester);
        r.setEvent(e);
        r.setCreated(LocalDateTime.now());
        boolean autoConfirm = !e.isRequestModeration() || e.getParticipantLimit() == 0;
        r.setStatus(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING);
        participationRequestRepository.save(r);
        return toDto(r);
    }

    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        userService.requireUser(userId);
        ParticipationRequestEntity r = participationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        if (!r.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " was not found");
        }
        r.setStatus(RequestStatus.CANCELED);
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(long userId, long eventId) {
        EventEntity e = eventService.requireEvent(eventId);
        if (!e.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return participationRequestRepository.findByEventIdOrderByIdAsc(eventId).stream()
                .map(this::toDto)
                .toList();
    }

    public EventRequestStatusUpdateResult changeRequestStatus(long userId, long eventId, EventRequestStatusUpdateRequest req) {
        EventEntity e = eventService.requireEvent(eventId);
        if (!e.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        for (Long id : req.getRequestIds()) {
            ParticipationRequestEntity r = participationRequestRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Request with id=" + id + " was not found"));
            if (!r.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Request with id=" + id + " was not found");
            }
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("For the requested operation the conditions are not met.", "Request must have status PENDING");
            }
            if (req.getStatus() == RequestStatusAction.CONFIRMED) {
                long conf = participationRequestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
                if (e.getParticipantLimit() > 0 && conf >= e.getParticipantLimit()) {
                    throw new ConflictException("For the requested operation the conditions are not met.",
                            "The participant limit has been reached");
                }
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(toDto(r));
            } else {
                r.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(r));
            }
        }

        if (e.getParticipantLimit() > 0
                && participationRequestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED) >= e.getParticipantLimit()) {
            List<ParticipationRequestEntity> pending = participationRequestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequestEntity p : pending) {
                p.setStatus(RequestStatus.REJECTED);
            }
        }
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private ParticipationRequestDto toDto(ParticipationRequestEntity r) {
        return new ParticipationRequestDto(
                r.getId(),
                r.getRequester().getId(),
                r.getEvent().getId(),
                r.getStatus().name(),
                r.getCreated()
        );
    }
}
