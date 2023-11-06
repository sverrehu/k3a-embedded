package no.shhsoft.k3aembedded;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public final class PlainKraftModeK3aEmbeddedTest
extends AbstractPlainK3aEmbeddedTest {

    @Override
    protected K3aEmbedded.Builder getK3aEmbeddedBuilder() {
        return new K3aEmbedded.Builder();
    }

    @Override
    protected String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    @Override
    protected Map<String, Object> getAdditionalClientConfig() {
        return Collections.emptyMap();
    }

}
