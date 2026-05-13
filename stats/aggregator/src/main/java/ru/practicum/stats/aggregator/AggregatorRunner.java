package ru.practicum.stats.aggregator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import ru.practicum.stats.aggregator.service.Aggregator;

@Component
@RequiredArgsConstructor
public class AggregatorRunner implements CommandLineRunner {
    private final Aggregator aggregator;

    @Override
    public void run(String... args) throws Exception {
        aggregator.start();
    }
}