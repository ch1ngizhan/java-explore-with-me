package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.StatsRequestDto;
import ru.practicum.exception.WrongTimeException;
import ru.practicum.mapper.StatMapper;
import ru.practicum.model.Stat;
import ru.practicum.repository.StatServiceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatServiceImpl implements StatService {

    private final StatServiceRepository statServiceRepository;

    @Transactional
    @Override
    public StatDto createStat(StatDto statDto) {
        log.info("createStat - invoked with: {}", statDto);
        Stat stat = statServiceRepository.save(StatMapper.toStat(statDto));
        log.info("createStat - stat saved successfully with id: {}", stat.getStatId());
        return StatMapper.toStatDto(stat);
    }

    @Override
    public List<StatResponseDto> readStat(StatsRequestDto request) {
        // Дополнительная валидация (для случая, если объект создан не через метод of())
        if (request.getStart().isAfter(request.getEnd())) {
            throw new WrongTimeException("Start date must be before end date");
        }

        List<String> uris = (request.getUris() == null || request.getUris().isEmpty())
                ? null
                : request.getUris();

        List<StatResponseDto> result = request.getUnique()
                ? statServiceRepository.getUniqueStats(request.getStart(), request.getEnd(), uris)
                : statServiceRepository.getStats(request.getStart(), request.getEnd(), uris);

        log.info("Размер полученного списка статистики: {}", result.size());
        return result;
    }
}