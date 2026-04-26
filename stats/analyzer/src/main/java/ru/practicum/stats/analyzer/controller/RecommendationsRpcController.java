package ru.practicum.stats.analyzer.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import ru.practicum.ewm.stats.grpc.recommendation.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.recommendation.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.recommendation.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.UserPredictionsRequestProto;
import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.analyzer.model.UserAction;
import ru.practicum.stats.analyzer.service.database.EventSimilarityService;
import ru.practicum.stats.analyzer.service.database.UserActionService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@GrpcService
public class RecommendationsRpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final UserActionService userActionService;
    private final EventSimilarityService eventSimilarityService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.debug("Received user predictions request: {}", request);

        try {
            // все взаимодействия пользователя
            List<UserAction> userActions = userActionService.getByUserId(request.getUserId());
            // set с id просмотренных событий
            Set<Long> viewedEventIds = userActions.stream()
                    .map(UserAction::getEventId)
                    .collect(Collectors.toSet());

            if (viewedEventIds.isEmpty()) {
                log.debug("User action history is empty, nothing to recommend");
                responseObserver.onCompleted();
                return;
            }

            // последние N взаимодействий
            List<UserAction> lastNViewed = userActions.stream()
                    .sorted(Comparator.comparing(UserAction::getTimestamp).reversed())
                    .limit(request.getMaxResults())
                    .toList();

            // id N других мероприятий, похожих на последние N, с которыми юзер не взаимодействовал
            List<Long> otherEvents = eventSimilarityService.getSimilarEvents(lastNViewed.stream()
                    .map(UserAction::getEventId)
                    .toList()).stream()
                    .filter(item -> !(viewedEventIds.contains(item.getEventA())
                            && viewedEventIds.contains(item.getEventB())))
                    .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                    .map(item -> viewedEventIds.contains(item.getEventA())
                            ? item.getEventB()
                            : item.getEventA())
                    .distinct()
                    .limit(request.getMaxResults())
                    .toList();
            log.debug("Selected other events: {}", otherEvents);

            for (Long otherEvent : otherEvents) {
                log.debug("Processing other event: {}", otherEvent);

                // мапа с мероприятиями, похожими на другое, но с которыми пользователь взаимодействовал
                Map<Long, Double> similarities = eventSimilarityService.getSimilarEvents(otherEvent).stream()
                        .filter(item -> viewedEventIds.contains(item.getEventA())
                                || viewedEventIds.contains(item.getEventB()))
                        .collect(Collectors.toMap(sim -> viewedEventIds.contains(sim.getEventA())
                                ? sim.getEventA()
                                : sim.getEventB(), EventSimilarity::getScore));
                log.debug("Similar events that user interacted with and their similarity score: {}", similarities);

                // мапа с оценками пользователя этих событий
                Map<Long, Double> ratings = userActionService.getUserScoresForEvents(request.getUserId(), similarities.keySet());
                log.debug("User ratings for similar events: {}", ratings);

                double nominator = 0.0;

                for (Map.Entry<Long, Double> entry : similarities.entrySet()) {
                    nominator += entry.getValue() * ratings.get(entry.getKey());
                }

                log.debug("nominator: {}", nominator);

                Double denominator = similarities.values().stream()
                        .reduce(Double::sum)
                        .orElseThrow(() -> new RuntimeException("Cannot calculate denominator"));
                log.debug("denominator: {}", denominator);

                double predictedRating = nominator / denominator;
                log.debug("prediction: {}", predictedRating);

                RecommendedEventProto recommendedEventProto = RecommendedEventProto.newBuilder()
                        .setEventId(otherEvent)
                        .setScore(predictedRating)
                        .build();
                log.debug("RecommendedEventProto: {}", recommendedEventProto);

                responseObserver.onNext(recommendedEventProto);
            }
            log.debug("Request completed");

            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error(exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(exception.getLocalizedMessage())
                            .withCause(exception)
            ));
        }
    }

    // TODO: проверка на взаимодействие с событием?
    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.debug("Received similar events request: {}", request);

        try {
            List<EventSimilarity> similarities = eventSimilarityService.getSimilarEvents(request.getEventId());
            Set<Long> viewedEventIds = userActionService.getByUserId(request.getUserId()).stream()
                    .map(UserAction::getEventId)
                    .collect(Collectors.toSet());

            List<EventSimilarity> similaritiesProcessed = similarities.stream()
                    .filter(item -> !(viewedEventIds.contains(item.getEventA())
                            && viewedEventIds.contains(item.getEventB())))
                    .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                    .limit(request.getMaxResults())
                    .toList();
            log.debug("Selected pairs: {}", similaritiesProcessed);

            for (EventSimilarity eventSimilarity : similaritiesProcessed) {
                long similarEventId = request.getEventId() == eventSimilarity.getEventA()
                        ? eventSimilarity.getEventB()
                        : eventSimilarity.getEventA();

                RecommendedEventProto recommendedEventProto = RecommendedEventProto.newBuilder()
                        .setEventId(similarEventId)
                        .setScore(eventSimilarity.getScore())
                        .build();
                log.debug("RecommendedEventProto: {}", recommendedEventProto);

                responseObserver.onNext(recommendedEventProto);
            }
            log.debug("Request completed");

            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error(exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(exception.getLocalizedMessage())
                            .withCause(exception)
            ));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.debug("Received interactions count request: {}", request);

        try {
            List<Long> eventIds = request.getEventIdList();

            Map<Long, Double> result = userActionService.getInteractionsCount(eventIds);
            log.debug("Interactions map: {}", result);

            for (Map.Entry<Long, Double> entry : result.entrySet()) {
                RecommendedEventProto recommendedEventProto = RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build();
                log.debug("RecommendedEventProto: {}", recommendedEventProto);

                responseObserver.onNext(recommendedEventProto);
            }
            log.debug("Request completed");

            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error(exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(exception.getLocalizedMessage())
                            .withCause(exception)
            ));
        }
    }
}