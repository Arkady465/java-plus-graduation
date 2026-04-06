package ru.practicum.main.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.stats.StatsClient;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MainController {
    private final StatsClient statsClient;

    @GetMapping("/ping")
    public String ping() {
        statsClient.hit("main-service");
        return "{\"status\":\"ok\"}";
    }

    @GetMapping("/stats")
    public String stats() {
        return statsClient.stats();
    }
}

