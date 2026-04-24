package ru.practicum.core.user.service;

import ru.practicum.core.common.dto.user.NewUserRequest;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.user.dto.UserParam;

import java.util.List;

public interface UserService {

    List<UserDto> getPage(UserParam userParam);

    UserDto create(NewUserRequest request);

    void deleteById(Long userId);

    UserDto getById(Long userId);

    UserShortDto getShortById(Long userId);

    List<UserShortDto> getShortByIdIn(List<Long> ids);
}