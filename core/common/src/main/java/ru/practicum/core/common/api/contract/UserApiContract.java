package ru.practicum.core.common.api.contract;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;

import java.util.List;

// контракт для внутненнего API
public interface UserApiContract {
    @GetMapping("/{userId}")
    UserDto getById(@PathVariable @Positive Long userId);

    @GetMapping("/short/{userId}")
    UserShortDto getShortById(@PathVariable @Positive Long userId);

    @GetMapping("/short")
    List<UserShortDto> getShortByIdIn(@RequestParam List<Long> ids);
}