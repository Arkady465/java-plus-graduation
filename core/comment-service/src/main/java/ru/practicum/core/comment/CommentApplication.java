package ru.practicum.core.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.context.annotation.ComponentScan;
import ru.practicum.core.common.api.client.EventClient;
import ru.practicum.core.common.api.client.UserClient;

@EnableFeignClients(clients = {EventClient.class, UserClient.class})
@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.core.comment", "ru.practicum.core.common.api.fallback"})
public class CommentApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommentApplication.class, args);
    }
}