package ru.practicum.main.ewm.dto.compilation;

import java.util.Set;

import lombok.Builder;
import lombok.Value;
import ru.practicum.main.ewm.dto.event.EventShortDto;

@Value
@Builder
public class CompilationDto {
    long id;
    String title;
    boolean pinned;
    Set<EventShortDto> events;
}
