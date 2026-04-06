package ru.practicum.main.stats;

import java.net.URI;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class StatsClient {
    private final DiscoveryClient discoveryClient;
    private final RestClient restClient = RestClient.create();

    private final String statsServiceId = "stats-server";

    private final RetryTemplate retryTemplate = createRetryTemplate();

    public void hit(String source) {
        URI uri = makeUri("/hit?source=" + source);
        restClient.post().uri(uri).retrieve().toBodilessEntity();
    }

    public String stats() {
        URI uri = makeUri("/stats");
        return restClient.get().uri(uri).retrieve().body(String.class);
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
}

