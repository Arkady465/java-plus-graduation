package ru.practicum.stats.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatsController {
    @GetMapping("/ping")
    public String ping() {
        return "stats-server:ok";
    }
}

