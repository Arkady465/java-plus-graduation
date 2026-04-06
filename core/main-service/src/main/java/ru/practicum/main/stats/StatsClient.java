package ru.practicum.main.stats;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class StatsClient {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DiscoveryClient discoveryClient;
    private final RestClient restClient = RestClient.create();

    private final String statsServiceId = "stats-server";

    private final RetryTemplate retryTemplate = createRetryTemplate();

    public void hit(String app, String uri, String ip) {
        URI target = makeUri("/hit");
        EndpointHit body = new EndpointHit(app, uri, ip, LocalDateTime.now().format(TS));
        restClient.post()
                .uri(target)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public List<ViewStats> getStats(String start, String end, List<String> uris, boolean unique) {
        URI target = makeUri("/stats?start=" + encode(start) + "&end=" + encode(end) + "&unique=" + unique);
        if (uris != null) {
            for (String u : uris) {
                target = URI.create(target.toString() + "&uris=" + encode(u));
            }
        }
        return restClient.get().uri(target).retrieve().body(Types.VIEW_STATS_LIST);
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private static RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Value
    public static class EndpointHit {
        String app;
        String uri;
        String ip;
        String timestamp;
    }

    @Value
    public static class ViewStats {
        String app;
        String uri;
        long hits;
    }

    private static final class Types {
        static final org.springframework.core.ParameterizedTypeReference<List<ViewStats>> VIEW_STATS_LIST =
                new org.springframework.core.ParameterizedTypeReference<>() {
                };
    }
}

