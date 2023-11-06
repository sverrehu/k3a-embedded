package no.shhsoft.k3aembedded;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Collections;
import java.util.Map;

abstract class AbstractPlainK3aEmbeddedTest
extends AbstractK3aEmbeddedTest {

    @Override
    protected Map<String, Object> getAdditionalClientConfig() {
        return Collections.emptyMap();
    }

}
