package ru.practicum.main.ewm.service.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        long confirmed = participationRequestRepository.countByEventIdAndStatus(e.getId(), RequestStatus.CONFIRMED);
        long views = getViews(e.getId());
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

    @Transactional(readOnly = true)
    public EventShortDto toShortDto(EventEntity e) {
        long confirmed = participationRequestRepository.countByEventIdAndStatus(e.getId(), RequestStatus.CONFIRMED);
        long views = getViews(e.getId());
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

    private long getViews(long eventId) {
        String uri = "/events/" + eventId;
        try {
            String start = LocalDateTime.of(2000, 1, 1, 0, 0).format(TS);
            String end = LocalDateTime.of(3000, 1, 1, 0, 0).format(TS);
            List<StatsClient.ViewStats> stats = statsClient.getStats(start, end, List.of(uri), true);
            if (stats == null || stats.isEmpty()) {
                return 0;
            }
            return stats.stream()
                    .filter(s -> uri.equals(s.getUri()))
                    .mapToLong(StatsClient.ViewStats::getHits)
                    .findFirst()
                    .orElse(0);
        } catch (Exception ex) {
            return 0;
        }
    }
}
