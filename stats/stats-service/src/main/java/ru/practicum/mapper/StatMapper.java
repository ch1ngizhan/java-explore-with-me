package ru.practicum.mapper;

import ru.practicum.StatDto;
import ru.practicum.StatResponseDto;
import ru.practicum.model.Stat;


public class StatMapper {
    private StatMapper() {
    }

    public static Stat toStat(StatDto statDto) {
        return Stat.builder()
                .app(statDto.getApp())
                .uri(statDto.getUri())
                .ip(statDto.getIp())
                .timestamp(statDto.getTimestamp())
                .build();
    }

    public static StatDto toStatDto(Stat stat) {
        return StatDto.builder()
                .app(stat.getApp())
                .uri(stat.getUri())
                .ip(stat.getIp())
                .timestamp(stat.getTimestamp())
                .build();
    }


    public static StatResponseDto toStatResponseDto(String app, String uri, long hits) {
        return new StatResponseDto(app, uri, hits);
    }
}