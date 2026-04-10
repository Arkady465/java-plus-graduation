package ru.practicum.main.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.ewm.domain.CompilationEntity;

public interface CompilationRepository extends JpaRepository<CompilationEntity, Long> {
}
