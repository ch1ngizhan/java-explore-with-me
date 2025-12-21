package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.event.model.SortState;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.util.DateTime.DATE_TIME_PATTERN;


@Data
@Builder(builderClassName = "Builder", toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SearchEventPublicRequest {
    private String text;
    private List<Long> categories;
    private Boolean paid;

    @DateTimeFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = DATE_TIME_PATTERN)
    private LocalDateTime rangeEnd;

    private Boolean onlyAvailable;
    private SortState sort;
    private Integer from;
    private Integer size;

    public Boolean getOnlyAvailable() {
        return onlyAvailable != null ? onlyAvailable : false;
    }

    public Integer getFrom() {
        return from != null ? from : 0;
    }

    public Integer getSize() {
        return size != null ? size : 10;
    }

}