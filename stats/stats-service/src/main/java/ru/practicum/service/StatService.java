package ru.practicum.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.StatsRequestDto;

import java.util.List;

public interface StatService {
    @Transactional
    StatDto createStat(StatDto statDto);

    @Transactional(readOnly = true)
    List<StatResponseDto> readStat(StatsRequestDto request);
}
