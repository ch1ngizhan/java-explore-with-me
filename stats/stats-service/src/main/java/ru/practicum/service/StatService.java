package ru.practicum.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatService {
    @Transactional
    StatDto createStat(StatDto statDto);

    @Transactional(readOnly = true)
    List<StatResponseDto> readStat(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
