package ru.practicum.stats.analyzer.service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.config.KafkaConfig;
import ru.practicum.stats.analyzer.service.database.UserActionService;
import serialization.avro.UserActionDeserializer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
public class UserActionConsumer implements Runnable {
    private final KafkaConfig kafkaConfig;
    private final KafkaConsumer<Long, UserActionAvro> consumer;

    private final UserActionService userActionService;

    @Autowired
    public UserActionConsumer(KafkaConfig kafkaConfig, UserActionService userActionService) {
        this.kafkaConfig = kafkaConfig;
        consumer = createConsumer(kafkaConfig);
        this.userActionService = userActionService;
    }

    @Override
    public void run() {
        try {
            String actionsTopic = kafkaConfig.getTopics().getUsers();
            consumer.subscribe(List.of(actionsTopic));
            log.info("Analyzer user action consumer subscribed to the topic: {}", actionsTopic);

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records =
                        consumer.poll(Duration.ofMillis(kafkaConfig.getUserConsumer().getPollDurationMs()));

                for (ConsumerRecord<Long, UserActionAvro> consumerRecord : records) {
                    UserActionAvro userActionAvro = consumerRecord.value();
                    log.debug("Received user action avro: {}", userActionAvro);

                    userActionService.save(userActionAvro);
                }
            }
        } catch (WakeupException ignored) {

        } catch (Exception exception) {
            log.error("Error processing user action: {}", exception.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
        } finally {
            log.info("Closing Aggregator user action consumer...");
            consumer.close();
        }
    }

    private KafkaConsumer<Long, UserActionAvro> createConsumer(KafkaConfig kafkaConfig) {
        Properties consumerConfig = new Properties();

        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConfig.getUserConsumer().getClientId());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getUserConsumer().getGroupId());
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getServer());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);

        KafkaConsumer<Long, UserActionAvro> kafkaConsumer = new KafkaConsumer<>(consumerConfig);
        log.info("Analyzer user action consumer is using Kafka-server at url: {}", kafkaConfig.getServer());

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaConsumer::wakeup));

        return kafkaConsumer;
    }
}