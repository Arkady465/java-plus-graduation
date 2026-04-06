package ru.practicum.main.ewm.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.main.ewm.api.ConflictException;
import ru.practicum.main.ewm.api.NotFoundException;
import ru.practicum.main.ewm.dto.Dtos;
import ru.practicum.main.ewm.dto.Dtos.CategoryDto;
import ru.practicum.main.ewm.dto.Dtos.CompilationDto;
import ru.practicum.main.ewm.dto.Dtos.EventFullDto;
import ru.practicum.main.ewm.dto.Dtos.EventRequestStatusUpdateRequest;
import ru.practicum.main.ewm.dto.Dtos.EventRequestStatusUpdateResult;
import ru.practicum.main.ewm.dto.Dtos.EventShortDto;
import ru.practicum.main.ewm.dto.Dtos.NewCategoryDto;
import ru.practicum.main.ewm.dto.Dtos.NewCompilationDto;
import ru.practicum.main.ewm.dto.Dtos.NewEventDto;
import ru.practicum.main.ewm.dto.Dtos.NewUserRequest;
import ru.practicum.main.ewm.dto.Dtos.ParticipationRequestDto;
import ru.practicum.main.ewm.dto.Dtos.PublicEventsQuery;
import ru.practicum.main.ewm.dto.Dtos.UpdateCompilationRequest;
import ru.practicum.main.ewm.dto.Dtos.UpdateEventAdminRequest;
import ru.practicum.main.ewm.dto.Dtos.UpdateEventUserRequest;
import ru.practicum.main.ewm.dto.Dtos.UserDto;
import ru.practicum.main.ewm.dto.Dtos.UserShortDto;
import ru.practicum.main.ewm.model.Entities.Category;
import ru.practicum.main.ewm.model.Entities.Compilation;
import ru.practicum.main.ewm.model.Entities.Event;
import ru.practicum.main.ewm.model.Entities.Location;
import ru.practicum.main.ewm.model.Entities.ParticipationRequest;
import ru.practicum.main.ewm.model.Entities.User;
import ru.practicum.main.ewm.model.Enums;
import ru.practicum.main.ewm.model.Enums.EventState;
import ru.practicum.main.ewm.model.Enums.RequestStatus;
import ru.practicum.main.ewm.model.Store;
import ru.practicum.main.stats.StatsClient;

