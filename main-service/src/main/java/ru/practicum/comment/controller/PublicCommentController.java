package ru.practicum.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping("/event/{eventId}")
    public List<CommentDto> getCommentsForEvent(@PathVariable @Positive Long eventId,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        log.debug("Controller: getCommentsForEvent eventId={}, from={}, size={}", eventId, from, size);
        return commentService.getCommentsForEvent(eventId, PageRequest.of(from / size, size));
    }
}
