package no.shhsoft.k3aembedded;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class K3aEmbeddedTest {

    private static K3aEmbedded kafka;
    private static final String TOPIC = "the-topic";
    private static final String CONSUMER_GROUP_ID = "consumer-group";
    private int lastProducedValue = 0;

    @BeforeClass
    public static void beforeClass() {
        kafka = new K3aEmbedded.Builder().build();
        kafka.start();
    }

    @AfterClass
    public static void afterClass() {
        kafka.stop();
    }

    @Test
    public void shouldProduceAndConsume() {
        try (final Producer<Integer, Integer> producer = getProducer()) {
            try (final Consumer<Integer, Integer> consumer = getConsumer()) {
                consumer.subscribe(Collections.singleton(TOPIC));
                produce(producer);
                final int consumedValue = consume(consumer);
                Assert.assertEquals(lastProducedValue, consumedValue);
            }
        }
    }

    public Producer<Integer, Integer> getProducer() {
        final Map<String, Object> map = getCommonConfig();
        map.put(ProducerConfig.ACKS_CONFIG, "all");
        map.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "3000");
        map.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        map.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        map.put(ProducerConfig.RETRIES_CONFIG, "1");
        map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        return new KafkaProducer<>(map);
    }

    private Consumer<Integer, Integer> getConsumer() {
        final Map<String, Object> map = getCommonConfig();
        map.put(ConsumerConfig.GROUP_ID_CONFIG, K3aEmbeddedTest.CONSUMER_GROUP_ID);
        map.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        map.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        map.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        map.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        return new KafkaConsumer<>(map);
    }

    private Map<String, Object> getCommonConfig() {
        final Map<String, Object> map = new HashMap<>();
        map.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        return map;
    }

    private void produce(final Producer<Integer, Integer> producer) {
        final ProducerRecord<Integer, Integer> record = new ProducerRecord<>(TOPIC, 0, ++lastProducedValue);
        try {
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RuntimeException(exception);
                }
            }).get(); // Make call synchronous, to be able to get exceptions in time.
        } catch (final InterruptedException | ExecutionException e) {
            final Throwable cause = e.getCause();
            throw (cause instanceof RuntimeException) ? (RuntimeException) cause : new RuntimeException(e);
        }
        producer.flush();
    }

    private int consume(final Consumer<Integer, Integer> consumer) {
        int lastValue = -1;
        final ConsumerRecords<Integer, Integer> records = consumer.poll(Duration.ofMillis(5000));
        System.out.println("************ got " + (records.iterator().hasNext() ? "some" : "none"));
        for (final ConsumerRecord<Integer, Integer> record : records) {
            lastValue = record.value();
            consumer.commitAsync();
        }
        return lastValue;
    }

}
