package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.SearchEventPublicRequest;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.BadRequestException;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEventsPublic(@ModelAttribute @Valid SearchEventPublicRequest request,
                                               HttpServletRequest httpRequest) {
        int size = (request.getSize() != null && request.getSize() > 0) ? request.getSize() : 10;
        int from = request.getFrom() != null ? request.getFrom() : 0;


        if (from < 0) {
            throw new BadRequestException("Параметр 'from' не может быть отрицательным");
        }
        if (size <= 0) {
            throw new BadRequestException("Параметр 'size' должен быть положительным");
        }

        PageRequest pageRequest = PageRequest.of(from / size, size);
        log.debug("Controller: getEventsPublic filters={}", request);
        return eventService.getEventsPublic(request, pageRequest, httpRequest.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public EventDto getEventByIdPublic(@PathVariable("id") @Positive Long eventId,
                                       HttpServletRequest httpRequest) {
        log.debug("Controller: getEventByIdPublic eventId={}", eventId);
        return eventService.getEventByIdPublic(eventId, httpRequest.getRemoteAddr());
    }
}