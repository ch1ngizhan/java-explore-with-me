package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final EventService eventService;
    private final UserService userService;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User user = userService.getUserByIdOrThrow(userId);
        Event event = eventService.getEventOrThrow(eventId);

        if (EventState.PUBLISHED != event.getState())
            throw new ConflictException("Нельзя комментировать неопубликованное событие");

        Comment comment = commentMapper.toComment(newCommentDto, user, event);

        log.info("Создан новый комментарий: authorId={}, eventId={}, newCommentDto={}", userId, eventId, newCommentDto);
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto newCommentDto) {
        Comment comment = commentRepository.findByIdAndUserIdAndEventId(commentId, userId, eventId)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "Комментарий с id=%d от пользователя с id=%d к событию с id=%d не найден",
                        commentId, userId, eventId)));

        commentMapper.updateCommentFromDto(newCommentDto, comment);

        log.info("Комментарий обновлен: commentId={}, userId={}, eventId={}, updateCommentDto={}",
                commentId, userId, eventId, newCommentDto);
        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "Комментарий с id=%d от пользователя с id=%d не найден",
                        commentId, userId)));

        log.info("Комментарий удален: commentId={}, userId={}", commentId, userId);
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public void deleteCommentAdmin(Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "Комментарий с id=%d не найден", commentId)));

        log.info("Комментарий удален администратором: commentId={}", commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getCommentsForEvent(Long eventId, Pageable pageable) {
        eventService.getEventOrThrow(eventId);

        log.info("Получен список комментариев для события: eventId={}", eventId);
        return commentRepository.findByEventId(eventId, pageable)
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }

    @Override
    public List<CommentDto> getCommentsForUser(Long userId, Pageable pageable) {
        userService.getUserByIdOrThrow(userId);

        log.info("Получен список комментариев для пользователя: userId={}", userId);
        return commentRepository.findByUserId(userId, pageable)
                .stream()
                .map(commentMapper::toDto)
                .toList();
    }
}
