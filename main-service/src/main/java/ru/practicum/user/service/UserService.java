package ru.practicum.user.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequestDto;
import ru.practicum.user.model.User;

import java.util.List;

public interface UserService {
    UserDto createUser(UserRequestDto userRequest);

    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long userId);

    User getUserById(Long userId);

    User getUserByIdOrThrow(Long userId);
}
