package ru.practicum.stats.analyzer.service.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.analyzer.repository.EventSimilarityRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventSimilarityServiceImpl implements EventSimilarityService {
    private final EventSimilarityRepository eventSimilarityRepository;

    // TODO: batch
    @Override
    public void save(EventSimilarityAvro eventSimilarityAvro) {
        log.debug("Request for save/update event similarity: {}", eventSimilarityAvro);

        long eventA = Math.min(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());
        long eventB = Math.max(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());

        Optional<EventSimilarity> maybeEventSimilarity = eventSimilarityRepository.findByEventAAndEventB(eventA, eventB);

        if (maybeEventSimilarity.isEmpty()) {
            EventSimilarity eventSimilarity = toEventSimilarity(eventSimilarityAvro);
            eventSimilarity = eventSimilarityRepository.save(eventSimilarity);
            log.debug("Saved new event similarity: {}", eventSimilarity);
        } else {
            EventSimilarity eventSimilarity = maybeEventSimilarity.get();
            log.debug("Found existing event similarity: {}", eventSimilarity);

            if (eventSimilarityAvro.getScore() != eventSimilarity.getScore()) {
                eventSimilarity.setScore(eventSimilarityAvro.getScore());
                eventSimilarity.setTimestamp(LocalDateTime.ofInstant(eventSimilarityAvro.getTimestamp(), ZoneOffset.UTC));

                eventSimilarity = eventSimilarityRepository.save(eventSimilarity);
                log.debug("Updated event similarity: {}", eventSimilarity);
            } else {
                log.debug("No need to update event similarity");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSimilarity> getSimilarEvents(long eventId) {
        log.debug("Get event pairs for event ({}) request", eventId);

        List<EventSimilarity> pairs = eventSimilarityRepository.findByEventAOrEventB(eventId);
        log.debug("Pairs: {}", pairs);

        return pairs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSimilarity> getSimilarEvents(List<Long> eventIds) {
        log.debug("Get event pairs for events: {}", eventIds);

        List<EventSimilarity> pairs = eventSimilarityRepository.findByEventAInOrEventBIn(eventIds);
        log.debug("Pairs: {}", pairs);

        return pairs;
    }

    private EventSimilarity toEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = new EventSimilarity();

        long eventA = Math.min(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());
        long eventB = Math.max(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());

        eventSimilarity.setEventA(eventA);
        eventSimilarity.setEventB(eventB);
        eventSimilarity.setScore(eventSimilarityAvro.getScore());
        eventSimilarity.setTimestamp(LocalDateTime.ofInstant(eventSimilarityAvro.getTimestamp(), ZoneOffset.UTC));

        return eventSimilarity;
    }
}