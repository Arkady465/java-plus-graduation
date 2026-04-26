package ru.practicum.core.event.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.core.common.api.client.UserClient;
import ru.practicum.core.common.dto.event.EventAdminFilter;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.event.EventInitiatorIdFilter;
import ru.practicum.core.common.dto.event.EventPublicFilter;
import ru.practicum.core.common.dto.event.EventShortDto;
import ru.practicum.core.common.dto.event.EventState;
import ru.practicum.core.common.dto.event.NewEventDto;
import ru.practicum.core.common.dto.event.UpdateEventRequest;
import ru.practicum.core.common.dto.request.ParticipationRequestDto;
import ru.practicum.core.common.dto.request.RequestStatus;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.common.exception.AccessViolationException;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.common.exception.ValidationException;
import ru.practicum.core.event.dto.StateTransitionValidator;
import ru.practicum.core.common.dto.page.*;
import ru.practicum.core.event.dto.event.EventSpecification;
import ru.practicum.core.event.mapper.EventMapper;
import ru.practicum.core.event.model.Category;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.model.Location;
import ru.practicum.core.event.repository.CategoryRepository;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.common.api.client.CommentClient;
import ru.practicum.core.common.api.client.RequestClient;
import ru.practicum.ewm.stats.proto.recommendation.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.user.ActionTypeProto;
import ru.practicum.stats.client.AnalyzerClient;
import ru.practicum.stats.client.CollectorClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
@Service
public class EventServiceImpl implements EventService {
    private static final int MAX_RECOMMENDATIONS = 5;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    private final EventMapper eventMapper;

    private final CommentClient commentClient;
    private final UserClient userClient;
    private final RequestClient requestClient;

    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;

    private Map<Long, Double> getRatings(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        try {
            log.debug("Calling analyzer client");
            Stream<RecommendedEventProto> result = analyzerClient.getInteractionsCount(events.stream()
                    .map(Event::getId)
                    .toList());
            return result.collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
        } catch (Exception exception) {
            log.error("Error retrieving view statistics: {}", exception.getMessage());
            return Map.of();
        }
    }

    private double getRating(Event event) {
        Map<Long, Double> map = getRatings(List.of(event));
        return map.getOrDefault(event.getId(), 0.0);
    }

    private long getRequestCount(Event event) {
        Map<Long, Long> map = getRequests(List.of(event));
        return map.getOrDefault(event.getId(), 0L);
    }

    private Map<Long, Long> getRequests(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        return requestClient.countConfirmedRequests(events.stream()
                .map(Event::getId)
                .toList());
    }

    private Map<Long, Boolean> checkAvailable(List<Event> events, Map<Long, Long> requestMap) {
        Map<Long, Boolean> availableMap = new HashMap<>();

        for (Event event : events) {
            if (event.getParticipantLimit() > 0) {
                if (requestMap.getOrDefault(event.getId(), 0L) < event.getParticipantLimit()) {
                    availableMap.put(event.getId(), true);
                } else {
                    availableMap.put(event.getId(), false);
                }
            } else {
                availableMap.put(event.getId(), true);
            }
        }

        return availableMap;
    }

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        log.debug("Create new event request by user with id = {}: {}", userId, newEventDto);

