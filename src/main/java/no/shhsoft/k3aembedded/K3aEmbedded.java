package no.shhsoft.k3aembedded;

import kafka.server.KafkaConfig;
import kafka.server.KafkaRaftServer;
import kafka.server.KafkaServer;
import kafka.server.Server;
import kafka.tools.StorageTool;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.metadata.bootstrap.BootstrapMetadata;
import org.apache.kafka.server.common.ApiMessageAndVersion;
import org.apache.kafka.server.common.MetadataVersion;
import scala.Option;
import scala.collection.immutable.Seq;
import scala.collection.mutable.ArrayBuffer;
import scala.jdk.CollectionConverters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that will run a Kafka broker, and optionally a ZooKeeper in
 * the current VM. May be used for testing Kafka integration.
 */
public final class K3aEmbedded {

    private static final int NODE_ID = 1;
    private Server server;
    private Path logDirectory;
    private ZooKeeper zooKeeper;
    private final boolean kraftMode;
    private final int brokerPort;
    private final int controllerPort;
    private final int zooKeeperPort;
    private final int[] additionalPorts;
    private final Map<String, Object> additionalConfiguration;
    private final AdditionalConfigurationProvider additionalConfigurationProvider;
    private final List<AdditionalListener> additionalListeners;

    /**
     * Implementors of this interface may provide additional broker
     * configuration. Will be called after all other configuration has
     * been determined. Use {@code Builder.additionalConfigurationProvider} to
     * set this up.
     */
    public interface AdditionalConfigurationProvider {

        /**
         * @return a {@code Map} of Kafka broker configurations
         *   that will be added to, or override the default
         *   configuration
         */
        Map<String, Object> getAdditionalConfiguration();

    }

    /**
     * Builder for {@code K3aEmbedded} instances.
     */
    public static final class Builder {

        private boolean kraftMode = true;
        private int brokerPort = -1;
        private int controllerPort = -1;
        private int zooKeeperPort = -1;
        private int numAdditionalPorts = 0;
        private Map<String, Object> additionalConfiguration;
        private AdditionalConfigurationProvider additionalConfigurationProvider;
        private final List<AdditionalListener> additionalListeners = new ArrayList<>();

        /**
         * Builds a {@code K3aEmbedded} instance based on builder
         * settings.
         *
         * @return a new {@code K3aEmbedded}.
         */
        public K3aEmbedded build() {
            return new K3aEmbedded(kraftMode, brokerPort, controllerPort, zooKeeperPort, numAdditionalPorts,
                                   additionalConfiguration, additionalConfigurationProvider, additionalListeners);
        }

        /**
         * Specifies whether to use KRaft mode. The default is
         * {@code true}. If {@code false} is specified, the
         * resulting {@code K3aEmbedded} will spawn a ZooKeeper
         * in addition to a broker.
         *
         * @param kraftMode whether to use KRaft mode
         * @return {@code this}
         */
        public Builder kraftMode(final boolean kraftMode) {
            this.kraftMode = kraftMode;
            return this;
        }

        /**
         * Assigns a fixed port to the broker listener. The default is
         * to assign a random port to the broker.
         *
         * @param brokerPort the broker port
         * @return {@code this}
         */
        public Builder brokerPort(final int brokerPort) {
            this.brokerPort = validatePort(brokerPort);
            return this;
        }

        /**
         * Assigns a fixed port to the controller listener. The
         * default is to assign a random port to the controller.
         *
         * @param controllerPort the controller port
         * @return {@code this}
         */
        public Builder controllerPort(final int controllerPort) {
            this.controllerPort = validatePort(controllerPort);
            return this;
        }

        /**
         * Assigns a fixed port to the controller ZooKeeper. The
         * default is to assign a random port.
         *
         * @param zooKeeperPort the ZooKeeper port
         * @return {@code this}
         */
        public Builder zooKeeperPort(final int zooKeeperPort) {
            this.zooKeeperPort = validatePort(zooKeeperPort);
            return this;
        }

        /**
         * Allocates additional, random ports that may be used to set
         * up extra listeners beyond the default.
         *
         * @param numAdditionalPorts number of extra ports to allocate
         * @return {@code this}
         */
        public Builder additionalPorts(final int numAdditionalPorts) {
            this.numAdditionalPorts = numAdditionalPorts;
            return this;
        }

