package ru.practicum.core.event.model;

import java.util.Optional;

public enum EventParticipationStatus {
    CONFIRMED,
    REJECTED;

    public static Optional<EventParticipationStatus> from(String stringStatus) {
        for (EventParticipationStatus state : values()) {
            if (state.name().equalsIgnoreCase(stringStatus)) {
                return Optional.of(state);
            }
        }

        return Optional.empty();
    }
}