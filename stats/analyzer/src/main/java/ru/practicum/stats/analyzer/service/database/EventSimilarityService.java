package ru.practicum.stats.analyzer.service.database;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.model.EventSimilarity;

import java.util.List;

public interface EventSimilarityService {
    void save(EventSimilarityAvro eventSimilarityAvro);

    List<EventSimilarity> getSimilarEvents(long eventId);

    List<EventSimilarity> getSimilarEvents(List<Long> eventIds);
}