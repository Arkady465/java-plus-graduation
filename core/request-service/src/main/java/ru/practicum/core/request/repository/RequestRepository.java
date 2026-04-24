package ru.practicum.core.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ru.practicum.core.request.model.Request;
import ru.practicum.core.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequester(Long requesterId);

    List<Request> findByEvent(Long eventId);

    List<Request> findByEventAndStatus(Long eventId, RequestStatus status);

    Optional<Request> findByRequesterAndEvent(Long requesterId, Long eventId);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.event = :eventId AND r.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEvent(Long eventId);

    @Query("SELECT r.event, COUNT(r) FROM Request r " +
            "WHERE r.event IN :eventIds AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event")
    List<Object[]> countConfirmedRequestsByEventIds(List<Long> eventIds);

    List<Request> findByIdInAndEvent(List<Long> ids, Long eventId);

    List<Request> findByEventAndRequester(Long eventId, Long requesterId);
}