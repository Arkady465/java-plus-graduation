package ru.practicum.main.ewm.dto.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Value;
import ru.practicum.main.ewm.dto.category.CategoryDto;
import ru.practicum.main.ewm.dto.user.UserShortDto;

@Value
@Builder
public class EventShortDto {
    long id;
    String title;
    String annotation;
    CategoryDto category;
    boolean paid;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;
    UserShortDto initiator;
    long views;
    long confirmedRequests;
}
