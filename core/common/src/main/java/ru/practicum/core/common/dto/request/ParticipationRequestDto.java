package ru.practicum.core.common.dto.request;

import lombok.Data;

@Data
public class ParticipationRequestDto {
    private String created;

    private Long event;

    private Long id;

    private Long requester;

    private String status;
}