@Service
@RequiredArgsConstructor
public class EwmService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Store store;
    private final StatsClient statsClient;

    // region Users
    public UserDto addUser(NewUserRequest req) {
        if (store.users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(req.getEmail()))) {
            throw new ConflictException("Integrity constraint has been violated.", "Email must be unique");
        }
        User u = new User();
        u.setId(store.userSeq.incrementAndGet());
        u.setName(req.getName().trim());
        u.setEmail(req.getEmail().trim());
        store.users.put(u.getId(), u);
        return new UserDto(u.getId(), u.getName(), u.getEmail());
    }

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        List<User> list = new ArrayList<>(store.users.values());
        if (ids != null && !ids.isEmpty()) {
            list = list.stream().filter(u -> ids.contains(u.getId())).toList();
        }
        return list.stream()
                .skip(from)
                .limit(size)
                .map(u -> new UserDto(u.getId(), u.getName(), u.getEmail()))
                .toList();
    }

    public void deleteUser(long userId) {
        if (store.users.remove(userId) == null) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private User requireUser(long userId) {
        User u = store.users.get(userId);
        if (u == null) throw new NotFoundException("User with id=" + userId + " was not found");
        return u;
    }
    // endregion

    // region Categories
    public CategoryDto addCategory(NewCategoryDto req) {
        String name = req.getName().trim();
        if (store.categories.values().stream().anyMatch(c -> c.getName().equalsIgnoreCase(name))) {
            throw new ConflictException("Integrity constraint has been violated.", "Category name must be unique");
        }
        Category c = new Category();
        c.setId(store.catSeq.incrementAndGet());
        c.setName(name);
        store.categories.put(c.getId(), c);
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        return dto;
    }

    public CategoryDto updateCategory(long catId, CategoryDto dto) {
        Category c = requireCategory(catId);
        String name = dto.getName().trim();
        if (store.categories.values().stream().anyMatch(x -> x.getId() != catId && x.getName().equalsIgnoreCase(name))) {
            throw new ConflictException("Integrity constraint has been violated.", "Category name must be unique");
        }
        c.setName(name);
        CategoryDto out = new CategoryDto();
        out.setId(c.getId());
        out.setName(c.getName());
        return out;
    }

    public void deleteCategory(long catId) {
        requireCategory(catId);
        boolean used = store.events.values().stream().anyMatch(e -> e.getCategoryId() == catId);
        if (used) {
            throw new ConflictException("For the requested operation the conditions are not met.", "The category is not empty");
        }
        store.categories.remove(catId);
    }

    public List<CategoryDto> getCategories(int from, int size) {
        return store.categories.values().stream()
                .skip(from)
                .limit(size)
                .map(c -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(c.getId());
                    dto.setName(c.getName());
                    return dto;
                })
                .toList();
    }

    public CategoryDto getCategory(long catId) {
        Category c = requireCategory(catId);
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        return dto;
    }

    private Category requireCategory(long catId) {
        Category c = store.categories.get(catId);
        if (c == null) throw new NotFoundException("Category with id=" + catId + " was not found");
        return c;
    }
    // endregion

    // region Compilations
    public CompilationDto addCompilation(NewCompilationDto req) {
        Compilation c = new Compilation();
        c.setId(store.compSeq.incrementAndGet());
        c.setTitle(req.getTitle().trim());
        c.setPinned(Boolean.TRUE.equals(req.getPinned()));
        if (req.getEvents() != null) {
            for (Long id : req.getEvents()) requireEvent(id);
            c.getEventIds().addAll(req.getEvents());
        }
        store.compilations.put(c.getId(), c);
        return toCompilationDto(c);
    }

    public void deleteCompilation(long compId) {
        if (store.compilations.remove(compId) == null) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
    }

    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest req) {
        Compilation c = store.compilations.get(compId);
        if (c == null) throw new NotFoundException("Compilation with id=" + compId + " was not found");
        if (req.getTitle() != null) c.setTitle(req.getTitle().trim());
        if (req.getPinned() != null) c.setPinned(req.getPinned());
        if (req.getEvents() != null) {
            for (Long id : req.getEvents()) requireEvent(id);
            c.getEventIds().clear();
            c.getEventIds().addAll(req.getEvents());
        }
        return toCompilationDto(c);
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        return store.compilations.values().stream()
                .filter(c -> pinned == null || c.isPinned() == pinned)
                .skip(from)
                .limit(size)
                .map(this::toCompilationDto)
                .toList();
    }

    public CompilationDto getCompilation(long compId) {
        Compilation c = store.compilations.get(compId);
        if (c == null) throw new NotFoundException("Compilation with id=" + compId + " was not found");
        return toCompilationDto(c);
    }

    private CompilationDto toCompilationDto(Compilation c) {
        Set<EventShortDto> events = c.getEventIds().stream()
                .map(this::requireEvent)
                .map(this::toEventShortDto)
                .collect(Collectors.toSet());
        return CompilationDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .pinned(c.isPinned())
                .events(events)
                .build();
    }
    // endregion

    // region Events
    public List<EventShortDto> getPublicEvents(PublicEventsQuery q) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = q.rangeStart() == null ? now : parse(q.rangeStart());
        LocalDateTime end = q.rangeEnd() == null ? LocalDateTime.of(3000, 1, 1, 0, 0) : parse(q.rangeEnd());

        List<Event> filtered = store.events.values().stream()
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .filter(e -> !e.getEventDate().isBefore(start) && !e.getEventDate().isAfter(end))
                .filter(e -> q.paid() == null || e.isPaid() == q.paid())
                .filter(e -> q.categories() == null || q.categories().isEmpty() || q.categories().contains(e.getCategoryId()))
                .filter(e -> q.text() == null || q.text().isBlank() || matchesText(e, q.text()))
                .toList();

        if (q.onlyAvailable()) {
            filtered = filtered.stream()
                    .filter(e -> e.getParticipantLimit() == 0 || countConfirmedRequests(e.getId()) < e.getParticipantLimit())
                    .toList();
        }

        List<EventShortDto> out = filtered.stream().map(this::toEventShortDto).toList();
        if (q.sort() == Enums.Sort.EVENT_DATE) {
            out = out.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList();
        } else if (q.sort() == Enums.Sort.VIEWS) {
            out = out.stream().sorted(Comparator.comparing(EventShortDto::getViews).reversed()).toList();
        }

        recordHit("/events");
        return out.stream().skip(q.from()).limit(q.size()).toList();
    }

    public EventFullDto getPublicEvent(long id) {
        Event e = requireEvent(id);
        if (e.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + id + " was not found");
        }
        recordHit("/events/" + id);
        return toEventFullDto(e);
    }

    public List<EventShortDto> getUserEvents(long userId, int from, int size) {
        requireUser(userId);
        return store.events.values().stream()
                .filter(e -> e.getInitiatorId() == userId)
                .skip(from)
                .limit(size)
                .map(this::toEventShortDto)
                .toList();
    }

    public EventFullDto addUserEvent(long userId, NewEventDto dto) {
        requireUser(userId);
        requireCategory(dto.getCategory());
        LocalDateTime eventDate = parse(dto.getEventDate());
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("For the requested operation the conditions are not met.",
                    "Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + dto.getEventDate());
        }
        Event e = new Event();
        e.setId(store.eventSeq.incrementAndGet());
        e.setInitiatorId(userId);
        e.setTitle(dto.getTitle().trim());
        e.setAnnotation(dto.getAnnotation().trim());
        e.setDescription(dto.getDescription().trim());
        e.setCategoryId(dto.getCategory());
        e.setPaid(Boolean.TRUE.equals(dto.getPaid()));
        e.setParticipantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit());
        e.setRequestModeration(dto.getRequestModeration() == null || dto.getRequestModeration());
        e.setState(EventState.PENDING);
        e.setCreatedOn(LocalDateTime.now());
        e.setEventDate(eventDate);
        Location loc = new Location();
        loc.setLat(dto.getLocation().getLat());
        loc.setLon(dto.getLocation().getLon());
        e.setLocation(loc);
        store.events.put(e.getId(), e);
        return toEventFullDto(e);
    }

    public EventFullDto getUserEvent(long userId, long eventId) {
        Event e = requireEvent(eventId);
        if (e.getInitiatorId() != userId) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return toEventFullDto(e);
    }

    public EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest req) {
        Event e = requireEvent(eventId);
        if (e.getInitiatorId() != userId) throw new NotFoundException("Event with id=" + eventId + " was not found");
        if (!(e.getState() == EventState.PENDING || e.getState() == EventState.CANCELED)) {
            throw new ConflictException("For the requested operation the conditions are not met.", "Only pending or canceled events can be changed");
        }
        applyEventUpdate(e, req.getTitle(), req.getAnnotation(), req.getDescription(), req.getCategory(),
                req.getEventDate(), req.getLocation(), req.getPaid(), req.getParticipantLimit(), req.getRequestModeration());

        if (req.getStateAction() == Enums.UserStateAction.SEND_TO_REVIEW) {
            e.setState(EventState.PENDING);
        } else if (req.getStateAction() == Enums.UserStateAction.CANCEL_REVIEW) {
            e.setState(EventState.CANCELED);
        }
        return toEventFullDto(e);
    }

    public List<EventFullDto> adminSearchEvents(List<Long> users, List<String> states, List<Long> categories,
                                                String rangeStart, String rangeEnd, int from, int size) {
        LocalDateTime start = rangeStart == null ? LocalDateTime.MIN : parse(rangeStart);
        LocalDateTime end = rangeEnd == null ? LocalDateTime.of(3000, 1, 1, 0, 0) : parse(rangeEnd);
        List<Event> filtered = store.events.values().stream()
                .filter(e -> users == null || users.isEmpty() || users.contains(e.getInitiatorId()))
                .filter(e -> categories == null || categories.isEmpty() || categories.contains(e.getCategoryId()))
                .filter(e -> !e.getEventDate().isBefore(start) && !e.getEventDate().isAfter(end))
                .filter(e -> states == null || states.isEmpty() || states.contains(e.getState().name()))
                .toList();
        return filtered.stream().skip(from).limit(size).map(this::toEventFullDto).toList();
    }

    public EventFullDto adminUpdateEvent(long eventId, UpdateEventAdminRequest req) {
        Event e = requireEvent(eventId);
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
        return toEventFullDto(e);
    }

    private void applyEventUpdate(Event e, String title, String annotation, String description, Long category,
                                  String eventDate, Dtos.Location location, Boolean paid, Integer participantLimit, Boolean requestModeration) {
        if (title != null) e.setTitle(title.trim());
        if (annotation != null) e.setAnnotation(annotation.trim());
        if (description != null) e.setDescription(description.trim());
        if (category != null) {
            requireCategory(category);
            e.setCategoryId(category);
        }
        if (eventDate != null) {
            LocalDateTime dt = parse(eventDate);
            if (dt.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConflictException("For the requested operation the conditions are not met.",
                        "Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + eventDate);
            }
            e.setEventDate(dt);
        }
        if (location != null) {
            Location loc = new Location();
            loc.setLat(location.getLat());
            loc.setLon(location.getLon());
            e.setLocation(loc);
        }
        if (paid != null) e.setPaid(paid);
        if (participantLimit != null) e.setParticipantLimit(participantLimit);
        if (requestModeration != null) e.setRequestModeration(requestModeration);
    }

    private boolean matchesText(Event e, String text) {
        String t = text.toLowerCase();
        return e.getAnnotation().toLowerCase().contains(t) || e.getDescription().toLowerCase().contains(t);
    }

    private Event requireEvent(long eventId) {
        Event e = store.events.get(eventId);
        if (e == null) throw new NotFoundException("Event with id=" + eventId + " was not found");
        return e;
    }
    // endregion

    // region Requests
    public List<ParticipationRequestDto> getUserRequests(long userId) {
        requireUser(userId);
        return store.requests.values().stream()
                .filter(r -> r.getRequesterId() == userId)
                .map(this::toRequestDto)
                .toList();
    }

    public ParticipationRequestDto addParticipationRequest(long userId, long eventId) {
        requireUser(userId);
        Event e = requireEvent(eventId);

        if (e.getInitiatorId() == userId) {
            throw new ConflictException("For the requested operation the conditions are not met.", "Initiator cannot participate in own event");
        }
        if (e.getState() != EventState.PUBLISHED) {
            throw new ConflictException("For the requested operation the conditions are not met.", "Event must be published");
        }
        boolean duplicate = store.requests.values().stream().anyMatch(r -> r.getRequesterId() == userId && r.getEventId() == eventId
                && r.getStatus() != RequestStatus.CANCELED);
        if (duplicate) {
            throw new ConflictException("Integrity constraint has been violated.", "Duplicate request");
        }

        long confirmed = countConfirmedRequests(eventId);
        if (e.getParticipantLimit() > 0 && confirmed >= e.getParticipantLimit()) {
            throw new ConflictException("For the requested operation the conditions are not met.", "The participant limit has been reached");
        }

        ParticipationRequest r = new ParticipationRequest();
        r.setId(store.reqSeq.incrementAndGet());
        r.setRequesterId(userId);
        r.setEventId(eventId);
        r.setCreated(LocalDateTime.now());

        boolean autoConfirm = !e.isRequestModeration() || e.getParticipantLimit() == 0;
        r.setStatus(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING);
        store.requests.put(r.getId(), r);
        return toRequestDto(r);
    }

    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        requireUser(userId);
        ParticipationRequest r = store.requests.get(requestId);
        if (r == null) throw new NotFoundException("Request with id=" + requestId + " was not found");
        if (r.getRequesterId() != userId) throw new NotFoundException("Request with id=" + requestId + " was not found");
        r.setStatus(RequestStatus.CANCELED);
        return toRequestDto(r);
    }

    public List<ParticipationRequestDto> getEventParticipants(long userId, long eventId) {
        Event e = requireEvent(eventId);
        if (e.getInitiatorId() != userId) throw new NotFoundException("Event with id=" + eventId + " was not found");
        return store.requests.values().stream()
                .filter(r -> r.getEventId() == eventId)
                .map(this::toRequestDto)
                .toList();
    }

    public EventRequestStatusUpdateResult changeRequestStatus(long userId, long eventId, EventRequestStatusUpdateRequest req) {
        Event e = requireEvent(eventId);
        if (e.getInitiatorId() != userId) throw new NotFoundException("Event with id=" + eventId + " was not found");

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        for (Long id : req.getRequestIds()) {
            ParticipationRequest r = store.requests.get(id);
            if (r == null || r.getEventId() != eventId) throw new NotFoundException("Request with id=" + id + " was not found");
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("For the requested operation the conditions are not met.", "Request must have status PENDING");
            }
            if (req.getStatus() == Dtos.RequestStatusAction.CONFIRMED) {
                long conf = countConfirmedRequests(eventId);
                if (e.getParticipantLimit() > 0 && conf >= e.getParticipantLimit()) {
                    throw new ConflictException("For the requested operation the conditions are not met.", "The participant limit has been reached");
                }
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(toRequestDto(r));
            } else {
                r.setStatus(RequestStatus.REJECTED);
                rejected.add(toRequestDto(r));
            }
        }

        if (e.getParticipantLimit() > 0 && countConfirmedRequests(eventId) >= e.getParticipantLimit()) {
            store.requests.values().stream()
                    .filter(r -> r.getEventId() == eventId && r.getStatus() == RequestStatus.PENDING)
                    .forEach(r -> r.setStatus(RequestStatus.REJECTED));
        }
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private long countConfirmedRequests(long eventId) {
        return store.requests.values().stream()
                .filter(r -> r.getEventId() == eventId)
                .filter(r -> r.getStatus() == RequestStatus.CONFIRMED)
                .count();
    }

    private ParticipationRequestDto toRequestDto(ParticipationRequest r) {
        return new ParticipationRequestDto(
                r.getId(),
                r.getRequesterId(),
                r.getEventId(),
                r.getStatus().name(),
                r.getCreated().toString()
        );
    }
    // endregion

    // region Mapping helpers
    private EventFullDto toEventFullDto(Event e) {
        Category cat = requireCategory(e.getCategoryId());
        User initiator = requireUser(e.getInitiatorId());
        long confirmed = countConfirmedRequests(e.getId());
        long views = getViews("/events/" + e.getId());
        return EventFullDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .category(toCategoryDto(cat))
                .paid(e.isPaid())
                .eventDate(e.getEventDate().format(TS))
                .initiator(new UserShortDto(initiator.getId(), initiator.getName()))
                .description(e.getDescription())
                .participantLimit(e.getParticipantLimit())
                .state(e.getState())
                .createdOn(e.getCreatedOn().format(TS))
                .publishedOn(e.getPublishedOn() == null ? null : e.getPublishedOn().format(TS))
                .location(toLocationDto(e.getLocation()))
                .requestModeration(e.isRequestModeration())
                .views(views)
                .confirmedRequests(confirmed)
                .build();
    }

    private EventShortDto toEventShortDto(Event e) {
        Category cat = requireCategory(e.getCategoryId());
        User initiator = requireUser(e.getInitiatorId());
        long confirmed = countConfirmedRequests(e.getId());
        long views = getViews("/events/" + e.getId());
        return EventShortDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .category(toCategoryDto(cat))
                .paid(e.isPaid())
                .eventDate(e.getEventDate().format(TS))
                .initiator(new UserShortDto(initiator.getId(), initiator.getName()))
                .views(views)
                .confirmedRequests(confirmed)
                .build();
    }

    private static CategoryDto toCategoryDto(Category c) {
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        return dto;
    }

    private static Dtos.Location toLocationDto(Location loc) {
        Dtos.Location dto = new Dtos.Location();
        dto.setLat(loc.getLat());
        dto.setLon(loc.getLon());
        return dto;
    }
    // endregion

    // region Views / stats
    private void recordHit(String uri) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            String ip = Objects.toString(attrs.getRequest().getHeader("X-Forwarded-For"), attrs.getRequest().getRemoteAddr());
            if (ip.contains(",")) ip = ip.split(",")[0].trim();
            statsClient.hit("ewm-main-service", uri, ip);
        } catch (Exception ignored) {
        }
    }

    private long getViews(String uri) {
        try {
            String start = LocalDateTime.of(2000, 1, 1, 0, 0).format(TS);
            String end = LocalDateTime.of(3000, 1, 1, 0, 0).format(TS);
            List<StatsClient.ViewStats> stats = statsClient.getStats(start, end, List.of(uri), true);
            return stats == null ? 0 : stats.stream()
                    .filter(s -> s.getUri().equals(uri))
                    .mapToLong(StatsClient.ViewStats::getHits)
                    .findFirst().orElse(0);
        } catch (Exception ex) {
            return 0;
        }
    }
    // endregion

    private static LocalDateTime parse(String dt) {
        return LocalDateTime.parse(dt, TS);
    }
}

