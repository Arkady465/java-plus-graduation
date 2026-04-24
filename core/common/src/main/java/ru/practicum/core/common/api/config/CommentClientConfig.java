package ru.practicum.core.common.api.config;

import org.springframework.context.annotation.Bean;

import ru.practicum.core.common.api.decoder.CommentErrorDecoder;

public class CommentClientConfig {
    @Bean
    public CommentErrorDecoder commentErrorDecoder() {
        return new CommentErrorDecoder();
    }
}