package ru.practicum.core.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ru.practicum.core.comment.model.Comment;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByEvent(Long eventId, Pageable pageable);

    Long countByEvent(Long eventId);

    @Query("SELECT c.event, COUNT(c) FROM Comment c " +
            "WHERE c.event IN :ids " +
            "GROUP BY c.event")
    List<Object[]> countByEventIdIn(Collection<Long> ids);
}