package ru.practicum.event.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;

import java.util.List;

public interface EventService {
    List<EventShortDto> getEvents(Long userId, Pageable pageable);

    EventDto createEvent(Long userId, NewEventDto newEventDto);

    EventDto getEvent(Long userId, Long eventId, String ip);

    EventDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    Event getEventOrThrow(Long eventId);

    List<EventDto> getEventsAdmin(SearchEventAdminRequest request, Pageable pageable);

    EventDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getEventsPublic(SearchEventPublicRequest requestParams, Pageable pageable, String ip);

    EventDto getEventByIdPublic(Long eventId, String ip);
}
