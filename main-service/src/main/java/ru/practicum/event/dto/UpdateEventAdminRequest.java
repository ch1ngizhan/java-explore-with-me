package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.event.model.AdminEventStatus;

import java.time.LocalDateTime;

import static ru.practicum.util.DateTime.DATE_TIME_PATTERN;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventAdminRequest {
    @Size(min = 20, max = 2000, message = "Недопустимое количество символов")
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000, message = "Описание должно содержать от {min} до {max} символов")
    private String description;

    @Future(message = "Дата события должна быть в будущем")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    private LocationDto location;

    private Boolean paid;

    @PositiveOrZero(message = "Лимит участников события должен быть нулевым или больше нуля")
    private Integer participantLimit;

    private Boolean requestModeration;

    private AdminEventStatus stateAction;

    @Size(min = 3, max = 120, message = "Заголовок должен содержать от {min} до {max} символов")
    private String title;
}