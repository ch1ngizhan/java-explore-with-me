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
import java.util.concurrent.CompletableFuture;
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
        log.debug("Начало создания события: userId={}, newEventDto={}", userId, newEventDto);

        validateDateEvent(newEventDto.getEventDate(), 2);
        log.debug("Дата события валидирована: eventDate={}", newEventDto.getEventDate());

        User user = userService.getUserById(userId);
        log.debug("Получен пользователь: userId={}, username={}", user.getId(), user.getName());

        CategoryDto categoryDto = categoryService.getCategoryById(newEventDto.getCategory());
        Category category = categoryMapper.toEntityFromDto(categoryDto);
        log.debug("Категория получена: categoryId={}, name={}", category.getId(), category.getName());

        if (newEventDto.getPaid() == null) newEventDto.setPaid(false);
        if (newEventDto.getParticipantLimit() == null) newEventDto.setParticipantLimit(0);
        if (newEventDto.getRequestModeration() == null) newEventDto.setRequestModeration(true);

        Event event = eventMapper.fromNewEvent(newEventDto, user, category, EventState.PENDING);
        event = eventRepository.save(event);

        log.info("Событие успешно создано: id={}, title={}, initiatorId={}", event.getId(), event.getTitle(), userId);

        EventDto dto = eventMapper.toEventDto(event, 0, 0L);
        log.debug("EventDto сформирован: {}", dto);
        return dto;
    }


    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        log.debug("Получение событий пользователя: userId={}, pageable={}", userId, pageable);

        userService.getUserById(userId);
        log.debug("Пользователь найден: userId={}", userId);

        List<Event> events = eventRepository.findAllByInitiatorIdOrderByCreatedOnDesc(userId, pageable);

        if (events.isEmpty()) {
            log.info("События не найдены для пользователя userId={}", userId);
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        log.debug("Найденные события userId={}, eventIds={}", userId, eventIds);

        EventStatistics stats = getEventStatistics(eventIds);
        log.debug("Статистика подтвержденных запросов и просмотров получена для eventIds={}", eventIds);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(stats.getConfirmedRequests().getOrDefault(event.getId(), 0).longValue());
                    dto.setViews(stats.getViews().getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .toList();

        log.info("Возвращено {} событий для userId={}", result.size(), userId);
        return result;
    }


    @Override
    public EventDto getEvent(Long userId, Long eventId, String ip) {
        log.debug("Запрос события: userId={}, eventId={}, ip={}", userId, eventId, ip);

        userService.getUserByIdOrThrow(userId);
        log.debug("Пользователь найден: userId={}", userId);

        Event event = getEventOrThrow(eventId);
        log.debug("Событие найдено: eventId={}, title={}, initiatorId={}", event.getId(), event.getTitle(), event.getInitiator().getId());

        if (!event.getInitiator().getId().equals(userId)) {
            log.warn("Событие с id={} не принадлежит пользователю с id={}", eventId, userId);
            throw new NotFoundException("Событие c id " + eventId + " не найдено у пользователя с id " + userId);
        }

        saveHit("/events/" + eventId, ip);
        log.debug("Хит сохранен: path=/events/{}, ip={}", eventId, ip);

        EventDto dto = buildDto(event);
        log.debug("Сформирован EventDto: {}", dto);
        return dto;
    }


    @Override
    @Transactional
    public EventDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.debug("Запрос на обновление события: userId={}, eventId={}, request={}", userId, eventId, request);

        userService.getUserByIdOrThrow(userId);
        log.debug("Пользователь найден: userId={}", userId);

        Event event = getEventOrThrow(eventId);
        log.debug("Событие найдено: eventId={}, title={}, state={}", event.getId(), event.getTitle(), event.getState());

        if (!event.getInitiator().getId().equals(userId)) {
            log.warn("Пользователь с id={} не является инициатором события eventId={}", userId, eventId);
            throw new ValidationException("Пользователь не является инициатором события и не может его редактировать");
        }

        if (event.getState() == EventState.PUBLISHED) {
            log.warn("Попытка редактирования опубликованного события eventId={}", eventId);
            throw new ConflictException("Изменять можно только не опубликованные события");
        }

        validateDateEvent(request.getEventDate(), 2);
        log.debug("Дата события валидирована: eventDate={}", request.getEventDate());

        Category category = null;
        if (request.getCategory() != null) {
            category = categoryService.getCategoryByIdOrThrow(request.getCategory());
            log.debug("Категория обновлена: categoryId={}, name={}", category.getId(), category.getName());
        }

        eventMapper.updateEventFromUserRequest(request, event, category);
        log.debug("Событие обновлено с учетом запроса пользователя");

        if (request.getStateAction() != null) {
            if (request.getStateAction() == UserEventStatus.SEND_TO_REVIEW) {
                event.pending();
                log.debug("Событие переведено в состояние PENDING");
            }
            if (request.getStateAction() == UserEventStatus.CANCEL_REVIEW) {
                event.canceled();
                log.debug("Событие переведено в состояние CANCELED");
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие успешно обновлено: eventId={}, title={}, initiatorId={}",
                updatedEvent.getId(), updatedEvent.getTitle(), updatedEvent.getInitiator().getId());

        EventDto dto = buildDto(updatedEvent);
        log.debug("Сформирован EventDto после обновления: {}", dto);
        return dto;
    }


    @Override
    public Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));
    }

    @Override
    public List<EventDto> getEventsAdmin(SearchEventAdminRequest request, Pageable pageable) {
        log.debug("Админ запрос событий: request={}, pageable={}", request, pageable);

        validateRangeStartAndEnd(request.getRangeStart(), request.getRangeEnd());
        log.debug("Диапазон дат валидирован: rangeStart={}, rangeEnd={}", request.getRangeStart(), request.getRangeEnd());

        Specification<Event> specification = SearchEventSpecifications.addWhereNull();

        if (request.getUsers() != null && !request.getUsers().isEmpty()) {
            specification = specification.and(SearchEventSpecifications.addWhereUsers(request.getUsers()));
            log.debug("Фильтр по пользователям: {}", request.getUsers());
        }
        if (request.getStates() != null && !request.getStates().isEmpty()) {
            specification = specification.and(SearchEventSpecifications.addWhereStates(request.getStates()));
            log.debug("Фильтр по состояниям: {}", request.getStates());
        }
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            specification = specification.and(SearchEventSpecifications.addWhereCategories(request.getCategories()));
            log.debug("Фильтр по категориям: {}", request.getCategories());
        }
        if (request.getRangeStart() != null) {
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(request.getRangeStart()));
        }
        if (request.getRangeEnd() != null) {
            specification = specification.and(SearchEventSpecifications.addWhereEndsAfter(request.getRangeEnd()));
        }
        if (request.getRangeStart() == null && request.getRangeEnd() == null) {
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(LocalDateTime.now()));
        }

        Page<Long> eventIdsPage = eventRepository.findAll(specification, pageable).map(Event::getId);
        List<Long> eventIds = eventIdsPage.getContent();
        log.debug("Найдено eventIds={}", eventIds);

        if (eventIds.isEmpty()) {
            log.info("События не найдены по фильтрам admin request");
            return List.of();
        }

        List<Event> events = eventRepository.findAllByEventIds(eventIds);
        if (events.isEmpty()) {
            log.info("События не найдены в репозитории по eventIds={}", eventIds);
            return List.of();
        }

        EventStatistics stats = getEventStatistics(eventIds);
        log.debug("Статистика подтвержденных запросов и просмотров получена для eventIds={}", eventIds);

        List<EventDto> result = events.stream()
                .map(event -> eventMapper.toEventDto(
                        event,
                        stats.getConfirmedRequests().getOrDefault(event.getId(), 0),
                        stats.getViews().getOrDefault(event.getId(), 0L)
                ))
                .toList();

        log.info("Возвращено {} событий для админа", result.size());
        return result;
    }


    @Override
    @Transactional
    public EventDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.debug("Админ обновление события: eventId={}, request={}", eventId, request);

        Event event = eventRepository.findByIdNew(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} не найдено", eventId);
                    return new NotFoundException("Событие c id " + eventId + " не найдено");
                });

        if (request.getStateAction() != null) {
            AdminEventStatus adminEventStatus = request.getStateAction();
            EventState currentState = event.getState();
            log.debug("Текущее состояние события: {}", currentState);

            if (adminEventStatus == AdminEventStatus.PUBLISH_EVENT) {
                if (currentState != EventState.PENDING) {
                    log.warn("Невозможно опубликовать событие с текущим состоянием: {}", currentState);
                    throw new ConflictException("Событие можно опубликовать только если оно в состоянии ожидания публикации");
                }
                validateDateEvent(event.getEventDate(), 1);
                event.publish();
                log.info("Событие с id={} опубликовано", eventId);
            }

            if (adminEventStatus == AdminEventStatus.REJECT_EVENT) {
                if (currentState == EventState.PUBLISHED) {
                    log.warn("Невозможно отклонить опубликованное событие");
                    throw new ConflictException("Событие можно отклонить пока оно не опубликовано");
                }
                event.canceled();
                log.info("Событие с id={} отклонено", eventId);
            }
        }

        Category category = null;
        if (request.getCategory() != null) {
            category = categoryService.getCategoryByIdOrThrow(request.getCategory());
            log.debug("Категория события обновлена: {}", category.getId());
        }

        eventMapper.updateEventFromAdminRequest(request, event, category);
        Event updatedEvent = eventRepository.save(event);

        log.info("Событие обновлено: id={}, title={}, initiatorId={}",
                updatedEvent.getId(), updatedEvent.getTitle(), updatedEvent.getInitiator().getId());

        return buildDto(updatedEvent);
    }


    @Override
    public List<EventShortDto> getEventsPublic(SearchEventPublicRequest request, Pageable pageable, String ip) {
        log.debug("Публичный поиск событий: request={}, pageable={}, ip={}", request, pageable, ip);
        validateRangeStartAndEnd(request.getRangeStart(), request.getRangeEnd());

        Specification<Event> specification = SearchEventSpecifications.addWhereNull();

        if (request.getText() != null && !request.getText().trim().isEmpty()) {
            specification = specification.and(SearchEventSpecifications.addLikeText(request.getText()));
            log.debug("Фильтр по тексту добавлен: {}", request.getText());
        }

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            specification = specification.and(SearchEventSpecifications.addWhereCategories(request.getCategories()));
            log.debug("Фильтр по категориям добавлен: {}", request.getCategories());
        }

        if (request.getPaid() != null) {
            specification = specification.and(SearchEventSpecifications.isPaid(request.getPaid()));
            log.debug("Фильтр по оплате добавлен: {}", request.getPaid());
        }

        LocalDateTime rangeStart = (request.getRangeStart() == null && request.getRangeEnd() == null)
                ? LocalDateTime.now() : request.getRangeStart();
        if (rangeStart != null) {
            specification = specification.and(SearchEventSpecifications.addWhereStartsBefore(rangeStart));
            log.debug("Фильтр по дате начала добавлен: {}", rangeStart);
        }

        if (request.getRangeEnd() != null) {
            specification = specification.and(SearchEventSpecifications.addWhereEndsAfter(request.getRangeEnd()));
            log.debug("Фильтр по дате окончания добавлен: {}", request.getRangeEnd());
        }

        if (request.getOnlyAvailable()) {
            specification = specification.and(SearchEventSpecifications.addWhereAvailableSlots());
            log.debug("Фильтр только доступные события добавлен");
        }

        List<Event> events = eventRepository.findAll(specification, pageable).getContent();
        log.debug("Найдено событий: {}", events.size());

        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        EventStatistics stats = getEventStatistics(eventIds);
        log.debug("Получена статистика для событий: {}", stats);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(stats.getConfirmedRequests().getOrDefault(event.getId(), 0).longValue());
                    dto.setViews(stats.getViews().getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .toList();

        saveHit("/events", ip);
        log.debug("Сохранён хит для IP: {}", ip);

        if (SortState.VIEWS.equals(request.getSort())) {
            log.debug("Сортировка по просмотрам");
            return result.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                    .toList();
        } else if (SortState.EVENT_DATE.equals(request.getSort())) {
            log.debug("Сортировка по дате события");
            return result.stream()
                    .sorted(Comparator.comparing(EventShortDto::getEventDate))
                    .toList();
        }

        log.debug("Возврат результатов без сортировки");
        return result;
    }


    @Override
    public EventDto getEventByIdPublic(Long eventId, String ip) {
        Event event = eventRepository.findByIdNew(eventId)
                .filter(ev -> ev.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие c id " + eventId + " не найдено"));

        saveHit("/events/" + eventId, ip);

        return buildDto(event);
    }

    private EventDto buildDto(Event event) {
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(List.of(event.getId()));
        Map<Long, Long> views = getViewsForEvents(List.of(event.getId()));

        return eventMapper.toEventDto(
                event,
                confirmedRequests.getOrDefault(event.getId(), 0),
                views.getOrDefault(event.getId(), 0L)
        );
    }

    private void validateRangeStartAndEnd(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd))
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
    }

    private void saveHit(String path, String ip) {
        try {
            StatDto hit = new StatDto(
                    "ewm-main-service",
                    path,
                    ip,
                    LocalDateTime.now()
            );
            // Асинхронный вызов
            CompletableFuture.runAsync(() -> statsClient.addStatEvent(hit));
        } catch (Exception e) {
            log.warn("Не удалось сохранить хит для path={}, ip={}: {}", path, ip, e.getMessage());
        }
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

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        LocalDateTime start = eventRepository.findFirstByOrderByCreatedOnAsc().getCreatedOn();
        LocalDateTime end = LocalDateTime.now();

        List<StatResponseDto> stats = statsClient.getStats(start, end, uris, true);

        Map<Long, Long> views = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0L));

        if (stats != null && !stats.isEmpty()) {
            stats.forEach(stat -> {
                Long eventId = getEventIdFromUri(stat.getUri());
                if (eventId > -1L) {
                    views.put(eventId, stat.getHits());
                }
            });
        }
        return views;
    }

    private Long getEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.substring("/events".length() + 1));
        } catch (Exception e) {
            return -1L;
        }
    }

    private EventStatistics getEventStatistics(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return new EventStatistics(Map.of(), Map.of());
        }

        Map<Long, Integer> confirmedRequests = getConfirmedRequests(eventIds);
        Map<Long, Long> views = getViewsForEvents(eventIds);

        return new EventStatistics(confirmedRequests, views);
    }
}