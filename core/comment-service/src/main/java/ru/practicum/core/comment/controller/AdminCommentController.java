package ru.practicum.core.comment.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.practicum.core.comment.service.CommentService;
import ru.practicum.core.common.api.contract.CommentApiContract;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class AdminCommentController implements CommentApiContract {
    private final CommentService commentService;

    @DeleteMapping("/admin/events/{eventId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long eventId,
                       @PathVariable @Positive Long commentId) {
        log.debug("DELETE /admin/events/{}/comments/{}", eventId, commentId);

        commentService.deleteByAdmin(eventId, commentId);
    }

    @Override
    @GetMapping("/admin/comments/count")
    public Map<Long, Long> countByEvents(@RequestParam List<Long> eventIds) {
        log.debug("GET /admin/comments/count");
        log.debug("Params: {}", eventIds);

        return commentService.countByEvents(eventIds);
    }

    @Override
    @GetMapping("/admin/events/{eventId}/comments/count")
    public Long countByEvent(@PathVariable Long eventId) {
        log.debug("GET /admin/events/{}/comments/count", eventId);

        return commentService.countByEvent(eventId);
    }
}