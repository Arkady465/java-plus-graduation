package ru.practicum.main.ewm.dto.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Value;
import ru.practicum.main.ewm.dto.category.CategoryDto;
import ru.practicum.main.ewm.dto.user.UserShortDto;
import ru.practicum.main.ewm.model.Enums.EventState;

@Value
@Builder
public class EventFullDto {
    long id;
    String title;
    String annotation;
    CategoryDto category;
    boolean paid;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;
    UserShortDto initiator;
    String description;
    int participantLimit;
    EventState state;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdOn;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime publishedOn;
    EventLocationDto location;
    boolean requestModeration;
    long views;
    long confirmedRequests;
}
