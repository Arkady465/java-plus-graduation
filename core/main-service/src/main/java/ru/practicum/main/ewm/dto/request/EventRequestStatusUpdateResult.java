package ru.practicum.main.ewm.dto.request;

import java.util.List;

import lombok.Value;

@Value
public class EventRequestStatusUpdateResult {
    List<ParticipationRequestDto> confirmedRequests;
    List<ParticipationRequestDto> rejectedRequests;
}
