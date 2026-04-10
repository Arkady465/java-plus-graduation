package ru.practicum.main.ewm.dto.compilation;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCompilationRequest {
    @Size(min = 1, max = 50)
    private String title;

    private Boolean pinned;

    private Set<Long> events;
}
