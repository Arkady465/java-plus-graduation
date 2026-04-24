package ru.practicum.exception;

public class StatsServerNotFoundException extends RuntimeException {
    public StatsServerNotFoundException(String message) {
        super(message);
    }
}