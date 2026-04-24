package ru.practicum.core.common.api.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import ru.practicum.core.common.api.client.EventClient;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.common.exception.ServiceUnavailableException;

@Slf4j
@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {
    @Override
    public EventClient create(Throwable cause) {
        if (cause instanceof NotFoundException) {
            log.debug("Rethrowing business exception");
            throw (RuntimeException) cause;
        }

        return new EventClient() {
            @Override
            public EventFullDto getEventInner(Long eventId) {
                log.warn("event-service is not available, returning fallback response");
                throw new ServiceUnavailableException("event-service is not available");
            }
        };
    }
}