package ru.practicum.stats.collector.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.stereotype.Component;
import serialization.avro.GeneralAvroSerializer;

import ru.practicum.stats.collector.config.KafkaConfig;

import java.util.Properties;
import java.util.concurrent.Future;

@Slf4j
@Component
public class Collector {
    private final KafkaProducer<Long, SpecificRecordBase> producer;

    public Collector(KafkaConfig kafkaConfig) {
        Properties config = new Properties();
        String serverUrl = kafkaConfig.getServer();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverUrl);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class);

        producer = new KafkaProducer<>(config);
        log.info("Collector is using Kafka-server at url: {}", serverUrl);
    }

    public Future<RecordMetadata> send(String topic, Long key, SpecificRecordBase value) {
        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(topic, key, value);
        log.debug("Sending message to the topic [{}] with key [{}]: {}", topic, key, value);

        return producer.send(record);
    }

    @PreDestroy
    public void preDestroy() {
        if (producer != null) {
            try {
                producer.flush();
            } finally {
                log.info("Closing Collector Kafka-producer...");
                producer.close();
            }
        }
    }
}