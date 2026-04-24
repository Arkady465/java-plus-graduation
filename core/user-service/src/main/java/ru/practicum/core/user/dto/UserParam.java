package ru.practicum.core.user.dto;

import java.util.List;

public record UserParam(List<Long> ids, Integer from, Integer size) {
    public boolean hasIds() {
        return ids != null && !ids.isEmpty();
    }
}