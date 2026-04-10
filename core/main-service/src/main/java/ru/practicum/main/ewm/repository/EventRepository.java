package ru.practicum.main.ewm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.practicum.main.ewm.domain.EventEntity;

public interface EventRepository extends JpaRepository<EventEntity, Long>, JpaSpecificationExecutor<EventEntity> {

    List<EventEntity> findByInitiatorIdOrderByIdAsc(Long initiatorId);

    boolean existsByCategoryId(Long categoryId);
}
