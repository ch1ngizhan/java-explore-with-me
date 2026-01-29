package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @RequestBody @Valid NewCommentDto newCommentDto
    ) {
        log.debug("Controller: createComment userId={}, eventId={}, data={}", userId, eventId, newCommentDto);
        return commentService.createComment(userId, eventId, newCommentDto);
    }

    @PatchMapping("/{eventId}/{commentId}")
    public CommentDto updateComment(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @PathVariable @Positive Long commentId,
                                    @RequestBody @Valid NewCommentDto newCommentDto
    ) {
        log.debug("Controller: updateComment userId={}, eventId={}, commentId={}, data={}", userId, eventId, commentId,
                newCommentDto);
        return commentService.updateComment(userId, eventId, commentId, newCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Positive Long userId,
                              @PathVariable @Positive Long commentId
    ) {
        log.debug("Controller: deleteComment userId={}, commentId={}", userId, commentId);
        commentService.deleteComment(userId, commentId);
    }

    @GetMapping
    public List<CommentDto> getCommentsForUser(@PathVariable @Positive Long userId,
                                               @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                               @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        log.debug("Controller: getCommentsForUser userId={}, from={}, size={}", userId, from, size);
        return commentService.getCommentsForUser(userId, PageRequest.of(from / size, size));
    }
}
