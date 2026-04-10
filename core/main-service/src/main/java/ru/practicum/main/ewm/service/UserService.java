package ru.practicum.main.ewm.service;

import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.ewm.api.ConflictException;
import ru.practicum.main.ewm.api.NotFoundException;
import ru.practicum.main.ewm.domain.UserEntity;
import ru.practicum.main.ewm.dto.user.NewUserRequest;
import ru.practicum.main.ewm.dto.user.UserDto;
import ru.practicum.main.ewm.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserDto addUser(NewUserRequest req) {
        String email = req.getEmail().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Integrity constraint has been violated.", "Email must be unique");
        }
        UserEntity u = new UserEntity();
        u.setName(req.getName().trim());
        u.setEmail(email);
        userRepository.save(u);
        return toDto(u);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        List<UserEntity> list = (ids == null || ids.isEmpty())
                ? userRepository.findAll()
                : userRepository.findByIdIn(ids);
        return list.stream()
                .sorted(Comparator.comparing(UserEntity::getId))
                .skip(from)
                .limit(size)
                .map(UserService::toDto)
                .toList();
    }

    public void deleteUser(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public UserEntity requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private static UserDto toDto(UserEntity u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail());
    }
}
