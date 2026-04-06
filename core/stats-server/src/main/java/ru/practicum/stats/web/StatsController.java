package ru.practicum.stats.web;

import static java.util.stream.Collectors.groupingBy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CopyOnWriteArrayList<Hit> hits = new CopyOnWriteArrayList<>();

    @PostMapping(path = "/hit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void hit(@Valid @RequestBody EndpointHit endpointHit) {
        hits.add(Hit.from(endpointHit));
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique
    ) {
        List<Hit> filtered = hits.stream()
                .filter(h -> !h.timestamp().isBefore(start) && !h.timestamp().isAfter(end))
                .filter(h -> uris == null || uris.isEmpty() || uris.contains(h.uri()))
                .toList();

        Map<Key, List<Hit>> grouped = filtered.stream()
                .collect(groupingBy(h -> new Key(h.app(), h.uri())));

        List<ViewStats> result = new ArrayList<>();
        for (Map.Entry<Key, List<Hit>> entry : grouped.entrySet()) {
            long count;
            if (unique) {
                Set<String> ips = entry.getValue().stream().map(Hit::ip).collect(Collectors.toSet());
                count = ips.size();
            } else {
                count = entry.getValue().size();
            }
            result.add(new ViewStats(entry.getKey().app(), entry.getKey().uri(), count));
        }

        result.sort(Comparator.comparing(ViewStats::getHits).reversed());
        return result;
    }

    private record Hit(String app, String uri, String ip, LocalDateTime timestamp) {
        static Hit from(EndpointHit dto) {
            return new Hit(dto.getApp(), dto.getUri(), dto.getIp(), LocalDateTime.parse(dto.getTimestamp(), TS));
        }
    }

    private record Key(String app, String uri) {
    }

    @Data
    public static class EndpointHit {
        private Long id;

        @NotBlank
        private String app;

        @NotBlank
        private String uri;

        @NotBlank
        private String ip;

        @NotNull
        private String timestamp;
    }

    @Data
    public static class ViewStats {
        private final String app;
        private final String uri;
        private final long hits;
    }
}

