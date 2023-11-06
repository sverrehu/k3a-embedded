package no.shhsoft.k3aembedded;

import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class PlainKraftModeK3aEmbeddedTest
extends AbstractPlainK3aEmbeddedTest {

    @Override
    protected boolean isKraftMode() {
        return true;
    }

}
