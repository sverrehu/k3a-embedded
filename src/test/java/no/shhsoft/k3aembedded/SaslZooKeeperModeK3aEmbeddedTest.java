package no.shhsoft.k3aembedded;

import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class SaslZooKeeperModeK3aEmbeddedTest
extends AbstractSaslK3aEmbeddedTest {

    @Override
    protected boolean isKraftMode() {
        return false;
    }

}
