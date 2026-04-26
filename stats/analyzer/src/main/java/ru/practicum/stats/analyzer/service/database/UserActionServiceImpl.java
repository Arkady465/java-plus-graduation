package ru.practicum.stats.analyzer.service.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.config.ActionWeightsConfig;
import ru.practicum.stats.analyzer.model.UserAction;
import ru.practicum.stats.analyzer.repository.UserActionRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserActionServiceImpl implements UserActionService {
    private final UserActionRepository userActionRepository;
    private final Map<ActionTypeAvro, Double> actionWeights;

    @Autowired
    public UserActionServiceImpl(UserActionRepository userActionRepository,
                                 ActionWeightsConfig actionWeightsConfig) {
        this.userActionRepository = userActionRepository;

        actionWeights = new HashMap<>();
        actionWeights.put(ActionTypeAvro.ACTION_VIEW, actionWeightsConfig.getView());
        actionWeights.put(ActionTypeAvro.ACTION_REGISTER, actionWeightsConfig.getRegister());
        actionWeights.put(ActionTypeAvro.ACTION_LIKE, actionWeightsConfig.getLike());
    }

    // TODO: batch
    @Override
    public void save(UserActionAvro userActionAvro) {
        log.debug("Request for save/update user action on event: {}", userActionAvro);

        Optional<UserAction> maybeUserAction = userActionRepository.findByUserIdAndEventId(userActionAvro.getUserId(),
                userActionAvro.getEventId());

        if (maybeUserAction.isEmpty()) {
            UserAction userAction = toUserAction(userActionAvro);
            userAction = userActionRepository.save(userAction);
            log.debug("Saved new user action: {}", userAction);
        } else {
            UserAction userAction = maybeUserAction.get();
            log.debug("Found existing user action: {}", userAction);

            if (actionWeights.get(userActionAvro.getActionType()) > actionWeights.get(userAction.getAction())) {
                userAction.setAction(userActionAvro.getActionType());
                userAction.setTimestamp(LocalDateTime.ofInstant(userActionAvro.getTimestamp(), ZoneOffset.UTC));

                userAction = userActionRepository.save(userAction);
                log.debug("Updated user action: {}", userAction);
            } else {
                log.debug("No need to update user action");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        log.debug("Get interactions count for events: {}", eventIds);

        List<UserAction> userActions = userActionRepository.findByEventIdIn(eventIds);

        Map<Long, Map<Long, Double>> grouped = userActions.stream()
                .collect(Collectors.groupingBy(
                        UserAction::getEventId,
                        Collectors.toMap(
                                UserAction::getUserId,
                                ua -> actionWeights.get(ua.getAction())
                        )
                ));
        log.debug("Actions grouped by event and with user weights: {}", grouped);

        return grouped.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream()
                                .mapToDouble(Double::doubleValue)
                                .sum()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAction> getByUserId(long userId) {
        log.debug("User actions request for user ({})", userId);

        List<UserAction> userActions = userActionRepository.findByUserId(userId);
        log.debug("Actions: {}", userActions);

        return userActions;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> getUserScoresForEvents(long userId, Collection<Long> eventIds) {
        log.debug("Get user ({}) scores for events: {}", userId, eventIds);

        List<Object[]> rawResult = userActionRepository.getUserScoresForEvents(userId, eventIds);
        log.debug("Raw: {}", rawResult);

        return rawResult.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> actionWeights.get(ActionTypeAvro.valueOf(result[1].toString()))));
    }

    private UserAction toUserAction(UserActionAvro userActionAvro) {
        UserAction userAction = new UserAction();

        userAction.setUserId(userActionAvro.getUserId());
        userAction.setEventId(userActionAvro.getEventId());
        userAction.setAction(userActionAvro.getActionType());
        userAction.setTimestamp(LocalDateTime.ofInstant(userActionAvro.getTimestamp(), ZoneOffset.UTC));

        return userAction;
    }
}