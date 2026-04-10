package ru.practicum.main.ewm.dto.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.main.ewm.model.Enums.UserStateAction;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateEventUserRequest {
    @Size(min = 3, max = 120)
    private String title;

    @Size(min = 20, max = 2000)
    private String annotation;

    @Size(min = 20, max = 7000)
    private String description;

    private Long category;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @Valid
    private EventLocationDto location;

    private Boolean paid;

    @Min(0)
    private Integer participantLimit;

    private Boolean requestModeration;

    private UserStateAction stateAction;
}