        UserDto initiator = userClient.getById(userId);

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("The event date must be at least 2 hours from now");
        }

        Event event = eventMapper.toEvent(newEventDto, initiator);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event = eventRepository.save(event);

        return eventMapper.toFullDto(event, getRequestCount(event),
                getRating(event), 0L, initiator);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EventShortDto> getEventsByUserId(EventInitiatorIdFilter eventInitiatorIdFilter,
                                                       Pageable pageable) {
        log.debug("Get users' events with short info");
        log.debug("EventInitiatorIdFilter: {}", eventInitiatorIdFilter);

        UserShortDto initiator = userClient.getShortById(eventInitiatorIdFilter.getUserId());

        Specification<Event> spec = EventSpecification.withInitiatorId(eventInitiatorIdFilter);
        Page<Event> events = eventRepository.findAll(spec, pageable);

        Map<Long, Long> requestsMap = getRequests(events.getContent());
        log.debug("Requests map: {}", requestsMap);

        Map<Long, Double> ratingMap = getRatings(events.getContent());
        log.debug("Rating map: {}", ratingMap);

        Map<Long, Long> commentsMap = commentClient.countByEvents(events.stream()
                .map(Event::getId).toList());
        log.debug("Comments map: {}", commentsMap);

        return events.getContent().stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        requestsMap.getOrDefault(event.getId(), 0L),
                        ratingMap.getOrDefault(event.getId(), 0.0),
                        commentsMap.getOrDefault(event.getId(), 0L),
                        initiator
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventFullDescription(Long userId, Long eventId) {
        log.debug("Get users' event with detailed info");
        log.debug("User = {}, event = {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event id = %s not found", eventId)));

        UserDto initiator = userClient.getById(userId);

        if (!Objects.equals(event.getInitiator(), userId)) {
            throw new AccessViolationException(String.format("User userId = %s is not the initiator of the event " +
                    "eventId = %s", userId, eventId));
        }

        log.debug("Found event: {}", event);

        return eventMapper.toFullDto(event, getRequestCount(event), getRating(event),
                commentClient.countByEvent(eventId), initiator);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByCreator(Long userId, Long eventId, UpdateEventRequest updateEventRequest) {
        log.debug("Update event = {} by initiator = {} request: {}", eventId, userId, updateEventRequest);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event id = %s not found", eventId)));
        log.debug("Initial state: {}", event);

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Events can only be edited when in Pending Moderation or Cancelled status");
        }
        if (!event.getEventDate().isAfter(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Editing events is allowed no later than 2 hours before they start.");
        }
        if (updateEventRequest.getEventDate() != null && updateEventRequest.getEventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Editing past events is prohibited");
        }

        UserDto initiator = userClient.getById(userId);

        if (!Objects.equals(event.getInitiator(), userId)) {
            throw new AccessViolationException(String.format("User userId = %s is not the initiator of the event " +
                    "eventId = %s", userId, eventId));
        }

        EventState newState = event.getState();
        if (updateEventRequest.getStateAction() != null) {
            newState =
                    StateTransitionValidator.changeState(event.getState(), updateEventRequest.getStateAction(), false);
        }

        Category category = null;
        if (updateEventRequest.hasCategory()) {
            category = categoryRepository.findById(updateEventRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }

        Location newLocation = null;
        if (updateEventRequest.hasLocationDto()) {
            newLocation = eventMapper.toLocation(updateEventRequest.getLocationDto());
        }

        eventMapper.applyTo(updateEventRequest, event, category, newLocation, newState);
        eventRepository.save(event);
        log.debug("New state: {}", event);

        return eventMapper.toFullDto(event, getRequestCount(event), getRating(event),
                commentClient.countByEvent(eventId), initiator);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest updateEventRequest) {
        log.debug("Update event = {} by admin request: {}", eventId, updateEventRequest);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event id = %s not found", eventId)));
        log.debug("Initial state: {}", event);

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Events can only be edited when in Pending Moderation or Cancelled state");
        }
        if (!event.getEventDate().isAfter(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Editing events is allowed no later than 1 hour before they start");
        }
        if (updateEventRequest.getEventDate() != null && updateEventRequest.getEventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Editing past events is prohibited");
        }

        EventState state = event.getState();
        if (updateEventRequest.getStateAction() != null) {
            state = StateTransitionValidator.changeState(event.getState(), updateEventRequest.getStateAction(), true);
        }

        if (event.getState() == EventState.PENDING &&  // <-- старое состояние
                state == EventState.PUBLISHED) {       // <-- новое состояние
            event.setPublishedOn(LocalDateTime.now());
        }

        Category category = null;
        if (updateEventRequest.hasCategory()) {
            category = categoryRepository.findById(updateEventRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }

        Location newLocation = null;
        if (updateEventRequest.hasLocationDto()) {
            newLocation = eventMapper.toLocation(updateEventRequest.getLocationDto());
        }

        eventMapper.applyTo(updateEventRequest, event, category, newLocation, state);
        eventRepository.save(event);
        log.debug("New state: {}", event);

        UserShortDto initiator = userClient.getShortById(event.getInitiator());

        return eventMapper.toFullDto(event, getRequestCount(event), getRating(event),
                commentClient.countByEvent(eventId), initiator);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> adminSearchEvents(EventAdminFilter eventAdminFilter, PageRequestDto pageRequestDto) {
        log.debug("Search events by admin with detailed info");
        log.debug("EventAdminFilter: {}", eventAdminFilter);
        log.debug("PageRequestDto: {}", pageRequestDto);

        Pageable pageable = pageRequestDto.toPageable();
        Specification<Event> spec = EventSpecification.withAdminFilter(eventAdminFilter);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        Map<Long, Long> requestsMap = getRequests(events);
        log.debug("Requests map: {}", requestsMap);

        Map<Long, Double> ratingMap = getRatings(events);
        log.debug("Rating map: {}", ratingMap);

        Map<Long, Long> commentsMap = commentClient.countByEvents(events.stream()
                .map(Event::getId)
                .toList());
        log.debug("Comments map: {}", commentsMap);

        Map<Long, UserShortDto> initiatorsMap = userClient.getShortByIdIn(events.stream()
                .map(Event::getInitiator)
                .toList()).stream()
                    .collect(Collectors.toMap(UserShortDto::getId, user -> user));
        Map<Long, UserShortDto> eventInitiatorsMap = events.stream()
                .collect(Collectors.toMap(Event::getId, event -> initiatorsMap.get(event.getInitiator())));

        return events.stream()
                .map(event -> eventMapper.toFullDto(
                        event,
                        requestsMap.getOrDefault(event.getId(), 0L),
                        ratingMap.getOrDefault(event.getId(), 0.0),
                        commentsMap.getOrDefault(event.getId(), 0L),
                        eventInitiatorsMap.get(event.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> publicSearchEvents(EventPublicFilter eventPublicFilter, PageRequestDto pageRequestDto) {
        log.debug("Search events by user with short info");
        log.debug("EventPublicFilter: {}", eventPublicFilter);
        log.debug("PageRequestDto: {}", pageRequestDto);

        Pageable pageable = pageRequestDto.toPageable();
        EventSort sort = pageRequestDto.getSort();
        boolean sortByDate = sort == EventSort.EVENT_DATE;
        boolean sortByViews = sort == EventSort.VIEWS;
        boolean noSort = sort == null;
        Specification<Event> spec = EventSpecification.withPublicFilter(eventPublicFilter);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> requestsMap = getRequests(events);
        log.debug("Requests map: {}", requestsMap);

        Map<Long, Double> ratingMap = getRatings(events);
        log.debug("Rating map: {}", ratingMap);

        Map<Long, Boolean> availableMap = checkAvailable(events, requestsMap);

        Map<Long, Long> commentsMap = commentClient.countByEvents(events.stream()
                .map(Event::getId).toList());
        log.debug("Comments map: {}", commentsMap);

        Map<Long, UserShortDto> initiatorsMap = userClient.getShortByIdIn(events.stream()
                .map(Event::getInitiator).toList()).stream().collect(Collectors.toMap(UserShortDto::getId, user -> user));
        Map<Long, UserShortDto> eventInitiatorsMap = events.stream()
                .collect(Collectors.toMap(Event::getId, event -> initiatorsMap.get(event.getInitiator())));

        if (eventPublicFilter.getOnlyAvailable() == true) {
            events = events.stream()
                    .filter(e -> availableMap.getOrDefault(e.getId(), false)).toList();
        }

        if (sortByViews) {
            events = events.stream()
                    .sorted(Comparator.comparingDouble(
                            e -> ratingMap.getOrDefault(e.getId(), 0.0)))
                    .toList().reversed();
        }

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        requestsMap.getOrDefault(event.getId(), 0L),
                        ratingMap.getOrDefault(event.getId(), 0.0),
                        commentsMap.getOrDefault(event.getId(), 0L),
                        eventInitiatorsMap.get(event.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublishedEvent(Long userId, Long eventId) {
        log.debug("Get published event ({}) detailed info by user ({})", eventId, userId);

        userClient.getShortById(userId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(String.format("Event id = %s not found", eventId)));

        UserShortDto initiator = userClient.getShortById(event.getInitiator());

        log.debug("Found event: {}", event);

        collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
        log.debug("User action sent: user ({}) viewed event ({})", userId, eventId);

        return eventMapper.toFullDto(event, getRequestCount(event), getRating(event),
                commentClient.countByEvent(eventId), initiator);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEvent(Long eventId) {
        log.debug("Get event detailed info: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event id = %s not found", eventId)));

        UserShortDto initiator = userClient.getShortById(event.getInitiator());

        log.debug("Found event: {}", event);

        return eventMapper.toFullDto(event, getRequestCount(event), getRating(event),
                commentClient.countByEvent(eventId), initiator);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getRecommendations(Long userId) {
        log.debug("Get recommendations for user: {}", userId);

        userClient.getShortById(userId);

        Stream<RecommendedEventProto> response = analyzerClient.getRecommendationsForUser(userId, MAX_RECOMMENDATIONS);
        List<Long> orderedIds = response
                .map(RecommendedEventProto::getEventId)
                .toList();

        List<Event> events = eventRepository.findAllById(orderedIds);

        if (events.isEmpty()) {
            log.debug("Analyzer response was empty, nothing to recommend");
            return List.of();
        }

        Map<Long, Event> eventsMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        Map<Long, Long> requestsMap = getRequests(events);
        log.debug("Requests map: {}", requestsMap);

        Map<Long, Double> ratingMap = getRatings(events);
        log.debug("Rating map: {}", ratingMap);

        Map<Long, Long> commentsMap = commentClient.countByEvents(events.stream()
                .map(Event::getId)
                .toList());
        log.debug("Comments map: {}", commentsMap);

        Map<Long, UserShortDto> initiatorsMap = userClient.getShortByIdIn(events.stream()
                        .map(Event::getInitiator)
                        .toList()).stream()
                .collect(Collectors.toMap(UserShortDto::getId, user -> user));
        Map<Long, UserShortDto> eventInitiatorsMap = events.stream()
                .collect(Collectors.toMap(Event::getId, event -> initiatorsMap.get(event.getInitiator())));

        return orderedIds.stream()
                .map(eventsMap::get)
                .filter(Objects::nonNull)
                .map(event -> eventMapper.toShortDto(
                        event,
                        requestsMap.getOrDefault(event.getId(), 0L),
                        ratingMap.getOrDefault(event.getId(), 0.0),
                        commentsMap.getOrDefault(event.getId(), 0L),
                        eventInitiatorsMap.get(event.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void putLike(Long userId, Long eventId) {
        log.debug("Like request for event ({}) from user ({})", eventId, userId);

        userClient.getShortById(userId);

        eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(String.format("Event id = %s not found", eventId)));

        List<ParticipationRequestDto> userParticipation = requestClient.getUserRequests(userId);

        if (userParticipation.stream()
                .anyMatch(item -> item.getEvent().equals(eventId)
                        && item.getStatus().equals(RequestStatus.CONFIRMED.name()))) {
            collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
            log.debug("User action sent: user ({}) like on event ({})", userId, eventId);
        } else {
            throw new ValidationException(String.format("User (%d) did not (yet) participate in the event (%d)", userId, eventId));
        }
    }
}