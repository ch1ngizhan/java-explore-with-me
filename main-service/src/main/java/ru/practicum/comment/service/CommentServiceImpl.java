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
        log.debug("Попытка создания комментария: userId={}, eventId={}", userId, eventId);

        User user = userService.getUserByIdOrThrow(userId);
        Event event = eventService.getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            log.warn("Попытка комментирования неопубликованного события: userId={}, eventId={}, state={}",
                    userId, eventId, event.getState());
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toComment(newCommentDto, user, event);
        comment = commentRepository.save(comment);

        log.info("Комментарий создан: commentId={}, authorId={}, eventId={}",
                comment.getId(), userId, eventId);

        return commentMapper.toDto(comment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto newCommentDto) {
        log.debug("Попытка обновления комментария: commentId={}, userId={}, eventId={}",
                commentId, userId, eventId);

        Comment comment = commentRepository.findByIdAndUserIdAndEventId(commentId, userId, eventId)
                .orElseThrow(() -> {
                    log.warn("Комментарий не найден для обновления: commentId={}, userId={}, eventId={}",
                            commentId, userId, eventId);
                    return new NotFoundException(String.format(
                            "Комментарий с id=%d от пользователя с id=%d к событию с id=%d не найден",
                            commentId, userId, eventId));
                });

        commentMapper.updateCommentFromDto(newCommentDto, comment);
        comment = commentRepository.save(comment);

        log.info("Комментарий обновлен: commentId={}, userId={}, eventId={}",
                commentId, userId, eventId);

        return commentMapper.toDto(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.debug("Попытка удаления комментария: commentId={}, userId={}", commentId, userId);

        commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> {
                    log.warn("Комментарий не найден для удаления: commentId={}, userId={}",
                            commentId, userId);
                    return new NotFoundException(String.format(
                            "Комментарий с id=%d от пользователя с id=%d не найден",
                            commentId, userId));
                });

        commentRepository.deleteById(commentId);
        log.info("Комментарий удален пользователем: commentId={}, userId={}", commentId, userId);
    }

    @Override
    @Transactional
    public void deleteCommentAdmin(Long commentId) {
        log.debug("Попытка удаления комментария администратором: commentId={}", commentId);

        commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Комментарий не найден для admin-удаления: commentId={}", commentId);
                    return new NotFoundException(String.format(
                            "Комментарий с id=%d не найден", commentId));
                });

        commentRepository.deleteById(commentId);
        log.info("Комментарий удален администратором: commentId={}", commentId);
    }

    @Override
    public List<CommentDto> getCommentsForEvent(Long eventId, Pageable pageable) {
        log.debug("Запрос комментариев события: eventId={}, page={}, size={}",
                eventId, pageable.getPageNumber(), pageable.getPageSize());

        eventService.getEventOrThrow(eventId);

        List<CommentDto> comments = commentRepository.findByEventId(eventId, pageable)
                .stream()
                .map(commentMapper::toDto)
                .toList();

        log.info("Получено {} комментариев для события {}", comments.size(), eventId);
        return comments;
    }

    @Override
    public List<CommentDto> getCommentsForUser(Long userId, Pageable pageable) {
        log.debug("Запрос комментариев пользователя: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        userService.getUserByIdOrThrow(userId);

        List<CommentDto> comments = commentRepository.findByUserId(userId, pageable)
                .stream()
                .map(commentMapper::toDto)
                .toList();

        log.info("Получено {} комментариев пользователя {}", comments.size(), userId);
        return comments;
    }
}