        /**
         * Gives additional broker configuration options. The options
         * will be added to, or override the defaults.
         *
         * @param additionalConfiguration the configuration map
         * @return {@code this}
         */
        public Builder additionalConfiguration(final Map<String, Object> additionalConfiguration) {
            this.additionalConfiguration = additionalConfiguration;
            return this;
        }

        /**
         * Adds an extra listener.
         *
         * @param name the listener name
         * @param securityProtocol the security protocol of the listener
         * @param port the (fixed) port the listener binds to
         * @return {@code this}
         */
        public Builder additionalListenerWithFixedPort(final String name, final String securityProtocol, final int port) {
            additionalListeners.add(new AdditionalListener(name, securityProtocol, validatePort(port)));
            return this;
        }

        /**
         * Adds an extra listener bound to a random port. Requires
         * that additional random ports have been allocated through
         * the {@code additionalPorts} method.
         *
         * @param name the listener name
         * @param securityProtocol the security protocol of the listener
         * @param portIndex an index between {@code 0} and {@code additionalPorts - 1}
         * @return {@code this}
         */
        public Builder additionalListenerWithPortIndex(final String name, final String securityProtocol, final int portIndex) {
            additionalListeners.add(new AdditionalListener(name, securityProtocol, -portIndex));
            return this;
        }

        /**
         * Sets up an {@code AdditionalConfigurationProvider}
         * that will be called after every other configuration has
         * been settled. Not often needed, except in cases where the
         * additional configuration relies on the random ports.
         *
         * @param additionalConfigurationProvider the {@code AdditionalConfigurationProvider}
         * @return {@code this}
         */
        public Builder additionalConfigurationProvider(final AdditionalConfigurationProvider additionalConfigurationProvider) {
            if (this.additionalConfigurationProvider != null) {
                throw new RuntimeException("Only one AdditionalConfigurationProvider may be set up");
            }
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

    private K3aEmbedded(final boolean kraftMode, final int brokerPort, final int controllerPort, final int zooKeeperPort, final int numAdditionalPorts,
                        final Map<String, Object> additionalConfiguration, final AdditionalConfigurationProvider additionalConfigurationProvider,
                        final List<AdditionalListener> additionalListeners) {
        this.kraftMode = kraftMode;
        this.brokerPort = brokerPort > 0 ? brokerPort : NetworkUtils.getRandomAvailablePort();
        this.controllerPort = controllerPort > 0 ? controllerPort : NetworkUtils.getRandomAvailablePort();
        this.zooKeeperPort = zooKeeperPort > 0 ? zooKeeperPort : NetworkUtils.getRandomAvailablePort();
        this.additionalConfiguration = additionalConfiguration;
        this.additionalConfigurationProvider = additionalConfigurationProvider;
        this.additionalListeners = additionalListeners;
        this.additionalPorts = new int[numAdditionalPorts];
        for (int q = 0; q < this.additionalPorts.length; q++) {
            this.additionalPorts[q] = NetworkUtils.getRandomAvailablePort();
        }
        if (!this.kraftMode) {
            this.zooKeeper = new ZooKeeper(this.zooKeeperPort);
        }
    }

    /**
     * Starts the Kafka broker according to specification from the
     * {@code Builder}. If KRaft mode is disabled, also starts a
     * ZooKeeper.
     */
    public void start() {
        if (server != null) {
            throw new RuntimeException("Server already started");
        }
        if (!kraftMode) {
            zooKeeper.start();
        }
        logDirectory = createKafkaLogDirectory();
        final Map<String, Object> map = getConfigMap();
        final KafkaConfig config = new KafkaConfig(map);
        if (kraftMode) {
            formatKafkaLogDirectory(config);
            server = new KafkaRaftServer(config, Time.SYSTEM);
        } else {
            server = new KafkaServer(config, Time.SYSTEM, Option.empty(), false);
        }
        server.startup();
    }

    /**
     * Stops the running Kafka broker (and ZooKeeper, if enabled).
     */
    public void stop() {
        if (server == null) {
            return;
        }
        server.shutdown();
        server.awaitShutdown();
        server = null;
        FileUtils.deleteRecursively(logDirectory.toFile());
        logDirectory = null;
        if (!kraftMode) {
            zooKeeper.stop();
            zooKeeper = null;
        }
    }

    /**
     * @return the port the broker listener is bound to
     */
    public int getBrokerPort() {
        return brokerPort;
    }

    /**
     * @return the port the controller listener is bound to
     */
    public int getControllerPort() {
        return controllerPort;
    }

    /**
     * @return the port the ZooKeeper listens to
     */
    public int getZooKeeperPort() {
        return zooKeeperPort;
    }

    /**
     * Determines the real port value for one of the additional,
     * random ports allocated through
     * {@code Builder.additionalPorts}.
     *
     * @param portIndex the index into the additional ports array
     *   ({@code 0} to {@code additionalPorts - 1})
     * @return the actual port
     */
    public int getAdditionalPort(final int portIndex) {
        return additionalPorts[portIndex];
    }

    /**
     * @return the boostrap servers string clients are supposed to
     *   use when connecting to the default broker listener
     */
    public String getBootstrapServers() {
        return "localhost:" + getBrokerPort();
    }

    /**
     * Determines the bootstrap server string for one of the
     * additional listeners.
     *
     * @param portIndex the index into the additional ports array
     *   ({@code 0} to {@code additionalPorts - 1})
     * @return the boostrap servers string clients are supposed to
     *   use when connecting to the listener on the given port
     */
    public String getBootstrapServersForAdditionalPort(final int portIndex) {
        return "localhost:" + additionalPorts[portIndex];
    }

    private HashMap<String, Object> getConfigMap() {
        final HashMap<String, Object> map = new HashMap<>();
        if (kraftMode) {
            map.put("node.id", String.valueOf(NODE_ID));
            map.put("process.roles", "broker, controller");
            map.put("controller.quorum.voters", NODE_ID + "@localhost:" + controllerPort);
            map.put("controller.listener.names", "CONTROLLER");
        } else {
            map.put("zookeeper.connect", "localhost:" + zooKeeperPort);
        }
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
        sb.append("BROKER://:" + brokerPort);
        if (kraftMode) {
            sb.append(", CONTROLLER://:" + controllerPort);
        }
        for (final AdditionalListener additionalListener : additionalListeners) {
            final int port = additionalListener.port <= 0 ? additionalPorts[additionalListener.port] : additionalListener.port;
            sb.append(", " + additionalListener.name + "://:" + port);
        }
        return sb.toString();
    }

    private String getSecurityProtocolsString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BROKER:PLAINTEXT");
        if (kraftMode) {
            sb.append(", CONTROLLER:PLAINTEXT");
        }
        for (final AdditionalListener additionalListener : additionalListeners) {
            sb.append(", " + additionalListener.name + ":" + additionalListener.securityProtocol);
        }
        return sb.toString();
    }

