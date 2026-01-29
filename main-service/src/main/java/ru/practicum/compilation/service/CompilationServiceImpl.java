package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        log.debug("Создание подборки: {}", request);

        if (compilationRepository.existsByTitle(request.getTitle())) {
            log.warn("Попытка создать подборку с дублирующимся названием: {}", request.getTitle());
            throw new ConflictException("Подборка с таким названием (" + request.getTitle() + ") уже существует");
        }

        Compilation compilation = compilationMapper.toEntity(request);

        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllByEventIds(new ArrayList<>(request.getEvents()));

            if (events.size() != request.getEvents().size()) {
                log.warn("Не все события найдены для подборки: requested={}, found={}",
                        request.getEvents().size(), events.size());
                throw new NotFoundException("Не все события найдены");
            }

            compilation.setEvents(new HashSet<>(events));
        } else {
            compilation.setEvents(new HashSet<>());
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Подборка создана: id={}, title={}", savedCompilation.getId(), savedCompilation.getTitle());

        return compilationMapper.toDto(savedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.debug("Удаление подборки с id={}", compId);

        if (!compilationRepository.existsById(compId)) {
            log.warn("Попытка удалить несуществующую подборку id={}", compId);
            throw new NotFoundException(String.format("Подборка с идентификатором %d не найдена", compId));
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка удалена: id={}", compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.debug("Обновление подборки id={}, данные={}", compId, request);

        Compilation compilation = getCompilationOrThrow(compId);

        if (request.getTitle() != null
                && !request.getTitle().equals(compilation.getTitle())
                && compilationRepository.existsByTitle(request.getTitle())) {
            log.warn("Попытка обновить подборку с дублирующимся названием: {}", request.getTitle());
            throw new ConflictException(String.format("Подборка с названием \"%s\" уже существует", request.getTitle()));
        }

        if (request.getEvents() != null) {
            if (request.getEvents().isEmpty()) {
                compilation.setEvents(new HashSet<>());
            } else {
                List<Event> events = eventRepository.findAllByEventIds(new ArrayList<>(request.getEvents()));
                if (events.size() != request.getEvents().size()) {
                    log.warn("Некоторые события не найдены для подборки id={}", compId);
                    throw new NotFoundException("Некоторые события не найдены");
                }
                compilation.setEvents(new HashSet<>(events));
            }
        }

        compilationMapper.updateCompilationFromRequest(request, compilation);
        Compilation updated = compilationRepository.save(compilation);

        log.info("Подборка обновлена: id={}, title={}", updated.getId(), updated.getTitle());
        return compilationMapper.toDto(updated);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable) {
        log.debug("Получение подборок pinned={}, pageable={}", pinned, pageable);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("id").ascending()
        );

        List<Long> ids = compilationRepository.findIdsByPinned(pinned, sortedPageable);

        if (ids == null || ids.isEmpty()) {
            log.info("Подборки не найдены pinned={}, pageable={}", pinned, pageable);
            return List.of();
        }

        List<Compilation> compilationsWithEvents = compilationRepository.findAllByIdInWithEvents(ids);

        Map<Long, Compilation> byId = compilationsWithEvents.stream()
                .collect(Collectors.toMap(Compilation::getId, Function.identity()));

        List<Compilation> ordered = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();

        log.info("Получен список подборок: count={}, pinned={}", ordered.size(), pinned);
        return ordered.stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.debug("Получение подборки по id={}", compId);

        Compilation compilation = getCompilationOrThrow(compId);
        log.info("Подборка получена: id={}, title={}", compilation.getId(), compilation.getTitle());

        return compilationMapper.toDto(compilation);
    }

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> {
                    log.warn("Подборка не найдена id={}", compId);
                    return new NotFoundException(String.format("Подборка с идентификатором %d не найдена", compId));
                });
    }
}
