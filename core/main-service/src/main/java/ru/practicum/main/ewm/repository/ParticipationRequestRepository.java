package ru.practicum.main.ewm.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.ewm.domain.ParticipationRequestEntity;
import ru.practicum.main.ewm.model.Enums.RequestStatus;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequestEntity, Long> {

    List<ParticipationRequestEntity> findByRequesterIdOrderByIdAsc(Long requesterId);

    List<ParticipationRequestEntity> findByEventIdOrderByIdAsc(Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.event.id, count(r) from ParticipationRequestEntity r "
            + "where r.event.id in :eventIds and r.status = :status group by r.event.id")
    List<Object[]> countByEventIdInAndStatusGrouped(@Param("eventIds") Collection<Long> eventIds,
                                                     @Param("status") RequestStatus status);

    boolean existsByRequesterIdAndEventIdAndStatusNot(Long requesterId, Long eventId, RequestStatus status);

    List<ParticipationRequestEntity> findByEventIdAndStatus(Long eventId, RequestStatus status);
}
