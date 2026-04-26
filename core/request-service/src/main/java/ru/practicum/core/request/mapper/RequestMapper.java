package ru.practicum.core.request.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import ru.practicum.core.common.dto.request.ParticipationRequestDto;
import ru.practicum.core.request.model.Request;

import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static ParticipationRequestDto toDto(Request request) {
        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(request.getId());
        dto.setCreated(request.getCreated().format(FORMATTER));
        dto.setEvent(request.getEvent());
        dto.setRequester(request.getRequester());
        dto.setStatus(request.getStatus().name());
        return dto;
    }
}