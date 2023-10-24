package no.shhsoft.k3aembedded;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class AbstractK3aEmbeddedTest {

    private static final String TOPIC = "the-topic";
    private static final String CONSUMER_GROUP_ID = "consumer-group";
    private int lastProducedValue = 0;

    protected abstract String getBootstrapServers();

    protected abstract Map<String, Object> getAdditionalClientConfig();

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
        final Map<String, Object> map = K3aTestUtils.producerProps(getBootstrapServers());
        map.putAll(getAdditionalClientConfig());
        return new KafkaProducer<>(map);
    }

    private Consumer<Integer, Integer> getConsumer() {
        final Map<String, Object> map = K3aTestUtils.consumerProps(getBootstrapServers(), AbstractK3aEmbeddedTest.CONSUMER_GROUP_ID, false);
        map.putAll(getAdditionalClientConfig());
        return new KafkaConsumer<>(map);
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
        for (final ConsumerRecord<Integer, Integer> record : records) {
            lastValue = record.value();
            consumer.commitAsync();
        }
        return lastValue;
    }

}