    private void validateAndAddConfiguration(final HashMap<String, Object> map, final Map<String, Object> additionalConfiguration) {
        if (additionalConfiguration == null) {
            return;
        }
        if (kraftMode && additionalConfiguration.containsKey("node.id")
            && !map.get("node.id").toString().equals(additionalConfiguration.get("node.id").toString())) {
            throw new RuntimeException("node.id cannot be overridden");
        }
        map.putAll(additionalConfiguration);
    }

    private Path createKafkaLogDirectory() {
        return FileUtils.createTempDirectory("kafka");
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
        final MetadataVersion metadataVersion = findMetadataVersion();
        /* Use var for MetaProperties, since the class was moved between 3.6 and 3.7. */
        final var metaProperties = StorageTool.buildMetadataProperties(clusterId, kafkaConfig);
        final BootstrapMetadata bootstrapMetadata = StorageTool.buildBootstrapMetadata(metadataVersion, Option.<ArrayBuffer<ApiMessageAndVersion>>empty(), "format command");
        final Seq<String> seq = CollectionConverters.ListHasAsScala(Collections.singletonList(logDirectory.toString())).asScala().toList().toSeq();
        StorageTool.formatCommand(System.out, seq, metaProperties, bootstrapMetadata, metadataVersion, false);
    }

    private static MetadataVersion findMetadataVersion() {
        final String[] candidateMethods = new String[] { "latestProduction", "latest" };
        Exception lastException = null;
        for (final String candidateMethod : candidateMethods) {
            try {
                final Method latestVersionMethod = MetadataVersion.class.getMethod(candidateMethod);
                return (MetadataVersion) latestVersionMethod.invoke(null);
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                lastException = e;
            }
        }
        throw new RuntimeException("Unable to find or call method returning latest MetadataVersion", lastException);
    }

}
