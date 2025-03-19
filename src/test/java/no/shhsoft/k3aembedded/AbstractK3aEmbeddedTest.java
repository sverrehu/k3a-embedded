package no.shhsoft.k3aembedded;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractK3aEmbeddedTest {

    private static final String TOPIC = "the-topic";
    private static final String CONSUMER_GROUP_ID = "consumer-group";
    private int lastProducedValue = 0;
    private K3aEmbedded kafka;

    protected abstract boolean isKraftMode();

    protected abstract String getBootstrapServers();

    protected abstract Map<String, Object> getAdditionalClientConfig();

    protected abstract K3aEmbedded.Builder getK3aEmbeddedBuilder();

    @BeforeAll
    public final void beforeClass() {
        if (isUnsupportedZooKeeperTest()) {
            return;
        }
        kafka = getK3aEmbeddedBuilder().build();
        kafka.start();
    }

    @AfterAll
    public final void afterClass() {
        if (isUnsupportedZooKeeperTest()) {
            return;
        }
        kafka.stop();
    }

    @Test
    public final void shouldProduceAndConsume() {
        if (isUnsupportedZooKeeperTest()) {
            return;
        }
        try (final Producer<Integer, String> producer = getProducer()) {
            try (final Consumer<Integer, String> consumer = getConsumer()) {
                consumer.subscribe(Collections.singleton(TOPIC));
                produce(producer);
                final int consumedValue = consume(consumer);
                Assertions.assertEquals(lastProducedValue, consumedValue);
            }
        }
    }

    public final K3aEmbedded getKafka() {
        return kafka;
    }

    protected boolean isUnsupportedZooKeeperTest() {
        return !isKraftMode() && !K3aTestUtils.isZooKeeperModeSupported();
    }

    private Producer<Integer, String> getProducer() {
        final Map<String, Object> map = K3aTestUtils.producerProps(getBootstrapServers());
        map.putAll(getAdditionalClientConfig());
        return new KafkaProducer<>(map);
    }

    private Consumer<Integer, String> getConsumer() {
        final Map<String, Object> map = K3aTestUtils.consumerProps(getBootstrapServers(), AbstractK3aEmbeddedTest.CONSUMER_GROUP_ID, false);
        map.putAll(getAdditionalClientConfig());
        return new KafkaConsumer<>(map);
    }

    private void produce(final Producer<Integer, String> producer) {
        final ProducerRecord<Integer, String> record = new ProducerRecord<>(TOPIC, 0, String.valueOf(++lastProducedValue));
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

    private int consume(final Consumer<Integer, String> consumer) {
        int lastValue = -1;
        final ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(5000));
        for (final ConsumerRecord<Integer, String> record : records) {
            lastValue = Integer.valueOf(record.value());
            consumer.commitAsync();
        }
        return lastValue;
    }

}
