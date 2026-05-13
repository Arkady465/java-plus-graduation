package ru.practicum.core.request.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.stats.client.CollectorClient;

@Configuration
public class StatsClientConfig {
    @Bean
    public CollectorClient collectorClient() {
        return new CollectorClient();
    }
}