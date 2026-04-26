package ru.practicum.core.common.api.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import ru.practicum.core.common.api.client.RequestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {
    @Override
    public RequestClient create(Throwable cause) {
        return new RequestClient() {
            @Override
            public Map<Long, Long> countConfirmedRequests(List<Long> eventIds) {
                log.warn("request-service is not available, returning fallback response");

                return eventIds.stream()
                        .collect(Collectors.toMap(Function.identity(), id -> 0L));
            }
        };
    }
}