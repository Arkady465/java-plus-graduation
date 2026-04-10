package ru.practicum.main.ewm.controller;

import java.util.List;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.ewm.dto.event.EventFullDto;
import ru.practicum.main.ewm.dto.event.EventShortDto;
import ru.practicum.main.ewm.dto.event.PublicEventsQuery;
import ru.practicum.main.ewm.model.Enums.Sort;
import ru.practicum.main.ewm.service.EventService;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicEventsController {
    private final EventService service;

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") boolean onlyAvailable,
            @RequestParam(required = false) Sort sort,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getPublicEvents(new PublicEventsQuery(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size
        ));
    }

    @GetMapping("/{id}")
    public EventFullDto getOne(@PathVariable long id) {
        return service.getPublicEvent(id);
    }
}

