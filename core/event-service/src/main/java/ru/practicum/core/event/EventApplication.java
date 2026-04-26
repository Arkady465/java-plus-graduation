package ru.practicum.core.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

import ru.practicum.core.common.api.client.CommentClient;
import ru.practicum.core.common.api.client.RequestClient;
import ru.practicum.core.common.api.client.UserClient;

@EnableFeignClients(clients = {UserClient.class, CommentClient.class, RequestClient.class})
@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(basePackages = {"ru.practicum.core.event", "ru.practicum.core.common.api.fallback"})
public class EventApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventApplication.class, args);
    }
}