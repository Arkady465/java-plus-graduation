package ru.practicum.main.ewm.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Data;
import ru.practicum.main.ewm.model.Enums.EventState;
import ru.practicum.main.ewm.model.Enums.RequestStatus;

public final class Entities {
    private Entities() {
    }

    @Data
    public static class User {
        long id;
        String name;
        String email;
    }

    @Data
    public static class Category {
        long id;
        String name;
    }

    @Data
    public static class Location {
        float lat;
        float lon;
    }

    @Data
    public static class Event {
        long id;
        String title;
        String annotation;
        String description;
        long categoryId;
        long initiatorId;
        boolean paid;
        int participantLimit;
        boolean requestModeration;
        EventState state;
        LocalDateTime createdOn;
        LocalDateTime publishedOn;
        LocalDateTime eventDate;
        Location location;
    }

    @Data
    public static class Compilation {
        long id;
        String title;
        boolean pinned;
        Set<Long> eventIds = new LinkedHashSet<>();
    }

    @Data
    public static class ParticipationRequest {
        long id;
        long requesterId;
        long eventId;
        RequestStatus status;
        LocalDateTime created;
    }
}

