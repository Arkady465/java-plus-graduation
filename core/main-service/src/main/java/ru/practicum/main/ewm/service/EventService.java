package ru.practicum.main.ewm.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.main.ewm.api.ConflictException;
import ru.practicum.main.ewm.api.NotFoundException;
import ru.practicum.main.ewm.domain.CategoryEntity;
import ru.practicum.main.ewm.domain.EventEntity;
import ru.practicum.main.ewm.domain.UserEntity;
import ru.practicum.main.ewm.dto.event.EventLocationDto;
import ru.practicum.main.ewm.dto.event.EventFullDto;
import ru.practicum.main.ewm.dto.event.EventShortDto;
import ru.practicum.main.ewm.dto.event.NewEventDto;
import ru.practicum.main.ewm.dto.event.PublicEventsQuery;
import ru.practicum.main.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.main.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.main.ewm.model.Enums;
import ru.practicum.main.ewm.model.Enums.EventState;
import ru.practicum.main.ewm.model.Enums.RequestStatus;
import ru.practicum.main.ewm.repository.EventRepository;
import ru.practicum.main.ewm.repository.EventSpecifications;
import ru.practicum.main.ewm.repository.ParticipationRequestRepository;
import ru.practicum.main.ewm.service.mapper.EventDtoMapper;
import ru.practicum.main.stats.StatsClient;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final EventDtoMapper eventDtoMapper;
    private final StatsClient statsClient;

    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(PublicEventsQuery q) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = q.rangeStart() == null ? now : parse(q.rangeStart());
        LocalDateTime end = q.rangeEnd() == null ? LocalDateTime.of(3000, 1, 1, 0, 0) : parse(q.rangeEnd());

        Specification<EventEntity> spec = Specification.where(EventSpecifications.publishedInWindow(start, end))
                .and(EventSpecifications.paidEquals(q.paid()))
                .and(EventSpecifications.categoryIn(q.categories()))
                .and(EventSpecifications.textMatches(q.text()));

        List<EventEntity> filtered = new ArrayList<>(eventRepository.findAll(spec));
        if (q.onlyAvailable()) {
            filtered = filtered.stream()
                    .filter(e -> e.getParticipantLimit() == 0
                            || participationRequestRepository.countByEventIdAndStatus(e.getId(), RequestStatus.CONFIRMED)
                            < e.getParticipantLimit())
                    .toList();
        }

        List<EventShortDto> out = filtered.stream().map(eventDtoMapper::toShortDto).toList();
        if (q.sort() == Enums.Sort.EVENT_DATE) {
            out = out.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList();
        } else if (q.sort() == Enums.Sort.VIEWS) {
            out = out.stream().sorted(Comparator.comparing(EventShortDto::getViews).reversed()).toList();
        }

        recordHit("/events");
        return out.stream().skip(q.from()).limit(q.size()).toList();
    }

    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(long id) {
        EventEntity e = requireEvent(id);
        if (e.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + id + " was not found");
        }
        recordHit("/events/" + id);
        return eventDtoMapper.toFullDto(e);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(long userId, int from, int size) {
        userService.requireUser(userId);
        return eventRepository.findByInitiatorIdOrderByIdAsc(userId).stream()
                .skip(from)
                .limit(size)
                .map(eventDtoMapper::toShortDto)
                .toList();
    }

    public EventFullDto addUserEvent(long userId, NewEventDto dto) {
        UserEntity initiator = userService.requireUser(userId);
        CategoryEntity category = categoryService.requireCategory(dto.getCategory());
        LocalDateTime eventDate = dto.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("For the requested operation the conditions are not met.",
                    "Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: "
                            + TS.format(eventDate));
        }
        EventEntity e = new EventEntity();
        e.setInitiator(initiator);
        e.setCategory(category);
        e.setTitle(dto.getTitle().trim());
        e.setAnnotation(dto.getAnnotation().trim());
        e.setDescription(dto.getDescription().trim());
        e.setPaid(Boolean.TRUE.equals(dto.getPaid()));
        e.setParticipantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit());
        e.setRequestModeration(dto.getRequestModeration() == null || dto.getRequestModeration());
        e.setState(EventState.PENDING);
        e.setCreatedOn(LocalDateTime.now());
        e.setEventDate(eventDate);
        e.setLat(dto.getLocation().getLat());
        e.setLon(dto.getLocation().getLon());
        eventRepository.save(e);
        return eventDtoMapper.toFullDto(e);
    }

    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(long userId, long eventId) {
        EventEntity e = requireEvent(eventId);
        if (!e.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return eventDtoMapper.toFullDto(e);
    }

    public EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest req) {
        EventEntity e = requireEvent(eventId);
        if (!e.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        if (!(e.getState() == EventState.PENDING || e.getState() == EventState.CANCELED)) {
            throw new ConflictException("For the requested operation the conditions are not met.",
                    "Only pending or canceled events can be changed");
        }
        applyEventUpdate(e, req.getTitle(), req.getAnnotation(), req.getDescription(), req.getCategory(),
                req.getEventDate(), req.getLocation(), req.getPaid(), req.getParticipantLimit(), req.getRequestModeration());

        if (req.getStateAction() == Enums.UserStateAction.SEND_TO_REVIEW) {
            e.setState(EventState.PENDING);
        } else if (req.getStateAction() == Enums.UserStateAction.CANCEL_REVIEW) {
            e.setState(EventState.CANCELED);
        }
        return eventDtoMapper.toFullDto(e);
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> adminSearchEvents(List<Long> users, List<String> states, List<Long> categories,
                                                String rangeStart, String rangeEnd, int from, int size) {
        LocalDateTime start = rangeStart == null ? LocalDateTime.MIN : parse(rangeStart);
        LocalDateTime end = rangeEnd == null ? LocalDateTime.of(3000, 1, 1, 0, 0) : parse(rangeEnd);
        List<EventState> stateEnums = null;
        if (states != null && !states.isEmpty()) {
            stateEnums = states.stream().map(EventState::valueOf).toList();
        }
        Specification<EventEntity> spec = EventSpecifications.adminFilter(users, stateEnums, categories, start, end);
        return eventRepository.findAll(spec).stream()
                .skip(from)
                .limit(size)
                .map(eventDtoMapper::toFullDto)
                .toList();
    }

    public EventFullDto adminUpdateEvent(long eventId, UpdateEventAdminRequest req) {
        EventEntity e = requireEvent(eventId);
        applyEventUpdate(e, req.getTitle(), req.getAnnotation(), req.getDescription(), req.getCategory(),
                req.getEventDate(), req.getLocation(), req.getPaid(), req.getParticipantLimit(), req.getRequestModeration());

        if (req.getStateAction() == Enums.AdminStateAction.PUBLISH_EVENT) {
            if (e.getState() != EventState.PENDING) {
                throw new ConflictException("For the requested operation the conditions are not met.",
                        "Cannot publish the event because it's not in the right state: " + e.getState());
            }
            if (e.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("For the requested operation the conditions are not met.",
                        "Event date must be at least 1 hour after publish");
            }
            e.setState(EventState.PUBLISHED);
            e.setPublishedOn(LocalDateTime.now());
        } else if (req.getStateAction() == Enums.AdminStateAction.REJECT_EVENT) {
            if (e.getState() == EventState.PUBLISHED) {
                throw new ConflictException("For the requested operation the conditions are not met.",
                        "Cannot reject the event because it's already published");
            }
            e.setState(EventState.CANCELED);
        }
        return eventDtoMapper.toFullDto(e);
    }

    @Transactional(readOnly = true)
    public EventEntity requireEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    @Transactional(readOnly = true)
    public EventShortDto toShortDto(EventEntity e) {
        return eventDtoMapper.toShortDto(e);
    }

    private void applyEventUpdate(EventEntity e, String title, String annotation, String description, Long category,
                                  LocalDateTime eventDate, EventLocationDto location, Boolean paid,
                                  Integer participantLimit, Boolean requestModeration) {
        if (title != null) {
            e.setTitle(title.trim());
        }
        if (annotation != null) {
            e.setAnnotation(annotation.trim());
        }
        if (description != null) {
            e.setDescription(description.trim());
        }
        if (category != null) {
            e.setCategory(categoryService.requireCategory(category));
        }
        if (eventDate != null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConflictException("For the requested operation the conditions are not met.",
                        "Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: "
                                + TS.format(eventDate));
            }
            e.setEventDate(eventDate);
        }
        if (location != null) {
            e.setLat(location.getLat());
            e.setLon(location.getLon());
        }
        if (paid != null) {
            e.setPaid(paid);
        }
        if (participantLimit != null) {
            e.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            e.setRequestModeration(requestModeration);
        }
    }

    private void recordHit(String uri) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            String ip = Objects.toString(attrs.getRequest().getHeader("X-Forwarded-For"), attrs.getRequest().getRemoteAddr());
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            statsClient.hit("ewm-main-service", uri, ip);
        } catch (Exception ignore) {
            // stats optional
        }
    }

    private static LocalDateTime parse(String dt) {
        return LocalDateTime.parse(dt, TS);
    }
}
