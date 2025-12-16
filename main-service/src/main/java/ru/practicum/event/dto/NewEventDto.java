package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ru.practicum.util.DateTime.DATE_TIME_PATTERN;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(message = "Краткое описание не может быть пустым")
    @Size(min = 20, max = 2000, message = "Аннотация должна содержать от {min} до {max} символов")
    private String annotation;

    @NotNull(message = "Id категории не может быть null")
    private Long category;

    @NotBlank(message = "Описание события не может быть пустым")
    @Size(min = 20, max = 7000, message = "Описание должно содержать от {min} до {max} символов")
    private String description;

    @NotNull(message = "Дата события не может быть null")
    @Future(message = "Дата события должна быть в будущем")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_PATTERN)
    private LocalDateTime eventDate;

    @NotNull(message = "Место проведения события не может быть null")
    private LocationDto location;

    private Boolean paid = false;

    @PositiveOrZero(message = "Лимит участников события должен быть нулевым или больше нуля")
    private Integer participantLimit;

    private Boolean requestModeration;

    @NotBlank(message = "Название события не может быть пустым")
    @Size(min = 3, max = 120, message = "Заголовок должен содержать от {min} до {max} символов")
    private String title;


}