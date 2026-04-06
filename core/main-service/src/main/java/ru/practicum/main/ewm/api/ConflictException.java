package ru.practicum.main.ewm.api;

public class ConflictException extends RuntimeException {
    private final String reason;

    public ConflictException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}

