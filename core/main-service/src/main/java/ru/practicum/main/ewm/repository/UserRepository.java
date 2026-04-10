package ru.practicum.main.ewm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.ewm.domain.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findByIdIn(List<Long> ids);
}
