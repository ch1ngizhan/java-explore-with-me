package ru.practicum.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDto {
    @NotBlank(message = "Email пользователя не может быть пустой")
    @Email(message = "Email пользователя введен не верно")
    @Size(min = 6, max = 254)
    private String email;

    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Size(min = 2, max = 250)
    private String name;
}
