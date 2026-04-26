package ru.practicum.core.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.core.common.dto.user.NewUserRequest;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.user.dto.UserParam;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.user.mapper.UserMapper;
import ru.practicum.core.user.model.User;
import ru.practicum.core.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> getPage(UserParam userParam) {
        log.debug("Get user page request: {}", userParam);

        if (userParam.hasIds()) {
            return userRepository.findByIdIn(userParam.ids()).stream()
                    .map(UserMapper::toUserDto)
                    .toList();
        } else {
            int page = userParam.from() / userParam.size();
            Pageable pageable = PageRequest.of(page, userParam.size());

            return userRepository.findAll(pageable).get()
                    .map(UserMapper::toUserDto)
                    .toList();
        }
    }

    @Override
    public UserDto create(NewUserRequest request) {
        log.debug("New user request: {}", request);

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("User with email = {} already exists", request.getEmail());
            throw new ConflictException(String.format("User with email = %s already exists", request.getEmail()));
        }

        User user = userRepository.save(UserMapper.toNewUser(request));

        log.info("New user added: {}", user);
        return UserMapper.toUserDto(user);
    }

    @Override
    public void deleteById(Long userId) {
        log.debug("User delete request with id = {}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("User with id = {} not found", userId);
            throw new NotFoundException(String.format("User with id = %d not found", userId));
        }

        userRepository.deleteById(userId);
        log.info("User with id = {} has been deleted", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long userId) {
        log.debug("Get user request with id = {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id = %d not found", userId)));

        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserShortDto getShortById(Long userId) {
        log.debug("Get user short request with id = {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id = %d not found", userId)));

        return UserMapper.toUserShortDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserShortDto> getShortByIdIn(List<Long> ids) {
        log.debug("Get users short request with ids = {}", ids);

        return userRepository.findByIdIn(ids).stream()
                .map(UserMapper::toUserShortDto)
                .toList();
    }
}