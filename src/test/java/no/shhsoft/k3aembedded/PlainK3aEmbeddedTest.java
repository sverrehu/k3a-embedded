package no.shhsoft.k3aembedded;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Collections;
import java.util.Map;

public final class PlainK3aEmbeddedTest
extends AbstractK3aEmbeddedTest {

    private static K3aEmbedded kafka;

    @BeforeClass
    public static void beforeClass() {
        kafka = new K3aEmbedded.Builder().build();
        kafka.start();
    }

    @AfterClass
    public static void afterClass() {
        kafka.stop();
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
