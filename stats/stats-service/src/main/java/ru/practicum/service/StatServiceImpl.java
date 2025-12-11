package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.exception.WrongTimeException;
import ru.practicum.mapper.StatMapper;
import ru.practicum.model.Stat;
import ru.practicum.repository.StatServiceRepository;


import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class StatServiceImpl implements StatService {

    private final StatServiceRepository statServiceRepository;

    @Transactional
    @Override
    public StatDto createStat(StatDto statDto) {
        log.info("createStat - invoked");
        Stat stat = statServiceRepository.save(StatMapper.toStat(statDto));
        log.info("createStat - stat saved successfully - {}", stat);
        return StatMapper.toStatDto(stat);
    }

    @Transactional(readOnly = true)
    @Override
    public List<StatResponseDto> readStat(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) throw new WrongTimeException("Start date cannot be after end date");

        List<Stat> stats = (uris == null || uris.isEmpty())
                ? statServiceRepository.findAllByTimestampBetweenOrderByUriAsc(start, end)
                : statServiceRepository.findAllByTimestampBetweenAndUriInOrderByUriAsc(start, end, uris);

        return StatMapper.toStatResponseDto(stats, unique);
    }

}
