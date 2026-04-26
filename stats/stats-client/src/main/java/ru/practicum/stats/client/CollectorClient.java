package ru.practicum.stats.client;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import ru.practicum.ewm.stats.grpc.user.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.user.ActionTypeProto;
import ru.practicum.ewm.stats.proto.user.UserActionProto;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

@Slf4j
@Component
public class CollectorClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserAction(long userId, long eventId, ActionTypeProto action) {
        log.debug("Collect user ({}) action ({}) for event ({}) request", userId, action, eventId);

        Instant now = Instant.now();
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(action)
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.getEpochSecond()).setNanos(now.getNano()).build())
                .build();
        log.debug("Request: {}", request);

        try {
            client.collectUserAction(request);
            log.debug("Successfully sent user action");
        } catch (StatusRuntimeException exception) {
            log.debug("Rpc-method call failure");
            log.error(exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
        }
    }
}