package ru.practicum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        LocalDateTime startDate, endDate;
        try {
            startDate = LocalDateTime.parse(start, formatter1);
            endDate = LocalDateTime.parse(end, formatter1);
        } catch (DateTimeParseException e) {
            startDate = LocalDateTime.parse(start, formatter2);
            endDate = LocalDateTime.parse(end, formatter2);
        }

        return new StatsRequestDto(startDate, endDate, uris, unique);
    }
}
