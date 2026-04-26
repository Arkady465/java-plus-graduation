package ru.practicum.stats.analyzer.service.database;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.model.UserAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserActionService {
    void save(UserActionAvro userActionAvro);

    Map<Long, Double> getInteractionsCount(List<Long> eventIds);

    List<UserAction> getByUserId(long userId);

    Map<Long, Double> getUserScoresForEvents(long userId, Collection<Long> eventIds);
}