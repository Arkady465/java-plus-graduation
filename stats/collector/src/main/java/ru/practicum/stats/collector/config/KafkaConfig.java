package ru.practicum.stats.collector.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("kafka")
public class KafkaConfig {
    private String server;
    private Topics topics;

    @Getter
    @AllArgsConstructor
    public static class Topics {
        private String users;
    }
}