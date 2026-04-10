package ru.practicum.main.ewm.dto.event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventLocationDto {
    @NotNull
    private Float lat;

    @NotNull
    private Float lon;
}
