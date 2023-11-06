package no.shhsoft.k3aembedded;

import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractPlainK3aEmbeddedTest
extends AbstractK3aEmbeddedTest {

    @Override
    protected K3aEmbedded.Builder getK3aEmbeddedBuilder() {
        return new K3aEmbedded.Builder().kraftMode(isKraftMode());
    }

    @Override
    protected final String getBootstrapServers() {
        return getKafka().getBootstrapServers();
    }

    @Override
    protected final Map<String, Object> getAdditionalClientConfig() {
        return Collections.emptyMap();
    }

}
