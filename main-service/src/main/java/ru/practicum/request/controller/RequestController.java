package ru.practicum.request.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getRequestsByUserId(@PathVariable("userId") @Positive Long userId) {
        log.debug("Controller: getRequestsByUserId userId={}", userId);
        return requestService.getRequestsByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@PathVariable("userId") @Positive Long userId,
                                              @RequestParam("eventId") @Positive Long eventId) {
        log.debug("Controller: addRequest userId={}, eventId={}", userId, eventId);
        return requestService.addRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable("userId") @Positive Long userId,
                                                 @PathVariable("requestId") @Positive Long requestId) {
        log.debug("Controller: cancelRequest userId={}, requestId={}", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}
