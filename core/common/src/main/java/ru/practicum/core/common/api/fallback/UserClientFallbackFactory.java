package ru.practicum.core.common.api.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import ru.practicum.core.common.api.client.UserClient;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.common.exception.ServiceUnavailableException;

import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        if (cause instanceof NotFoundException) {
            log.debug("Rethrowing business exception");
            throw (RuntimeException) cause;
        }

        return new UserClient() {
            @Override
            public UserDto getById(Long userId) {
                log.warn("user-service is not available, returning fallback response");
                throw new ServiceUnavailableException("user-service is not available");
            }

            @Override
            public UserShortDto getShortById(Long userId) {
                log.warn("user-service is not available, returning fallback response");
                throw new ServiceUnavailableException("user-service is not available");
            }

            @Override
            public List<UserShortDto> getShortByIdIn(List<Long> ids) {
                log.warn("user-service is not available, returning fallback response");
                throw new ServiceUnavailableException("user-service is not available");
            }
        };
    }
}