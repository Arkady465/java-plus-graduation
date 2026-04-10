package ru.practicum.main.ewm.service.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.ewm.domain.CategoryEntity;
import ru.practicum.main.ewm.domain.EventEntity;
import ru.practicum.main.ewm.domain.UserEntity;
import ru.practicum.main.ewm.dto.category.CategoryDto;
import ru.practicum.main.ewm.dto.event.EventFullDto;
import ru.practicum.main.ewm.dto.event.EventLocationDto;
import ru.practicum.main.ewm.dto.event.EventShortDto;
import ru.practicum.main.ewm.dto.user.UserShortDto;
import ru.practicum.main.ewm.model.Enums.RequestStatus;
import ru.practicum.main.ewm.repository.ParticipationRequestRepository;
import ru.practicum.main.stats.StatsClient;

@Component
@RequiredArgsConstructor
public class EventDtoMapper {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ParticipationRequestRepository participationRequestRepository;
    private final StatsClient statsClient;

    public CategoryDto toCategoryDto(CategoryEntity c) {
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        return dto;
    }

    public UserShortDto toUserShort(UserEntity u) {
        return new UserShortDto(u.getId(), u.getName());
    }

    public EventLocationDto toLocationDto(EventEntity e) {
        EventLocationDto dto = new EventLocationDto();
        dto.setLat(e.getLat());
        dto.setLon(e.getLon());
        return dto;
    }

    @Transactional(readOnly = true)
    public EventFullDto toFullDto(EventEntity e) {
        long id = e.getId();
        Map<Long, Long> confirmed = loadConfirmedCounts(List.of(id));
        Map<Long, Long> views = loadViewCounts(List.of(id));
        return toFullDto(e, views.getOrDefault(id, 0L), confirmed.getOrDefault(id, 0L));
    }

    @Transactional(readOnly = true)
    public EventShortDto toShortDto(EventEntity e) {
        long id = e.getId();
        Map<Long, Long> confirmed = loadConfirmedCounts(List.of(id));
        Map<Long, Long> views = loadViewCounts(List.of(id));
        return toShortDto(e, views.getOrDefault(id, 0L), confirmed.getOrDefault(id, 0L));
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> toShortDtoList(List<EventEntity> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> ids = events.stream().map(EventEntity::getId).toList();
        Map<Long, Long> confirmed = loadConfirmedCounts(ids);
        Map<Long, Long> views = loadViewCounts(ids);
        return events.stream()
                .map(e -> toShortDto(e, views.getOrDefault(e.getId(), 0L), confirmed.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> toFullDtoList(List<EventEntity> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> ids = events.stream().map(EventEntity::getId).toList();
        Map<Long, Long> confirmed = loadConfirmedCounts(ids);
        Map<Long, Long> views = loadViewCounts(ids);
        return events.stream()
                .map(e -> toFullDto(e, views.getOrDefault(e.getId(), 0L), confirmed.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    public Map<Long, Long> loadConfirmedCounts(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = participationRequestRepository.countByEventIdInAndStatusGrouped(
                eventIds, RequestStatus.CONFIRMED);
        Map<Long, Long> map = HashMap.newHashMap(rows.size());
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    public Map<Long, Long> loadViewCounts(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();
        String start = LocalDateTime.of(2000, 1, 1, 0, 0).format(TS);
        String end = LocalDateTime.of(3000, 1, 1, 0, 0).format(TS);
        List<StatsClient.ViewStats> stats = statsClient.getStats(start, end, uris, true);
        if (stats == null || stats.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = HashMap.newHashMap(stats.size());
        for (StatsClient.ViewStats s : stats) {
            String uri = s.getUri();
            if (uri == null || !uri.startsWith("/events/")) {
                continue;
            }
            try {
                long id = Long.parseLong(uri.substring("/events/".length()));
                map.put(id, s.getHits());
            } catch (NumberFormatException ignore) {
                // skip malformed uri
            }
        }
        return map;
    }

    public EventFullDto toFullDto(EventEntity e, long views, long confirmed) {
        return EventFullDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .category(toCategoryDto(e.getCategory()))
                .paid(e.isPaid())
                .eventDate(e.getEventDate())
                .initiator(toUserShort(e.getInitiator()))
                .description(e.getDescription())
                .participantLimit(e.getParticipantLimit())
                .state(e.getState())
                .createdOn(e.getCreatedOn())
                .publishedOn(e.getPublishedOn())
                .location(toLocationDto(e))
                .requestModeration(e.isRequestModeration())
                .views(views)
                .confirmedRequests(confirmed)
                .build();
    }

    public EventShortDto toShortDto(EventEntity e, long views, long confirmed) {
        return EventShortDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .category(toCategoryDto(e.getCategory()))
                .paid(e.isPaid())
                .eventDate(e.getEventDate())
                .initiator(toUserShort(e.getInitiator()))
                .views(views)
                .confirmedRequests(confirmed)
                .build();
    }
}
