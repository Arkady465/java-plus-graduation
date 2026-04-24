package ru.practicum.core.common.api.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.config.EventClientConfig;
import ru.practicum.core.common.api.contract.EventApiContract;
import ru.practicum.core.common.api.fallback.EventClientFallbackFactory;

@FeignClient(
        name = "event-service",
        configuration = EventClientConfig.class,
        fallbackFactory = EventClientFallbackFactory.class)
public interface EventClient extends EventApiContract {
}