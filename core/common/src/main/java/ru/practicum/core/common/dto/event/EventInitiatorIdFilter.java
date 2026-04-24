package ru.practicum.core.common.dto.event;

import lombok.Data;

@Data
public class EventInitiatorIdFilter {
    private Long userId;

    public Long getInitiator() {
        return userId;
    }
}