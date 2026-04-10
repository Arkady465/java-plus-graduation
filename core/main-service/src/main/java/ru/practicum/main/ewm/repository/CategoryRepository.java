package ru.practicum.main.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.ewm.domain.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
