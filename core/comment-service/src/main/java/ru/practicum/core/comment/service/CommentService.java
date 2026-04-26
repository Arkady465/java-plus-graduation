package ru.practicum.core.comment.service;

import ru.practicum.core.comment.dto.CommentDto;
import ru.practicum.core.comment.dto.CommentParam;

import java.util.List;
import java.util.Map;

public interface CommentService {

    CommentDto create(CommentParam commentParam);

    CommentDto update(CommentParam commentParam);

    void deleteByUser(CommentParam commentParam);

    void deleteByAdmin(Long eventId, Long commentId);

    List<CommentDto> findAllByEventId(CommentParam commentParam);

    Map<Long, Long> countByEvents(List<Long> eventIds);

    Long countByEvent(Long eventId);
}