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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class K3aEmbedded {

    private static final int NODE_ID = 1;
    private Server server;
    private Path logDirectory;
    private final int brokerPort;
    private final int controllerPort;
    private final Map<String, String> additionalConfiguration;
    private final AdditionalConfigurationProvider additionalConfigurationProvider;

    public interface AdditionalConfigurationProvider {

        Map<String, String> getAdditionalConfiguration();

    }

    public static final class Builder {

        private int brokerPort = -1;
        private int controllerPort = -1;
        private Map<String, String> additionalConfiguration;
        private AdditionalConfigurationProvider additionalConfigurationProvider;

        public K3aEmbedded build() {
            return new K3aEmbedded(brokerPort, controllerPort, additionalConfiguration, additionalConfigurationProvider);
        }

        public Builder brokerPort(final int brokerPort) {
            this.brokerPort = brokerPort;
            return this;
        }

        public Builder controllerPort(final int controllerPort) {
            this.controllerPort = controllerPort;
            return this;
        }

        public Builder additionalConfiguration(final Map<String, String> additionalConfiguration) {
            this.additionalConfiguration = additionalConfiguration;
            return this;
        }

        public Builder additionalConfigurationProvider(final AdditionalConfigurationProvider additionalConfigurationProvider) {
            this.additionalConfigurationProvider = additionalConfigurationProvider;
            return this;
        }

    }

    /**
     * @deprecated Use Builder instead.
     */
    public K3aEmbedded() {
        this(-1, -1, null, null);
    }

    private K3aEmbedded(final int brokerPort, final int controllerPort, final Map<String, String> additionalConfiguration, final AdditionalConfigurationProvider additionalConfigurationProvider) {
        this.brokerPort = brokerPort >= 0 ? brokerPort : NetworkUtils.getRandomAvailablePort();
        this.controllerPort = controllerPort >= 0 ? controllerPort : NetworkUtils.getRandomAvailablePort();
        this.additionalConfiguration = additionalConfiguration;
        this.additionalConfigurationProvider = additionalConfigurationProvider;
    }

    public void start() {
        if (server != null) {
            throw new RuntimeException("Server already started");
        }
        logDirectory = createKafkaLogDirectory();
        final Map<String, String> map = getConfigMap();
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

    public String getBootstrapServers() {
        return "localhost:" + getBrokerPort();
    }

    private HashMap<String, String> getConfigMap() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("node.id", String.valueOf(NODE_ID));
        map.put("process.roles", "broker, controller");
        map.put("controller.quorum.voters", NODE_ID + "@localhost:" + controllerPort);
        map.put("controller.listener.names", "CONTROLLER");
        map.put("inter.broker.listener.name", "BROKER");
        map.put("listeners", "BROKER://:" + brokerPort + ", CONTROLLER://:" + controllerPort);
        map.put("listener.security.protocol.map", "BROKER:PLAINTEXT, CONTROLLER:PLAINTEXT");
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

    private void validateAndAddConfiguration(final HashMap<String, String> map, final Map<String, String> additionalConfiguration) {
        if (additionalConfiguration == null) {
            return;
        }
        if (additionalConfiguration.containsKey("node.id") && !map.get("node.id").equals(additionalConfiguration.get("node.id"))) {
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
