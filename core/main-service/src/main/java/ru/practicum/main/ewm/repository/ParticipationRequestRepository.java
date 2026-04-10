package ru.practicum.main.ewm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.ewm.domain.ParticipationRequestEntity;
import ru.practicum.main.ewm.model.Enums.RequestStatus;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequestEntity, Long> {

    List<ParticipationRequestEntity> findByRequesterIdOrderByIdAsc(Long requesterId);

    List<ParticipationRequestEntity> findByEventIdOrderByIdAsc(Long eventId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByRequesterIdAndEventIdAndStatusNot(Long requesterId, Long eventId, RequestStatus status);

    List<ParticipationRequestEntity> findByEventIdAndStatus(Long eventId, RequestStatus status);
}
