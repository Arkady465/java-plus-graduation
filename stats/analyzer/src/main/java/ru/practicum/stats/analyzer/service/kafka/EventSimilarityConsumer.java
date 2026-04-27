package ru.practicum.stats.analyzer.service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.config.KafkaConfig;
import ru.practicum.stats.analyzer.service.database.EventSimilarityService;
import serialization.avro.EventSimilarityDeserializer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
public class EventSimilarityConsumer {
    private final KafkaConfig kafkaConfig;
    private final KafkaConsumer<String, EventSimilarityAvro> consumer;
    private final EventSimilarityService eventSimilarityService;

    public EventSimilarityConsumer(KafkaConfig kafkaConfig, EventSimilarityService eventSimilarityService) {
        this.kafkaConfig = kafkaConfig;
        consumer = createConsumer(kafkaConfig);
        this.eventSimilarityService = eventSimilarityService;
    }

    public void start() {
        try {
            String actionsTopic = kafkaConfig.getTopics().getEvents();
            consumer.subscribe(List.of(actionsTopic));
            log.info("Analyzer event similarity consumer subscribed to the topic: {}", actionsTopic);

            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records =
                        consumer.poll(Duration.ofMillis(kafkaConfig.getEventConsumer().getPollDurationMs()));

                for (ConsumerRecord<String, EventSimilarityAvro> consumerRecord : records) {
                    EventSimilarityAvro eventSimilarityAvro = consumerRecord.value();
                    log.debug("Received event similarity avro: {}", eventSimilarityAvro);

                    eventSimilarityService.save(eventSimilarityAvro);
                }
            }
        } catch (WakeupException ignored) {

        } catch (Exception exception) {
            log.error("Error processing event similarity: {}", exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
        } finally {
            log.info("Closing Aggregator event similarity consumer...");
            consumer.close();
        }
    }

    private KafkaConsumer<String, EventSimilarityAvro> createConsumer(KafkaConfig kafkaConfig) {
        Properties consumerConfig = new Properties();

        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConfig.getEventConsumer().getClientId());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getEventConsumer().getGroupId());
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getServer());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityDeserializer.class);

        KafkaConsumer<String, EventSimilarityAvro> kafkaConsumer = new KafkaConsumer<>(consumerConfig);
        log.info("Analyzer event similarity consumer is using Kafka-server at url: {}", kafkaConfig.getServer());

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaConsumer::wakeup));

        return kafkaConsumer;
    }
}