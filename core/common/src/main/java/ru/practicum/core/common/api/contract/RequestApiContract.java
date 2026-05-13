package ru.practicum.core.common.api.contract;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.core.common.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

// контракт для внутреннего API
public interface RequestApiContract {
    @GetMapping("/requests/countConfirmed")
    Map<Long, Long> countConfirmedRequests(@RequestParam List<Long> eventIds);

    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId);
}