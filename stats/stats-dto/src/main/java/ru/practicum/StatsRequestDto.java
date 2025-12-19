package ru.practicum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatsRequestDto {

    private LocalDateTime start;
    private LocalDateTime end;
    private List<String> uris;
    private Boolean unique;

    public static StatsRequestDto of(String start, String end, List<String> uris, Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new StatsRequestDto(
                LocalDateTime.parse(start, formatter),
                LocalDateTime.parse(end, formatter),
                uris,
                unique
        );
    }
}
