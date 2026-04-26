package ru.practicum.core.common.api.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.config.UserClientConfig;
import ru.practicum.core.common.api.contract.UserApiContract;
import ru.practicum.core.common.api.fallback.UserClientFallbackFactory;

@FeignClient(
        name = "user-service",
        path = "/admin/users",
        configuration = UserClientConfig.class,
        fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient extends UserApiContract {
}