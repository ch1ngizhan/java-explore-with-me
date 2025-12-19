package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.StatsRequestDto;
import ru.practicum.mapper.StatMapper;
import ru.practicum.model.Stat;
import ru.practicum.repository.StatServiceRepository;

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

    @Override
    public List<StatResponseDto> readStat(StatsRequestDto request) {
        List<String> uris = (request.getUris() == null || request.getUris().isEmpty())
                ? null
                : request.getUris();

        List<StatResponseDto> result = request.getUnique()
                ? statServiceRepository.getUniqueStats(request.getStart(), request.getEnd(), uris)
                : statServiceRepository.getStats(request.getStart(), request.getEnd(), uris);

        log.info("Размер полученного списка статистики: {}", result.size());
        return result;
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