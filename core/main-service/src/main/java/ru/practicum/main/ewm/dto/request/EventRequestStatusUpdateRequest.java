package ru.practicum.main.ewm.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private RequestStatusAction status;
}
