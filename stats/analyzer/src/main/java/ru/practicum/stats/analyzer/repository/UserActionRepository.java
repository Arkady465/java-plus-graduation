package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ru.practicum.stats.analyzer.model.UserAction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findByEventIdIn(List<Long> ids);

    Optional<UserAction> findByUserIdAndEventId(long userId, long eventId);

    List<UserAction> findByUserId(long userId);

    @Query("SELECT ua.eventId, ua.action FROM UserAction ua " +
            "WHERE ua.userId = :userId AND ua.eventId IN :eventIds")
    List<Object[]> getUserScoresForEvents(long userId, Collection<Long> eventIds);
}