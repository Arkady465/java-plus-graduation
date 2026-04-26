package ru.practicum.core.comment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class CommentParam {
    private Long userId;
    private Long eventId;
    private Long commentId;
    private CommentDto commentDto;
    private UpdateCommentRequest updateCommentRequest;
    private Integer from;
    private Integer size;
}