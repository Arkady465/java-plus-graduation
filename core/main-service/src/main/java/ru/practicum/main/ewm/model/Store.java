package ru.practicum.main.ewm.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;
import ru.practicum.main.ewm.model.Entities.Category;
import ru.practicum.main.ewm.model.Entities.Compilation;
import ru.practicum.main.ewm.model.Entities.Event;
import ru.practicum.main.ewm.model.Entities.ParticipationRequest;
import ru.practicum.main.ewm.model.Entities.User;

@Component
public class Store {
    public final AtomicLong userSeq = new AtomicLong(0);
    public final AtomicLong catSeq = new AtomicLong(0);
    public final AtomicLong eventSeq = new AtomicLong(0);
    public final AtomicLong compSeq = new AtomicLong(0);
    public final AtomicLong reqSeq = new AtomicLong(0);

    public final Map<Long, User> users = new LinkedHashMap<>();
    public final Map<Long, Category> categories = new LinkedHashMap<>();
    public final Map<Long, Event> events = new LinkedHashMap<>();
    public final Map<Long, Compilation> compilations = new LinkedHashMap<>();
    public final Map<Long, ParticipationRequest> requests = new LinkedHashMap<>();
}

