package ru.practicum.main.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.stats.StatsClient;

@RestController
@RequestMapping("/api")
public class MainController {
    private final StatsClient statsClient;

    public MainController(StatsClient statsClient) {
        this.statsClient = statsClient;
    }

    @GetMapping("/ping")
    public String ping() {
        return "main-service:ok";
    }

    @GetMapping("/stats/ping")
    public String statsPing() {
        return statsClient.ping();
    }
}

