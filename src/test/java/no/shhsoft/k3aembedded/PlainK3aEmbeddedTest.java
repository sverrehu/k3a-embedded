package no.shhsoft.k3aembedded;

import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class PlainK3aEmbeddedTest
extends AbstractK3aEmbeddedTest {

    @Override
    protected K3aEmbedded.Builder getK3aEmbeddedBuilder() {
        return new K3aEmbedded.Builder();
    }

    @Override
    protected String getBootstrapServers() {
        return getKafka().getBootstrapServers();
    }

    @Override
    protected Map<String, Object> getAdditionalClientConfig() {
        return Collections.emptyMap();
    }

}
