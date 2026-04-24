package ru.practicum.core.common.api.config;

import org.springframework.context.annotation.Bean;

import ru.practicum.core.common.api.decoder.RequestErrorDecoder;

public class RequestClientConfig {
    @Bean
    public RequestErrorDecoder requestErrorDecoder() {
        return new RequestErrorDecoder();
    }
}