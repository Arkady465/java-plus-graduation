package ru.practicum.core.common.api.contract;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

// контракт для внутреннего API
public interface RequestApiContract {
    @GetMapping("/requests/countConfirmed")
    Map<Long, Long> countConfirmedRequests(@RequestParam List<Long> eventIds);
}