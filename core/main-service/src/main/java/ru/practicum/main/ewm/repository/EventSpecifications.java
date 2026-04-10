package ru.practicum.main.ewm.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.main.ewm.domain.EventEntity;
import ru.practicum.main.ewm.model.Enums.EventState;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<EventEntity> publishedInWindow(LocalDateTime start, LocalDateTime end) {
        return (root, q, cb) -> cb.and(
                cb.equal(root.get("state"), EventState.PUBLISHED),
                cb.between(root.get("eventDate"), start, end)
        );
    }

    public static Specification<EventEntity> paidEquals(Boolean paid) {
        if (paid == null) {
            return (root, q, cb) -> cb.conjunction();
        }
        return (root, q, cb) -> cb.equal(root.get("paid"), paid);
    }

    public static Specification<EventEntity> categoryIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return (root, q, cb) -> cb.conjunction();
        }
        return (root, q, cb) -> root.get("category").get("id").in(categoryIds);
    }

    public static Specification<EventEntity> textMatches(String text) {
        if (text == null || text.isBlank()) {
            return (root, q, cb) -> cb.conjunction();
        }
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("annotation")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    public static Specification<EventEntity> adminFilter(List<Long> users, List<EventState> states,
                                                       List<Long> categories, LocalDateTime start, LocalDateTime end) {
        return (root, q, cb) -> {
            Predicate p = cb.conjunction();
            if (users != null && !users.isEmpty()) {
                p = cb.and(p, root.get("initiator").get("id").in(users));
            }
            if (states != null && !states.isEmpty()) {
                p = cb.and(p, root.get("state").in(states));
            }
            if (categories != null && !categories.isEmpty()) {
                p = cb.and(p, root.get("category").get("id").in(categories));
            }
            p = cb.and(p, cb.between(root.get("eventDate"), start, end));
            return p;
        };
    }
}
