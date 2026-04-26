package ru.practicum.core.common.api.contract;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

// контракт для внутреннего API
public interface CommentApiContract {
    @GetMapping("/admin/comments/count")
    Map<Long, Long> countByEvents(@RequestParam List<Long> eventIds);

    @GetMapping("/admin/events/{eventId}/comments/count")
    Long countByEvent(@PathVariable Long eventId);
}