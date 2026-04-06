package ru.practicum.main.ewm.dto;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import ru.practicum.main.ewm.model.Enums.AdminStateAction;
import ru.practicum.main.ewm.model.Enums.EventState;
import ru.practicum.main.ewm.model.Enums.RequestStatus;
import ru.practicum.main.ewm.model.Enums.Sort;
import ru.practicum.main.ewm.model.Enums.UserStateAction;

public final class Dtos {
    private Dtos() {
    }

    @Data
    public static class NewUserRequest {
        @NotBlank
        @Size(min = 2, max = 250)
        private String name;

        @NotBlank
        @Size(min = 6, max = 254)
        @Email
        private String email;
    }

    @Value
    public static class UserDto {
        long id;
        String name;
        String email;
    }

    @Value
    public static class UserShortDto {
        long id;
        String name;
    }

    @Data
    public static class NewCategoryDto {
        @NotBlank
        @Size(min = 1, max = 50)
        private String name;
    }

    @Data
    public static class CategoryDto {
        private Long id;

        @NotBlank
        @Size(min = 1, max = 50)
        private String name;
    }

    @Data
    public static class Location {
        @NotNull
        private Float lat;

        @NotNull
        private Float lon;
    }

    @Data
    public static class NewEventDto {
        @NotBlank
        @Size(min = 3, max = 120)
        private String title;

        @NotBlank
        @Size(min = 20, max = 2000)
        private String annotation;

        @NotBlank
        @Size(min = 20, max = 7000)
        private String description;

        @NotNull
        private Long category;

        @NotBlank
        private String eventDate; // yyyy-MM-dd HH:mm:ss

        @NotNull
        @Valid
        private Location location;

        private Boolean paid = false;

        @Min(0)
        private Integer participantLimit = 0;

        private Boolean requestModeration = true;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateEventUserRequest {
        @Size(min = 3, max = 120)
        private String title;

        @Size(min = 20, max = 2000)
        private String annotation;

        @Size(min = 20, max = 7000)
        private String description;

        private Long category;

        private String eventDate;

        @Valid
        private Location location;

        private Boolean paid;

        @Min(0)
        private Integer participantLimit;

        private Boolean requestModeration;

        private UserStateAction stateAction;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateEventAdminRequest {
        @Size(min = 3, max = 120)
        private String title;

        @Size(min = 20, max = 2000)
        private String annotation;

        @Size(min = 20, max = 7000)
        private String description;

        private Long category;

        private String eventDate;

        @Valid
        private Location location;

        private Boolean paid;

        @Min(0)
        private Integer participantLimit;

        private Boolean requestModeration;

        private AdminStateAction stateAction;
    }

    @Value
    @Builder
    public static class EventFullDto {
        long id;
        String title;
        String annotation;
        CategoryDto category;
        boolean paid;
        String eventDate;
        UserShortDto initiator;
        String description;
        int participantLimit;
        EventState state;
        String createdOn;
        String publishedOn;
        Location location;
        boolean requestModeration;
        long views;
        long confirmedRequests;
    }

    @Value
    @Builder
    public static class EventShortDto {
        long id;
        String title;
        String annotation;
        CategoryDto category;
        boolean paid;
        String eventDate;
        UserShortDto initiator;
        long views;
        long confirmedRequests;
    }

    @Data
    public static class NewCompilationDto {
        @NotBlank
        @Size(min = 1, max = 50)
        private String title;

        private Boolean pinned = false;

        private Set<Long> events;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateCompilationRequest {
        @Size(min = 1, max = 50)
        private String title;

        private Boolean pinned;

        private Set<Long> events;
    }

    @Value
    @Builder
    public static class CompilationDto {
        long id;
        String title;
        boolean pinned;
        Set<EventShortDto> events;
    }

    @Data
    public static class EventRequestStatusUpdateRequest {
        private List<Long> requestIds;
        private RequestStatusAction status;
    }

    public enum RequestStatusAction {CONFIRMED, REJECTED}

    @Value
    public static class ParticipationRequestDto {
        long id;
        long requester;
        long event;
        String status;
        String created;
    }

    @Value
    public static class EventRequestStatusUpdateResult {
        List<ParticipationRequestDto> confirmedRequests;
        List<ParticipationRequestDto> rejectedRequests;
    }

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
}

