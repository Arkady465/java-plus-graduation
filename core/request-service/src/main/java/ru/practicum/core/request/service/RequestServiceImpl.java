package ru.practicum.core.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.core.common.api.client.EventClient;
import ru.practicum.core.common.api.client.UserClient;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.core.common.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.core.common.dto.request.ParticipationRequestDto;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.exception.AccessViolationException;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.common.exception.ValidationException;
import ru.practicum.core.request.mapper.RequestMapper;
import ru.practicum.core.common.dto.event.EventState;
import ru.practicum.core.request.model.Request;
import ru.practicum.core.common.dto.request.RequestStatus;
import ru.practicum.core.request.repository.RequestRepository;
import ru.practicum.ewm.stats.proto.user.ActionTypeProto;
import ru.practicum.stats.client.CollectorClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;

    private final UserClient userClient;
    private final EventClient eventClient;

    private final CollectorClient collectorClient;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.debug("Get requests for user with id = {}", userId);

        userClient.getById(userId);

        return requestRepository.findByRequester(userId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        log.debug("Add participation request: userId = {}, eventId = {}", userId, eventId);

        UserDto requester = userClient.getById(userId);
        log.debug("Requester: {}", requester);
        EventFullDto eventFullDto = eventClient.getEventInner(eventId);
        log.debug("Event: {}", eventFullDto);

        if (requestRepository.findByRequesterAndEvent(userId, eventId).isPresent()) {
            throw new ConflictException("Request already exists");
        }

        if (eventFullDto.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in own event");
        }

        log.debug("Event state: {}", eventFullDto.getState());
        if (!eventFullDto.getState().equals(EventState.PUBLISHED.name())) {
            throw new ConflictException("Event is not published");
        }

        if (eventFullDto.getParticipantLimit() > 0) {
            Long confirmedCount = requestRepository.countConfirmedRequestsByEvent(eventId);
            if (confirmedCount >= eventFullDto.getParticipantLimit()) {
                throw new ConflictException("Participant limit reached");
            }
        }

        Request request = new Request();
        request.setCreated(LocalDateTime.now());
        request.setEvent(eventFullDto.getId());
        request.setRequester(requester.getId());

        if (!eventFullDto.isRequestModeration() || eventFullDto.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        Request savedRequest = requestRepository.save(request);
        log.info("Request created: {}", savedRequest);

        collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
        log.debug("User action sent: user ({}) registered to event ({})", userId, eventId);

        return RequestMapper.toDto(savedRequest);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.debug("Cancel request: userId = {}, requestId = {}", userId, requestId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id = %d not found", requestId)));

        if (!request.getRequester().equals(userId)) {
            throw new AccessViolationException("User is not request owner");
        }

        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        log.info("Request cancelled: {}", request);

        return RequestMapper.toDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        log.debug("Get participants: userId = {}, eventId = {}", userId, eventId);

        EventFullDto eventFullDto = eventClient.getEventInner(eventId);
        log.debug("Event: {}", eventFullDto);

        if (!eventFullDto.getInitiator().getId().equals(userId)) {
            throw new AccessViolationException("User is not event initiator");
        }

        return requestRepository.findByEvent(eventId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.debug("Change request status: userId = {}, eventId = {}, update = {}", userId, eventId, updateRequest);

        EventFullDto eventFullDto = eventClient.getEventInner(eventId);
        log.debug("Event: {}", eventFullDto);

        if (!eventFullDto.getInitiator().getId().equals(userId)) {
            throw new AccessViolationException("User is not event initiator");
        }

        List<Request> requests = requestRepository.findByIdInAndEvent(updateRequest.getRequestIds(), eventId);

        if (requests.isEmpty()) {
            throw new NotFoundException("Requests not found");
        }

        for (Request request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        Long confirmedCount = requestRepository.countConfirmedRequestsByEvent(eventId);
        int availableSlots = eventFullDto.getParticipantLimit() - confirmedCount.intValue();

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        RequestStatus newStatus = RequestStatus.from(updateRequest.getStatus().toString())
                .orElseThrow(() -> new ValidationException("Invalid status"));

        if (newStatus.equals(RequestStatus.CONFIRMED)) {
            if (eventFullDto.getParticipantLimit() == 0 || !eventFullDto.isRequestModeration()) {
                for (Request request : requests) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                }
            } else {
                for (Request request : requests) {
                    if (availableSlots > 0) {
                        request.setStatus(RequestStatus.CONFIRMED);
                        confirmed.add(RequestMapper.toDto(request));
                        availableSlots--;
                    } else {
                        request.setStatus(RequestStatus.REJECTED);
                        rejected.add(RequestMapper.toDto(request));
                    }
                }

                if (availableSlots == 0) {
                    List<Request> pendingRequests = requestRepository.findByEventAndStatus(eventId, RequestStatus.PENDING);
                    for (Request pending : pendingRequests) {
                        if (!updateRequest.getRequestIds().contains(pending.getId())) {
                            pending.setStatus(RequestStatus.REJECTED);
                            requestRepository.save(pending);
                        }
                    }
                }
            }
        } else if (newStatus.equals(RequestStatus.REJECTED)) {
            for (Request request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toDto(request));
            }
        }

        requestRepository.saveAll(requests);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countConfirmedRequestsForEvents(List<Long> eventIds) {
        log.debug("Count confirmed requests for events in: {}", eventIds);

        Map<Long, Long> confirmedRequests = requestRepository.countConfirmedRequestsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
        log.debug("Confirmed requests map: {}", confirmedRequests);

        return confirmedRequests;
    }
}