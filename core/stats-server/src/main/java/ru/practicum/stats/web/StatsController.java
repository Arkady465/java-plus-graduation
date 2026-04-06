package ru.practicum.stats.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsController {
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @PostMapping("/hit")
    public void hit(@RequestParam(defaultValue = "unknown") String source) {
        counters.computeIfAbsent(source, k -> new AtomicLong()).incrementAndGet();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return counters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));
    }
}

