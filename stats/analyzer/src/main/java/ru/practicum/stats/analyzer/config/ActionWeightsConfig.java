package ru.practicum.stats.analyzer.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("action-weights")
public class ActionWeightsConfig {
    private double view;
    private double register;
    private double like;
}