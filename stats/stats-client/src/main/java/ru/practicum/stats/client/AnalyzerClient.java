package ru.practicum.stats.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import ru.practicum.ewm.stats.grpc.recommendation.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.recommendation.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.recommendation.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.UserPredictionsRequestProto;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class AnalyzerClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.debug("Get interactions count for events: {}", eventIds);

        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        log.debug("Request: {}", request);

        try {
            Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
            Stream<RecommendedEventProto> stream = asStream(iterator);
            log.debug("Successfully got interactions count");

            return stream;
        } catch (StatusRuntimeException exception) {
            log.debug("Rpc-method call failure");
            log.error(exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);

            throw new RuntimeException("Rpc-method call failure", exception);
        }
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.debug("Get events similar to ({}) request for user ({})", eventId, userId);
        log.debug("Max results: {}", maxResults);

        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        log.debug("Request: {}", request);

        try {
            Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
            Stream<RecommendedEventProto> stream = asStream(iterator);
            log.debug("Successfully got similar events");

            return stream;
        } catch (StatusRuntimeException exception) {
            log.debug("Rpc-method call failure");
            log.error(exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);

            throw new RuntimeException("Rpc-method call failure", exception);
        }
    }

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.debug("Get recommendations for user ({})", userId);
        log.debug("Max results: {}", maxResults);

        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        log.debug("Request: {}", request);

        try {
            Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);
            Stream<RecommendedEventProto> stream = asStream(iterator);
            log.debug("Successfully got recommendations");

            return stream;
        } catch (StatusRuntimeException exception) {
            log.debug("Rpc-method call failure");
            log.error(exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);

            throw new RuntimeException("Rpc-method call failure", exception);
        }
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}