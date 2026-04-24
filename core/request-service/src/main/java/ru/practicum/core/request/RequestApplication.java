package ru.practicum.core.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

import ru.practicum.core.common.api.client.EventClient;
import ru.practicum.core.common.api.client.UserClient;

@EnableFeignClients(clients = {EventClient.class, UserClient.class})
@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.core.request", "ru.practicum.core.common.api.fallback"})
public class RequestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestApplication.class, args);
    }
}