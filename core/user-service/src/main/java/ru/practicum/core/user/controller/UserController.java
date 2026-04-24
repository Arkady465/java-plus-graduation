package ru.practicum.core.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.practicum.core.common.api.contract.UserApiContract;
import ru.practicum.core.common.dto.user.NewUserRequest;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.user.dto.UserParam;
import ru.practicum.core.user.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
public class UserController implements UserApiContract {
    private final UserService userService;

    @GetMapping
    public List<UserDto> getPage(@RequestParam(required = false) List<Long> ids,
                                 @RequestParam(required = false, defaultValue = "0") Integer from,
                                 @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.debug("GET /admin/users: ids = {}, from = {}, size = {}", ids, from, size);

        UserParam userParam = new UserParam(ids, from, size);

        return userService.getPage(userParam);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody @Valid NewUserRequest request) {
        log.debug("POST /admin/users: {}", request);

        return userService.create(request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long userId) {
        log.debug("DELETE /admin/users/{}", userId);

        userService.deleteById(userId);
    }

    @Override
    @GetMapping("/{userId}")
    public UserDto getById(@PathVariable @Positive Long userId) {
        log.debug("GET /admin/users/{}", userId);

        return userService.getById(userId);
    }

    @GetMapping("/short/{userId}")
    public UserShortDto getShortById(@PathVariable @Positive Long userId) {
        log.debug("GET /admin/users/short/{}", userId);

        return userService.getShortById(userId);
    }

    @GetMapping("/short")
    public List<UserShortDto> getShortByIdIn(@RequestParam List<Long> ids) {
        log.debug("GET /admin/users/short: ids = {}", ids);

        return userService.getShortByIdIn(ids);
    }
}