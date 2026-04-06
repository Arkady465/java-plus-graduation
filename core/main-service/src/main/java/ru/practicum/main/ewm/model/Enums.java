package ru.practicum.main.ewm.model;

public final class Enums {
    private Enums() {
    }

    public enum EventState {PENDING, PUBLISHED, CANCELED}

    public enum Sort {EVENT_DATE, VIEWS}

    public enum AdminStateAction {PUBLISH_EVENT, REJECT_EVENT}

    public enum UserStateAction {SEND_TO_REVIEW, CANCEL_REVIEW}

    public enum RequestStatus {PENDING, CONFIRMED, REJECTED, CANCELED}
}

