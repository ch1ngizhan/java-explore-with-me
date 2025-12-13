package ru.practicum.user.mapper;

import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequestDto;
import ru.practicum.user.model.User;

public class UserMapper {

    public static User toEntity(UserRequestDto userRequest) {
        if (userRequest == null) {
            return null;
        }

        User user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        return user;
    }

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(Math.toIntExact(user.getId()))
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static void updateEntity(User user, UserRequestDto userRequest) {
        if (user == null || userRequest == null) {
            return;
        }

        if (userRequest.getName() != null) {
            user.setName(userRequest.getName());
        }
        if (userRequest.getEmail() != null) {
            user.setEmail(userRequest.getEmail());
        }
    }
}