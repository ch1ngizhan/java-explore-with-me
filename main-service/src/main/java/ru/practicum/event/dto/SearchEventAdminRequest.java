package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.util.DateTime.DATE_TIME_PATTERN;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchEventAdminRequest {
    private List<Long> users;
    private List<EventState> states;
    private List<Long> categories;

    @DateTimeFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime rangeEnd;

    private Integer from;
    private Integer size;
}