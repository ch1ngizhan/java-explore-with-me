package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatClient;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.service.CategoryService;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.*;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.SearchEventSpecifications;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserService userService;
    private final RequestRepository requestRepository;
    private final StatClient statsClient;
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public EventDto createEvent(Long userId, NewEventDto newEventDto) {
        validateDateEvent(newEventDto.getEventDate(), 2);

        User user = userService.getUserById(userId);

        CategoryDto categoryDto = categoryService.getCategoryById(newEventDto.getCategory());
        Category category = categoryMapper.toEntityFromDto(categoryDto);

        if (newEventDto.getPaid() == null) {
            newEventDto.setPaid(false);
        }
        if (newEventDto.getParticipantLimit() == null) {
            newEventDto.setParticipantLimit(0);
        }
        if (newEventDto.getRequestModeration() == null) {
            newEventDto.setRequestModeration(true);
        }

        Event event = eventMapper.fromNewEvent(newEventDto, user, category, EventState.PENDING);
        event = eventRepository.save(event);

        log.info("Создано событие с id={}, title={}, initiatorId={}", event.getId(), event.getTitle(), userId);


        EventDto dto = eventMapper.toEventDto(event);
        dto.setConfirmedRequests(0L);
        dto.setViews(0L);
        return dto;
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        userService.getUserById(userId);
        List<Event> events = eventRepository.findAllByInitiatorIdOrderByCreatedOnDesc(userId, pageable);

        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        EventStatistics stats = getEventStatistics(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(stats.getConfirmedRequests().getOrDefault(event.getId(), 0).longValue());
                    dto.setViews(stats.getViews().getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    @Override
    public EventDto getEvent(Long userId, Long eventId, String ip) {
        userService.getUserByIdOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие c id " + eventId + " не найдено у пользователя с id " + userId);
        }

        saveHit("/events/" + eventId, ip);

        return buildDto(event);
    }

    @Override
    @Transactional
    public EventDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        userService.getUserByIdOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId))
            throw new ValidationException("Пользователь не является инициатором события и не может его редактировать");

        if (event.getState() == EventState.PUBLISHED)
            throw new ConflictException("Изменять можно только не опубликованные события");

        validateDateEvent(request.getEventDate(), 2);

        Category category = null;
        if (request.getCategory() != null) {
            category = categoryService.getCategoryByIdOrThrow(request.getCategory());
        }
        eventMapper.updateEventFromUserRequest(request, event, category);

        if (request.getStateAction() != null) {
            if (request.getStateAction() == UserEventStatus.SEND_TO_REVIEW)
                event.pending();
            if (request.getStateAction() == UserEventStatus.CANCEL_REVIEW)
                event.canceled();
        }

        Event updatedEvent = eventRepository.save(event);
        return buildDto(updatedEvent);
    }

    @Override
    public Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));
    }

    @Override
    public List<EventDto> getEventsAdmin(SearchEventAdminRequest request, Pageable pageable) {
        validateRangeStartAndEnd(request.getRangeStart(), request.getRangeEnd());

        Specification<Event> specification = SearchEventSpecifications.addWhereNull();
        if (request.getUsers() != null && !request.getUsers().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereUsers(request.getUsers()));
        if (request.getStates() != null && !request.getStates().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereStates(request.getStates()));
        if (request.getCategories() != null && !request.getCategories().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereCategories(request.getCategories()));
        if (request.getRangeStart() != null)
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(request.getRangeStart()));
        if (request.getRangeEnd() != null)
            specification = specification.and(SearchEventSpecifications.addWhereEndsAfter(request.getRangeEnd()));
        if (request.getRangeStart() == null && request.getRangeEnd() == null)
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(LocalDateTime.now()));

        Page<Long> eventIdsPage = eventRepository.findAll(specification, pageable).map(Event::getId);
        List<Long> eventIds = eventIdsPage.getContent();

        if (eventIds.isEmpty()) return List.of();
        List<Event> events = eventRepository.findAllByEventIds(eventIds);

        if (events.isEmpty()) return List.of();

        EventStatistics stats = getEventStatistics(eventIds);

        return events.stream()
                .map(event -> {
                    EventDto dto = eventMapper.toEventDto(event);
                    dto.setConfirmedRequests(stats.getConfirmedRequests().getOrDefault(event.getId(), 0).longValue());
                    dto.setViews(stats.getViews().getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public EventDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findByIdNew(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));

        if (request.getStateAction() != null) {
            AdminEventStatus adminEventStatus = request.getStateAction();
            EventState currentState = event.getState();

            if (adminEventStatus == AdminEventStatus.PUBLISH_EVENT) {
                if (currentState != EventState.PENDING)
                    throw new ConflictException("Событие можно опубликовать только если оно в состоянии ожидания публикации");
                validateDateEvent(event.getEventDate(), 1);
                event.publish();
            }
            if (adminEventStatus == AdminEventStatus.REJECT_EVENT) {
                if (currentState == EventState.PUBLISHED)
                    throw new ConflictException("Событие можно отклонить пока оно не опубликовано");
                event.canceled();
            }
        }

        Category category = null;
        if (request.getCategory() != null) {
            category = categoryService.getCategoryByIdOrThrow(request.getCategory());
        }

        eventMapper.updateEventFromAdminRequest(request, event, category);
        Event updatedEvent = eventRepository.save(event);

        log.info("Обновлено событие с id={}, title={}, initiatorId={}",
                updatedEvent.getId(), updatedEvent.getTitle(), updatedEvent.getInitiator().getId());
        return buildDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> getEventsPublic(SearchEventPublicRequest request, Pageable pageable, String ip) {
        validateRangeStartAndEnd(request.getRangeStart(), request.getRangeEnd());

        Specification<Event> specification = SearchEventSpecifications.addWhereNull();
        if (request.getText() != null && !request.getText().trim().isEmpty())
            specification = specification.and(SearchEventSpecifications.addLikeText(request.getText()));
        if (request.getCategories() != null && !request.getCategories().isEmpty())
            specification = specification.and(SearchEventSpecifications.addWhereCategories(request.getCategories()));
        if (request.getPaid() != null)
            specification = specification.and(SearchEventSpecifications.isPaid(request.getPaid()));
        LocalDateTime rangeStart = (request.getRangeStart() == null && request.getRangeEnd() == null) ?
                LocalDateTime.now() : request.getRangeStart();
        if (rangeStart != null)
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(rangeStart));
        if (request.getRangeEnd() != null)
            specification = specification.and(SearchEventSpecifications.addWhereEndsAfter(request.getRangeEnd()));
        if (request.getOnlyAvailable())
            specification = specification.and(SearchEventSpecifications.addWhereAvailableSlots());

        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        EventStatistics stats = getEventStatistics(eventIds);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(stats.getConfirmedRequests().getOrDefault(event.getId(), 0).longValue());
                    dto.setViews(stats.getViews().getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .toList();

        saveHit("/events", ip);

        if (SortState.VIEWS.equals(request.getSort())) {
            return result.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                    .toList();
        } else if (SortState.EVENT_DATE.equals(request.getSort())) {
            return result.stream()
                    .sorted(Comparator.comparing(EventShortDto::getEventDate))
                    .toList();
        }

        return result;
    }

    @Override
    @Transactional
    public EventDto getEventByIdPublic(Long eventId, String ip) {
        Event event = eventRepository.findByIdNew(eventId)
                .filter(ev -> ev.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));

        // Сохраняем хит (отправляем в сервис статистики)
        saveHit("/events/" + eventId, ip);

        // Получаем статистику с unique=true (уникальные просмотры)
        Map<Long, Long> viewsFromStats = getViewsForEvents(List.of(eventId), true);
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(List.of(eventId));

        EventDto dto = eventMapper.toEventDto(event);
        dto.setViews(viewsFromStats.getOrDefault(eventId, 0L));
        dto.setConfirmedRequests(confirmedRequests.getOrDefault(eventId, 0).longValue());

        log.debug("Returning event with id={}, uniqueViews={}", eventId, dto.getViews());

        return dto;
    }

    private EventDto buildDto(Event event) {
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(List.of(event.getId()));
        // Для индивидуального события используем уникальные просмотры
        Map<Long, Long> views = getViewsForEvents(List.of(event.getId()), true);

        EventDto dto = eventMapper.toEventDto(event);
        dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0).longValue());
        dto.setViews(views.getOrDefault(event.getId(), 0L));

        return dto;
    }

    private void validateRangeStartAndEnd(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd))
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
    }

    private void saveHit(String path, String ip) {
        StatDto hit = new StatDto(
                "ewm-main-service",
                path,
                ip,
                LocalDateTime.now()
        );
        statsClient.addStatEvent(hit);
    }


    private void validateDateEvent(LocalDateTime eventDate, long minHoursBeforeStartEvent) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(minHoursBeforeStartEvent)))
            throw new ValidationException("Дата начала события не может быть ранее чем через " + minHoursBeforeStartEvent + " часа(ов)");
    }

    private Map<Long, Integer> getConfirmedRequests(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<EventWithCountConfirmedRequests> events = requestRepository.findCountConfirmedRequestsByEventIds(eventIds);
        Map<Long, Integer> confirmedRequests = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0));

        events.forEach(dto -> confirmedRequests.put(dto.getEventId(), dto.getCountConfirmedRequests()));

        return confirmedRequests;
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds, boolean unique) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        try {
            // Используем unique=true для уникальных просмотров
            List<StatResponseDto> stats = statsClient.getStats(start, end, uris, unique);

            Map<Long, Long> result = eventIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> 0L));

            for (StatResponseDto stat : stats) {
                try {
                    Long eventId = Long.parseLong(stat.getUri().substring("/events/".length()));
                    result.put(eventId, stat.getHits());
                } catch (NumberFormatException e) {
                    log.warn("Невозможно извлечь ID события из URI: {}", stat.getUri());
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("Ошибка при получении статистики: {}", e.getMessage());
            // В тестовой среде возвращаем 0
            return eventIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> 0L));
        }
    }

    private Long getEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.substring("/events".length() + 1));
        } catch (Exception e) {
            return -1L;
        }
    }
    private Map<Long, Long> getAllViewsForEvents(List<Long> eventIds) {
        return getViewsForEvents(eventIds, false);
    }
    private EventStatistics getEventStatistics(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return new EventStatistics(Map.of(), Map.of());
        }

        Map<Long, Integer> confirmedRequests = getConfirmedRequests(eventIds);
        // Для событий в списках используем уникальные просмотры (unique=true)
        Map<Long, Long> views = getViewsForEvents(eventIds, true);

        return new EventStatistics(confirmedRequests, views);
    }
}