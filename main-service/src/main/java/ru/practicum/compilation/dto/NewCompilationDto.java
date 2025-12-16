package ru.practicum.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewCompilationDto {
    private Set<Long> events;
    private boolean pinned;

    @NotBlank(message = "Заголовок подборки не может быть пустым")
    @Size(min = 1, max = 50, message = "Заголовок должен содержать от {min} до {max} символов")
    private String title;
}