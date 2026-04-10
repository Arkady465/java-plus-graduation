package ru.practicum.main.ewm.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Value;

@Value
public class ParticipationRequestDto {
    long id;
    long requester;
    long event;
    String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime created;
}
