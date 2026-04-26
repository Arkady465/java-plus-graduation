package ru.practicum.core.event.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("stats-server")
public class StatsServerConfig {
    private String id;
    private Long retryBackOffPeriod;
    private Integer retryMaxAttempts;
}