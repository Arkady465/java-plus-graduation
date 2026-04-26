package ru.practicum.core.common.api.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.contract.CommentApiContract;
import ru.practicum.core.common.api.config.CommentClientConfig;
import ru.practicum.core.common.api.fallback.CommentClientFallbackFactory;

@FeignClient(
        name = "comment-service",
        configuration = CommentClientConfig.class,
        fallbackFactory = CommentClientFallbackFactory.class)
public interface CommentClient extends CommentApiContract {
}