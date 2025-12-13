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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    @Override
    public List<StatResponseDto> readStat(LocalDateTime start, LocalDateTime end,
                                          List<String> uris, boolean unique) {
        if (start == null || end == null) {
            throw new WrongTimeException("Start and end dates cannot be null");
        }
        if (start.isAfter(end)) {
            throw new WrongTimeException("Start date cannot be after end date");
        }

        List<Stat> stats;
        if (uris == null || uris.isEmpty()) {
            stats = statServiceRepository.findAllByTimestampBetween(start, end);
            log.info("Found {} stats without URI filter", stats.size());
        } else {
            stats = statServiceRepository.findAllByTimestampBetweenAndUriIn(start, end, uris);
            log.info("Found {} stats with URI filter: {}", stats.size(), uris);
        }

        return convertToResponseDto(stats, unique);
    }

    private List<StatResponseDto> convertToResponseDto(List<Stat> stats, boolean unique) {
        if (stats.isEmpty()) {
            return new ArrayList<>();
        }

        // Группируем по app и uri
        Map<String, Map<String, List<Stat>>> grouped = stats.stream()
                .collect(Collectors.groupingBy(Stat::getApp,
                        Collectors.groupingBy(Stat::getUri)));

        List<StatResponseDto> result = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<Stat>>> appEntry : grouped.entrySet()) {
            String app = appEntry.getKey();
            Map<String, List<Stat>> uriMap = appEntry.getValue();

            for (Map.Entry<String, List<Stat>> uriEntry : uriMap.entrySet()) {
                String uri = uriEntry.getKey();
                List<Stat> uriStats = uriEntry.getValue();

                long hits;
                if (unique) {
                    hits = uriStats.stream()
                            .map(Stat::getIp)
                            .distinct()
                            .count();
                } else {
                    hits = uriStats.size();
                }

                result.add(new StatResponseDto(app, uri, hits));
            }
        }

        // Сортировка по hits по убыванию
        result.sort((a, b) -> Long.compare(b.getHits(), a.getHits()));

        log.info("Converted to {} response DTOs", result.size());
        return result;
    }
}