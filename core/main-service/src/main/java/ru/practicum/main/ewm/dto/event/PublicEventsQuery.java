package ru.practicum.main.ewm.dto.event;

import java.util.List;

import ru.practicum.main.ewm.model.Enums.Sort;

public record PublicEventsQuery(
        String text,
        List<Long> categories,
        Boolean paid,
        String rangeStart,
        String rangeEnd,
        boolean onlyAvailable,
        Sort sort,
        int from,
        int size
) {
}
