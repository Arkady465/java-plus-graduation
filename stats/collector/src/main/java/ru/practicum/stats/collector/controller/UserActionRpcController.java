package ru.practicum.stats.collector.controller;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Autowired;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.user.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.user.UserActionProto;
import ru.practicum.stats.collector.config.KafkaConfig;
import ru.practicum.stats.collector.service.Collector;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

@Slf4j
@GrpcService
public class UserActionRpcController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final KafkaConfig kafkaConfig;
    private final Collector collector;

    @Autowired
    public UserActionRpcController(KafkaConfig kafkaConfig, Collector collector) {
        this.kafkaConfig = kafkaConfig;
        this.collector = collector;

        log.debug("Started user action rpc controller");
        log.debug("Topic for user action messages: {}", kafkaConfig.getTopics().getUsers());
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.debug("Received user action message: {}", request);

        try {
            SpecificRecordBase specificRecordBase = toAvro(request);

            log.debug("Sending user action message: {}", specificRecordBase);
            collector.send(kafkaConfig.getTopics().getUsers(), request.getEventId(), specificRecordBase);
            responseObserver.onNext(Empty.getDefaultInstance());
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

    private SpecificRecordBase toAvro(UserActionProto userActionProto) {
        Timestamp timestamp = userActionProto.getTimestamp();

        return UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setActionType(ActionTypeAvro.valueOf(userActionProto.getActionType().name()))
                .setTimestamp(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()))
                .build();
    }
}