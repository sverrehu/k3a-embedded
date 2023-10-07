package no.shhsoft.k3aembedded;

import kafka.server.KafkaConfig;
import kafka.server.KafkaRaftServer;
import kafka.server.MetaProperties;
import kafka.server.Server;
import kafka.tools.StorageTool;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.metadata.bootstrap.BootstrapMetadata;
import org.apache.kafka.server.common.MetadataVersion;
import scala.Option;
import scala.collection.immutable.Seq;
import scala.jdk.CollectionConverters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class K3aEmbedded {

    private static final int NODE_ID = 1;
    private Server server;
    private Path logDirectory;
    private final int brokerPort;
    private final int controllerPort;
    private final int[] additionalPorts;
    private final Map<String, Object> additionalConfiguration;
    private final AdditionalConfigurationProvider additionalConfigurationProvider;
    private final List<AdditionalListener> additionalListeners;

    public interface AdditionalConfigurationProvider {

        Map<String, Object> getAdditionalConfiguration();

    }

    public static final class Builder {

        private int brokerPort = -1;
        private int controllerPort = -1;
        private int numAdditionalPorts = 0;
        private Map<String, Object> additionalConfiguration;
        private AdditionalConfigurationProvider additionalConfigurationProvider;
        private final List<AdditionalListener> additionalListeners = new ArrayList<>();

        public K3aEmbedded build() {
            return new K3aEmbedded(brokerPort, controllerPort, numAdditionalPorts, additionalConfiguration, additionalConfigurationProvider, additionalListeners);
        }

        public Builder brokerPort(final int brokerPort) {
            this.brokerPort = validatePort(brokerPort);
            return this;
        }

        public Builder controllerPort(final int controllerPort) {
            this.controllerPort = validatePort(controllerPort);
            return this;
        }

        public Builder additionalPorts(final int numAdditionalPorts) {
            this.numAdditionalPorts = numAdditionalPorts;
            return this;
        }

        public Builder additionalConfiguration(final Map<String, Object> additionalConfiguration) {
            this.additionalConfiguration = additionalConfiguration;
            return this;
        }

        public Builder additionalListenerWithFixedPort(final String name, final String securityProtocol, final int port) {
            additionalListeners.add(new AdditionalListener(name, securityProtocol, validatePort(port)));
            return this;
        }

        public Builder additionalListenerWithPortIndex(final String name, final String securityProtocol, final int portIndex) {
            additionalListeners.add(new AdditionalListener(name, securityProtocol, -portIndex));
            return this;
        }

        public Builder additionalConfigurationProvider(final AdditionalConfigurationProvider additionalConfigurationProvider) {
            this.additionalConfigurationProvider = additionalConfigurationProvider;
            return this;
        }

        private static int validatePort(final int port) {
            if (port < 1 || port > 65535) {
                throw new RuntimeException("Ports must be in the range 1 to 65535");
            }
            return port;
        }

    }

    private static final class AdditionalListener {

        private final String name;
        private final String securityProtocol;
        private final int port; /* If less than 1, use the negated number as index into additionalPorts. */

        private AdditionalListener(final String name, final String securityProtocol, final int port) {
            this.name = name;
            this.securityProtocol = securityProtocol;
            this.port = port;
        }

    }

    /**
     * @deprecated Use Builder instead.
     */
    @Deprecated
    public K3aEmbedded() {
        this(-1, -1, 0, null, null, Collections.emptyList());
    }

    private K3aEmbedded(final int brokerPort, final int controllerPort, final int numAdditionalPorts,
                        final Map<String, Object> additionalConfiguration, final AdditionalConfigurationProvider additionalConfigurationProvider,
                        final List<AdditionalListener> additionalListeners) {
        this.brokerPort = brokerPort > 0 ? brokerPort : NetworkUtils.getRandomAvailablePort();
        this.controllerPort = controllerPort > 0 ? controllerPort : NetworkUtils.getRandomAvailablePort();
        this.additionalConfiguration = additionalConfiguration;
        this.additionalConfigurationProvider = additionalConfigurationProvider;
        this.additionalListeners = additionalListeners;
        this.additionalPorts = new int[numAdditionalPorts];
        for (int q = 0; q < this.additionalPorts.length; q++) {
            this.additionalPorts[q] = NetworkUtils.getRandomAvailablePort();
        }
    }

    public void start() {
        if (server != null) {
            throw new RuntimeException("Server already started");
        }
        logDirectory = createKafkaLogDirectory();
        final Map<String, Object> map = getConfigMap();
        final KafkaConfig config = new KafkaConfig(map);
        formatKafkaLogDirectory(config);
        server = new KafkaRaftServer(config, Time.SYSTEM);
        server.startup();
    }

    public void stop() {
        if (server == null) {
            return;
        }
        server.shutdown();
        server.awaitShutdown();
        server = null;
        FileUtils.deleteRecursively(logDirectory.toFile());
        logDirectory = null;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public int getControllerPort() {
        return controllerPort;
    }

    public int getAdditionalPort(final int index) {
        return additionalPorts[index];
    }

    public String getBootstrapServers() {
        return "localhost:" + getBrokerPort();
    }

    public String getBootstrapServersForAdditionalPort(final int index) {
        return "localhost:" + additionalPorts[index];
    }

    private HashMap<String, Object> getConfigMap() {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("node.id", String.valueOf(NODE_ID));
        map.put("process.roles", "broker, controller");
        map.put("controller.quorum.voters", NODE_ID + "@localhost:" + controllerPort);
        map.put("controller.listener.names", "CONTROLLER");
        map.put("inter.broker.listener.name", "BROKER");
        map.put("listeners", getListenersString());
        map.put("listener.security.protocol.map", getSecurityProtocolsString());
        map.put("log.dir", logDirectory.toString());
        map.put("offsets.topic.num.partitions", "1");
        map.put("offsets.topic.replication.factor", "1");
        map.put("group.initial.rebalance.delay.ms", "0");
        validateAndAddConfiguration(map, additionalConfiguration);
        if (additionalConfigurationProvider != null) {
            validateAndAddConfiguration(map, additionalConfigurationProvider.getAdditionalConfiguration());
        }
        return map;
    }

    private String getListenersString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BROKER://:" + brokerPort + ", CONTROLLER://:" + controllerPort);
        for (final AdditionalListener additionalListener : additionalListeners) {
            final int port = additionalListener.port <= 0 ? additionalPorts[additionalListener.port] : additionalListener.port;
            sb.append(", " + additionalListener.name + "://:" + port);
        }
        return sb.toString();
    }

    private String getSecurityProtocolsString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BROKER:PLAINTEXT, CONTROLLER:PLAINTEXT");
        for (final AdditionalListener additionalListener : additionalListeners) {
            sb.append(", " + additionalListener.name + ":" + additionalListener.securityProtocol);
        }
        return sb.toString();
    }
    private void validateAndAddConfiguration(final HashMap<String, Object> map, final Map<String, Object> additionalConfiguration) {
        if (additionalConfiguration == null) {
            return;
        }
        if (additionalConfiguration.containsKey("node.id") && !map.get("node.id").toString().equals(additionalConfiguration.get("node.id").toString())) {
            throw new RuntimeException("node.id cannot be overriden");
        }
        map.putAll(additionalConfiguration);
    }

    private Path createKafkaLogDirectory() {
        try {
            return Files.createTempDirectory("kafka");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void formatKafkaLogDirectory(final KafkaConfig kafkaConfig) {
        /* This is to mimic the command line formatting of the log directory,
         * which is required when KRaft mode is used. Easiest approach would be to
         * call StorageTool.main with the correct command line arguments, but
         * unfortunately the main method does an explicit exit at the end.
         * So this is the second best: Mimic what StorageTool.main does for
         * the "format" command. */
        if (logDirectory == null) {
            throw new RuntimeException("No log directory. This should not happen.");
        }
        final String clusterId = Uuid.randomUuid().toString();
        final MetadataVersion metadataVersion = MetadataVersion.latest();
        final MetaProperties metaProperties = StorageTool.buildMetadataProperties(clusterId, kafkaConfig);
        final BootstrapMetadata bootstrapMetadata = StorageTool.buildBootstrapMetadata(metadataVersion, Option.empty(), "format command");
        final Seq<String> seq = CollectionConverters.ListHasAsScala(Collections.singletonList(logDirectory.toString())).asScala().toList().toSeq();
        StorageTool.formatCommand(System.out, seq, metaProperties, bootstrapMetadata, metadataVersion, false);
    }

}
