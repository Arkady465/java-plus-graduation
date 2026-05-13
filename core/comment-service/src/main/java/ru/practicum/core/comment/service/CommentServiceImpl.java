package ru.practicum.core.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.core.comment.dto.CommentDto;
import ru.practicum.core.comment.dto.CommentParam;
import ru.practicum.core.common.api.client.EventClient;
import ru.practicum.core.common.api.client.UserClient;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.exception.AccessViolationException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.common.exception.ValidationException;
import ru.practicum.core.comment.mapper.CommentMapper;
import ru.practicum.core.comment.model.Comment;
import ru.practicum.core.comment.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;

    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public CommentDto create(CommentParam commentParam) {
        log.debug("Comment create request for eventId = {} by userId = {}: {}",
                commentParam.getEventId(), commentParam.getUserId(), commentParam.getCommentDto());

        UserDto commentator = userClient.getById(commentParam.getUserId());
        EventFullDto eventFullDto = eventClient.getEventInner(commentParam.getEventId());

        Comment comment = CommentMapper.toNewComment(commentator, eventFullDto, commentParam.getCommentDto());
        comment.setCreatedOn(LocalDateTime.now());
        comment = commentRepository.save(comment);

        log.info("New comment added: {}", comment);
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    public CommentDto update(CommentParam commentParam) {
        log.debug("CommentId = {} update request for eventId = {} by userId = {}: {}",
                commentParam.getCommentId(), commentParam.getEventId(), commentParam.getUserId(),
                commentParam.getUpdateCommentRequest());

        Comment comment = commentRepository.findById(commentParam.getCommentId())
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id = %d not found", commentParam.getCommentId())));

        log.debug("Initial comment state: {}", comment);

        if (!comment.getUser().equals(commentParam.getUserId())) {
            log.warn("No access to edit comment");
            throw new AccessViolationException("No access to edit comment");
        }

        if (!commentParam.getEventId().equals(comment.getEvent())) {
            log.warn("Comment with id = {} doesn't belong to event with id = {}",
                    commentParam.getCommentId(), commentParam.getEventId());
            throw new ValidationException(String.format("Comment with id = %d doesn't belong to event with id = %d",
                    commentParam.getCommentId(), commentParam.getEventId()));
        }

        CommentMapper.updateFields(comment, commentParam.getUpdateCommentRequest());
        comment = commentRepository.save(comment);

        log.debug("Comment has been updated: {}", comment);
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    public void deleteByUser(CommentParam commentParam) {
        log.debug("CommentId = {} delete request for eventId = {} by userId = {}",
                commentParam.getCommentId(), commentParam.getEventId(), commentParam.getUserId());

        Comment comment = commentRepository.findById(commentParam.getCommentId())
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id = %d not found", commentParam.getCommentId())));

        if (!commentParam.getEventId().equals(comment.getEvent())) {
            log.warn("Comment with id = {} doesn't belong to event with id = {}",
                    commentParam.getCommentId(), commentParam.getEventId());
            throw new ValidationException(String.format("Comment with id = %d doesn't belong to event with id = %d",
                    commentParam.getCommentId(), commentParam.getEventId()));
        }

        if (!comment.getUser().equals(commentParam.getUserId())) {
            log.warn("No access to delete comment");
            throw new AccessViolationException("No access to delete comment");
        }

        commentRepository.deleteById(commentParam.getCommentId());
        log.debug("Comment has been deleted by user");
    }

    @Override
    public void deleteByAdmin(Long eventId, Long commentId) {
        log.debug("Comment id = {} delete request for eventId = {} by admin", commentId, eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id = %d not found", commentId)));

        if (!eventId.equals(comment.getEvent())) {
            log.warn("Comment with id = {} doesn't belong to event with id = {}", commentId, eventId);
            throw new ValidationException(String.format("Comment with id = %d doesn't belong to event with id = %d",
                    commentId, eventId));
        }

        commentRepository.deleteById(commentId);
        log.debug("Comment has been deleted by admin");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> findAllByEventId(CommentParam commentParam) {
        log.debug("Comments request for eventId = {}", commentParam.getEventId());

        eventClient.getEventInner(commentParam.getEventId());

        int page = commentParam.getFrom() / commentParam.getSize();
        Sort sort = Sort.by("createdOn").ascending();
        Pageable pageable = PageRequest.of(page, commentParam.getSize(), sort);
        List<Comment> comments = commentRepository.findByEvent(commentParam.getEventId(), pageable).getContent();
        log.debug("Comments size = {}", comments.size());

        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countByEvents(List<Long> eventIds) {
        log.debug("Comments count for events: {}", eventIds);

        return commentRepository.countByEventIdIn(eventIds).stream()
                .collect(Collectors.toMap(
                result -> (Long) result[0],
                result -> (Long) result[1]
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByEvent(Long eventId) {
        log.debug("Comments count for event: {}", eventId);

        return commentRepository.countByEvent(eventId);
    }
}