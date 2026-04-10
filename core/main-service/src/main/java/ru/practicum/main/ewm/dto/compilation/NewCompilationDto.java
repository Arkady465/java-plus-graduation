package ru.practicum.main.ewm.dto.compilation;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCompilationDto {
    @NotBlank
    @Size(min = 1, max = 50)
    private String title;

    private Boolean pinned = false;

    private Set<Long> events;
}
