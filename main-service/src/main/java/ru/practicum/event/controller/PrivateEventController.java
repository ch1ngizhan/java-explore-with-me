package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.service.EventService;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.RequestStatusUpdateRequest;
import ru.practicum.request.dto.RequestStatusUpdateResult;
import ru.practicum.request.service.RequestService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {
    private final EventService eventService;
    private final RequestService requestService;

    @GetMapping
    public List<EventShortDto> getEvents(@PathVariable("userId") @Positive Long userId,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                         @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        Pageable pageable = PageRequest.of(from / size, size);
        log.debug("Controller: getEvents userId={}, pageable={}", userId, pageable);
        return eventService.getEvents(userId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto createEvent(@PathVariable("userId") @Positive Long userId,
                                @RequestBody @Valid NewEventDto newEventDto
    ) {
        log.debug("Controller: createEvent userId={}, data={}", userId, newEventDto);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping("/{eventId}")
    public EventDto getEvent(@PathVariable("userId") @Positive Long userId,
                             @PathVariable("eventId") @Positive Long eventId,
                             HttpServletRequest request
    ) {
        log.debug("Controller: getEvent userId={}, eventId={}", userId, eventId);
        return eventService.getEvent(userId, eventId, request.getRemoteAddr());
    }

    @PatchMapping("/{eventId}")
    public EventDto updateEvent(@PathVariable("userId") @Positive Long userId,
                                @PathVariable("eventId") @Positive Long eventId,
                                @RequestBody @Valid UpdateEventUserRequest request
    ) {
        log.debug("Controller: updateEvent userId={}, eventId={}, data={}", userId, eventId, request);
        return eventService.updateEvent(userId, eventId, request);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsByEvent(@PathVariable("userId") @Positive Long userId,
                                                            @PathVariable("eventId") @Positive Long eventId
    ) {
        log.debug("Controller: getRequestsByEvent userId={}, eventId={}", userId, eventId);
        return requestService.getRequestsByEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public RequestStatusUpdateResult updateRequestStatus(@PathVariable("userId") @Positive Long userId,
                                                         @PathVariable("eventId") @Positive Long eventId,
                                                         @RequestBody RequestStatusUpdateRequest request
    ) {
        log.debug("Controller: updateRequestStatus userId={}, eventId={}, data={}", userId, eventId, request);
        return requestService.updateRequestStatus(userId, eventId, request);
    }
}
