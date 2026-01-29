package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequestDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;


    @Override
    @Transactional
    public UserDto createUser(UserRequestDto userRequest) {
        log.debug("Попытка создания нового пользователя с email={}", userRequest.getEmail());

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            log.warn("Попытка создания пользователя с существующим email={}", userRequest.getEmail());
            throw new ConflictException("Пользователь с таким e-mail уже существует: " + userRequest.getEmail());
        }

        User user = UserMapper.toEntity(userRequest);
        user = userRepository.save(user);
        log.info("Добавлен новый пользователь: id={}, email={}", user.getId(), user.getEmail());

        return UserMapper.toDto(user);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Pageable pageable) {
        log.debug("Запрос списка пользователей. ids={}, pageNumber={}, pageSize={}", ids, pageable.getPageNumber(), pageable.getPageSize());

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "id")
        );

        List<User> users = (ids == null || ids.isEmpty())
                ? userRepository.findAllList(sortedPageable)
                : userRepository.findByIdIn(ids, sortedPageable);

        log.info("Найдено {} пользователей", users.size());
        return users.stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("Попытка удаления пользователя с id={}", userId);
        getUserByIdOrThrow(userId);

        userRepository.deleteById(userId);
        log.info("Пользователь с id={} успешно удален", userId);
    }

    @Override
    public User getUserById(Long userId) {
        log.debug("Запрос пользователя по id={}", userId);
        return getUserByIdOrThrow(userId);
    }

    @Override
    public User getUserByIdOrThrow(Long userId) {
        log.debug("Проверка существования пользователя с id={}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", userId);
                    return new NotFoundException(String.format("Пользователь с id=%d не найден", userId));
                });
    }

}
