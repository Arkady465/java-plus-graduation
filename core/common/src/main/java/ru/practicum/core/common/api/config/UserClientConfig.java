package ru.practicum.core.common.api.config;

import org.springframework.context.annotation.Bean;

import ru.practicum.core.common.api.decoder.UserErrorDecoder;

public class UserClientConfig {
    @Bean
    public UserErrorDecoder userErrorDecoder() {
        return new UserErrorDecoder();
    }
}