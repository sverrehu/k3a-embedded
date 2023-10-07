package no.shhsoft.k3aembedded;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.HashMap;
import java.util.Map;

public final class SaslK3aEmbeddedTest
extends AbstractK3aEmbeddedTest {

    private static K3aEmbedded kafka;

    @BeforeClass
    public static void beforeClass() {
        final Map<String, Object> map = new HashMap<>();
        map.put("listener.name.sasl_plaintext.sasl.enabled.mechanisms", "PLAIN");
        map.put("listener.name.sasl_plaintext.plain.sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka\" password=\"kafka\" user_kafka=\"kafka\";");
        kafka = new K3aEmbedded.Builder()
                .additionalPorts(1)
                .additionalConfiguration(map)
                .additionalListenerWithPortIndex("SASL_PLAINTEXT", "SASL_PLAINTEXT", 0)
                .build();
        kafka.start();
    }

    @AfterClass
    public static void afterClass() {
        kafka.stop();
    }

    @Override
    protected String getBootstrapServers() {
        return kafka.getBootstrapServersForAdditionalPort(0);
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
