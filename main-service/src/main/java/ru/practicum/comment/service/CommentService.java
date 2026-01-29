package ru.practicum.comment.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto newCommentDto);

    void deleteComment(Long userId, Long commentId);

    void deleteCommentAdmin(Long commentId);

    List<CommentDto> getCommentsForEvent(Long eventId, Pageable pageable);

    List<CommentDto> getCommentsForUser(Long userId, Pageable pageable);
}
