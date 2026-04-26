package ru.practicum.core.event.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.practicum.client.StatsClient;
import ru.practicum.client.StatsClientImpl;

@Configuration
public class StatsClientConfig {

    @Bean
    public RetryTemplate retryTemplate(StatsServerConfig statsServerConfig) {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(statsServerConfig.getRetryBackOffPeriod());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(statsServerConfig.getRetryMaxAttempts());
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    @Lazy // иначе DiscoveryClient инициализируется преждевременно и main-service регистрируется в Eureka с портом 0
    public StatsClient statClient(DiscoveryClient discoveryClient,
                                  RetryTemplate retryTemplate,
                                  StatsServerConfig statsServerConfig) {
        return new StatsClientImpl(discoveryClient, retryTemplate, statsServerConfig.getId());
    }
}