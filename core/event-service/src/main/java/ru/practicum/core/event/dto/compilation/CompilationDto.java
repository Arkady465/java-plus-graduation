package ru.practicum.core.event.dto.compilation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import ru.practicum.core.common.dto.event.EventShortDto;

import java.util.Set;

@Data
public class CompilationDto {
    private Set<EventShortDto> events;

    @NotNull
    private Long id;

    @NotNull
    private Boolean pinned;

    @NotNull
    private String title;
}