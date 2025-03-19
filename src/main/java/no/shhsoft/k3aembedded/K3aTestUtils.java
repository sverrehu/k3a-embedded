package no.shhsoft.k3aembedded;

import kafka.server.KafkaConfig;
import kafka.server.Server;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Time;
import scala.Option;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/* Inspired by / stolen from org.springframework.kafka.test.utils.KafkaTestUtils */

/**
 * Helper methods for writing tests using {@link K3aEmbedded}.
 */
public final class K3aTestUtils {

    private K3aTestUtils() {
    }

    /**
     * Set up test properties for an {@code <Integer, String>} consumer.
     *
     * @param group the group id.
     * @param autoCommit the auto commit.
     * @param k3aEmbedded a {@link K3aEmbedded} instance.
     * @return the properties.
     */
    public static Map<String, Object> consumerProps(final String group, final boolean autoCommit, final K3aEmbedded k3aEmbedded) {
        return consumerProps(k3aEmbedded.getBootstrapServers(), group, autoCommit);
    }

    /**
     * Set up test properties for an {@code <Integer, String>} producer.
     *
     * @param k3aEmbedded a {@link K3aEmbedded} instance.
     * @return the properties.
     */
    public static Map<String, Object> producerProps(final K3aEmbedded k3aEmbedded) {
        return producerProps(k3aEmbedded.getBootstrapServers());
    }

    /**
     * Set up test properties for admin client.
     *
     * @param k3aEmbedded a {@link K3aEmbedded} instance.
     * @return the properties.
     */
    public static Map<String, Object> adminProps(final K3aEmbedded k3aEmbedded) {
        return adminProps(k3aEmbedded.getBootstrapServers());
    }

    /**
     * Set up test properties for an {@code <Integer, String>} consumer.
     *
     * @param bootstrapServers the bootstrapServers property.
     * @param group the group id.
     * @param autoCommit the auto commit.
     * @return the properties.
     */
    public static Map<String, Object> consumerProps(final String bootstrapServers, final String group, final boolean autoCommit) {
        final Map<String, Object> map = new HashMap<>();
        map.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        map.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        map.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
        map.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10");
        map.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        map.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        map.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return map;
    }

    /**
     * Set up test properties for an {@code <Integer, String>} producer.
     *
     * @param bootstrapServers the bootstrapServers property.
     * @return the properties.
     */
    public static Map<String, Object> producerProps(final String bootstrapServers) {
        final Map<String, Object> map = new HashMap<>();
        map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        map.put(ProducerConfig.ACKS_CONFIG, "all");
        map.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "3000");
        map.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        map.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        map.put(ProducerConfig.RETRIES_CONFIG, "0");
        map.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        map.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return map;
    }

    /**
     * Set up test properties for an admin client.
     *
     * @param bootstrapServers the bootstrapServers property.
     * @return the properties.
     */
    public static Map<String, Object> adminProps(final String bootstrapServers) {
        final Map<String, Object> map = new HashMap<>();
        map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return map;
    }

    /**
     * Checks if ZooKeeper is supported in this version of Kafka, i.e. if the Kafka major version is less than 4.
     *
     * @return <code>true</code> if ZooKeeper mode is supported, otherwise <code>false</code>.
     */
    public static boolean isZooKeeperModeSupported() {
        try {
            Class.forName("kafka.server.KafkaServer");
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

}
