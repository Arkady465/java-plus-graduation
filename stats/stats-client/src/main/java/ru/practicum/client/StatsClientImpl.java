package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.StatsParamDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.exception.StatsServerNotFoundException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClientImpl implements StatsClient {
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServerId;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RestClient createRestClient() {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        String baseUrl = "http://" + instance.getHost() + ":" + instance.getPort();
        log.info("Stat-client использует stats-server по следующему url: {}", baseUrl);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            createRestClient().post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Stat-client отправил статистику по url: {}", endpointHitDto);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(StatsParamDto statsParamDto) {
        log.info("Запрос статистики для uri: {}", statsParamDto.getUris());
        log.debug("statsParamDto: {}", statsParamDto);

        try {
            List<ViewStatsDto> stats = createRestClient().get()
                    .uri(uriBuilder -> {
                        String startEncoded = URLEncoder.encode(
                                statsParamDto.getStart().format(formatter),
                                StandardCharsets.UTF_8
                        );
                        String endEncoded = URLEncoder.encode(
                                statsParamDto.getEnd().format(formatter),
                                StandardCharsets.UTF_8
                        );

                        uriBuilder.path("/stats")
                                .queryParam("start", startEncoded)
                                .queryParam("end", endEncoded);

                        if (statsParamDto.getUris() != null && !statsParamDto.getUris().isEmpty()) {
                            uriBuilder.queryParam("uris", statsParamDto.getUris());
                        }

                        if (statsParamDto.getIsUnique() != null) {
                            uriBuilder.queryParam("unique", statsParamDto.getIsUnique());
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (stats == null) {
                log.warn("Отсутствует статистика посещений для: {}", statsParamDto);
                return Collections.emptyList();
            }

            log.debug("Выгружена статистика: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServerId);

        if (instances.isEmpty()) {
            throw new StatsServerNotFoundException("Не найден сервис статистики с id: " + statsServerId);
        }

        return instances.getFirst();
    }
}