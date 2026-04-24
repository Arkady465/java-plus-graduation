package ru.practicum.core.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.practicum.core.user.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByIdIn(Collection<Long> ids);
}