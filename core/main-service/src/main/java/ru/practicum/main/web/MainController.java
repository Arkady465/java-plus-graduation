package ru.practicum.main.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class MainController {

    @GetMapping("/ping")
    public String ping() {
        return "{\"status\":\"ok\"}";
    }
}

