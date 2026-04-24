package ru.practicum.core.comment.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import ru.practicum.core.comment.dto.CommentDto;
import ru.practicum.core.comment.dto.UpdateCommentRequest;
import ru.practicum.core.comment.model.Comment;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.user.UserDto;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommentMapper {
    public static Comment toNewComment(UserDto user, EventFullDto event, CommentDto commentDto) {
        Comment comment = new Comment();

        comment.setText(commentDto.getText());
        comment.setUser(user.getId());
        comment.setEvent(event.getId());

        return comment;
    }

    public static CommentDto toCommentDto(Comment comment) {
        CommentDto commentDto = new CommentDto();

        commentDto.setId(comment.getId());
        commentDto.setText(comment.getText());
        commentDto.setUserId(comment.getUser());
        commentDto.setEventId(comment.getEvent());
        commentDto.setCreatedOn(comment.getCreatedOn());
        commentDto.setEditedOn(comment.getEditedOn());

        return commentDto;
    }

    public static void updateFields(Comment comment, UpdateCommentRequest updateCommentRequest) {
        comment.setText(updateCommentRequest.getText());

        comment.setEditedOn(LocalDateTime.now());
    }
}