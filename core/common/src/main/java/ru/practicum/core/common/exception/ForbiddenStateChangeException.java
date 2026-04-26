package ru.practicum.core.common.exception;

public class ForbiddenStateChangeException extends RuntimeException {
    public ForbiddenStateChangeException(String message) {
        super(message);
    }
}