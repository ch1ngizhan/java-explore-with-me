package ru.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventDto;
import ru.practicum.event.dto.SearchEventAdminRequest;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.service.EventService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventDto> getEventsAdmin(@ModelAttribute @Valid SearchEventAdminRequest request) {
        int size = (request.getSize() != null && request.getSize() > 0) ? request.getSize() : 10;
        int from = request.getFrom() != null ? request.getFrom() : 0;
        PageRequest pageRequest = PageRequest.of(from / size, size);
        log.debug("Controller: getEventAdmin filters={}", request);
        return eventService.getEventsAdmin(request, pageRequest);
    }

    @PatchMapping("/{eventId}")
    public EventDto updateEventAdmin(@PathVariable @Positive Long eventId,
                                     @RequestBody @Valid UpdateEventAdminRequest request
    ) {
        log.debug("Controller: updateEventAdmin eventId={}, data={}", eventId, request);
        return eventService.updateEventAdmin(eventId, request);
    }
}
