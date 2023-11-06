package no.shhsoft.k3aembedded;

import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class SaslK3aEmbeddedTest
extends AbstractK3aEmbeddedTest {

    @Override
    protected K3aEmbedded.Builder getK3aEmbeddedBuilder() {
        final Map<String, Object> map = new HashMap<>();
        map.put("listener.name.sasl_plaintext.sasl.enabled.mechanisms", "PLAIN");
        map.put("listener.name.sasl_plaintext.plain.sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka\" password=\"kafka\" user_kafka=\"kafka\";");
        return new K3aEmbedded.Builder()
                .additionalPorts(1)
                .additionalConfiguration(map)
                .additionalListenerWithPortIndex("SASL_PLAINTEXT", "SASL_PLAINTEXT", 0);
    }

    @Override
    protected String getBootstrapServers() {
        return getKafka().getBootstrapServersForAdditionalPort(0);
    }

    @Override
    protected Map<String, Object> getAdditionalClientConfig() {
        final Map<String, Object> map = new HashMap<>();
        map.put("security.protocol", "SASL_PLAINTEXT");
        map.put("sasl.mechanism", "PLAIN");
        map.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka\" password=\"kafka\";");
        return map;
    }

}
