package ru.practicum.main.ewm.api;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiError {
    List<String> errors;
    String message;
    String reason;
    String status;
    String timestamp;
}

