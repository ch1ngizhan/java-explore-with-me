package ru.practicum.mapper;

import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.model.Stat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class StatMapper {
    private StatMapper() {
    }

    public static Stat toStat(StatDto statDto) {

        return Stat.builder()
                .ip(statDto.getIp())
                .uri(statDto.getUri())
                .timestamp(statDto.getTimestamp())
                .app(statDto.getApp())
                .build();
    }

    public static StatDto toStatDto(Stat stat) {
        return StatDto.builder()
                .timestamp(stat.getTimestamp())
                .app(stat.getApp())
                .uri(stat.getUri())
                .ip(stat.getIp())
                .build();
    }

    // Маппим список Stat в StatResponseDto с подсчетом hits
    public static List<StatResponseDto> toStatResponseDto(List<Stat> stats, boolean unique) {
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
                long hits = unique
                        ? uriEntry.getValue().stream().map(Stat::getIp).distinct().count()
                        : uriEntry.getValue().size();
                result.add(new StatResponseDto(app, uri, hits));
            }
        }

        // Сортировка по hits по убыванию
        result.sort((a, b) -> Long.compare(b.getHits(), a.getHits()));

        return result;
    }
}


