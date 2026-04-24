package ru.practicum.core.common.api.config;

import org.springframework.context.annotation.Bean;

import ru.practicum.core.common.api.decoder.EventErrorDecoder;

public class EventClientConfig {
    @Bean
    public EventErrorDecoder eventErrorDecoder() {
        return new EventErrorDecoder();
    }
}