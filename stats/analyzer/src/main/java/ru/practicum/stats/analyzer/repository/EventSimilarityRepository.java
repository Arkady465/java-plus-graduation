package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.analyzer.model.EventSimilarityId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {
    Optional<EventSimilarity> findByEventAAndEventB(long eventA, long eventB);

    @Query("SELECT es FROM EventSimilarity es " +
            "WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findByEventAOrEventB(long eventId);

    @Query("SELECT es FROM EventSimilarity es " +
            "WHERE es.eventA IN :eventIds OR es.eventB IN :eventIds")
    List<EventSimilarity> findByEventAInOrEventBIn(Collection<Long> eventIds);
}