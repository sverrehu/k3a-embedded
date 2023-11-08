# k3a-embedded

Embedded Kafka server for running simple integration tests. Embedded
in the sense that the Kafka server is running in the same VM as the
tests. No container runtime required.

## Limitations

* Starts a single-node "cluster".

## What it does

* Creates a temporary directory that will hold all runtime files of
  Kafka. This directory is deleted when the server is stopped.
* Finds a random available port that the broker will listen to.

## Usage

To use this tool, add a dependency like the following:

```xml
    <dependency>
        <groupId>no.shhsoft</groupId>
        <artifactId>k3a-embedded</artifactId>
        <version>0.5.0+${kafka.version}</version>
        <scope>test</scope>
    </dependency>
```

Where `${kafka.version}` is the same version you intend to use for the
kafka-clients library. Since both the server, which uses kafka-clients
internally, and your test code runs in the same VM, the versions must
match.

The following Kafka versions are supported at the moment:

* 3.6.0
* 3.5.1

For JUnit tests, do something like this for starting, stopping, and
getting the correct Kafka bootstrap servers (the latter will be
`localhost` in combination with the random port):

```java
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

    private Map<String, Object> getCommonConfig() {
        final Map<String, Object> map = new HashMap<>();
        map.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        return map;
    }
```

## Using the Builder


### Using ZooKeeper instead of KRaft Mode

By default, the broker is started in KRaft mode as a combined broker
and controller. If your testing requires use of ZooKeeper, you may
disable KRaft mode with this Builder method:

```java
kraftMode(boolean kraftMode)
```

### Specifying Non-random Ports

All network ports will be allocated at random, to avoid collisions
when running multiple tests in parallell on the same system. If you
need specific ports, you may use any of the following Builder methods:

```java
brokerPort(int brokerPort)
controllerPort(int controllerPort)
zooKeeperPort(int zooKeeperPort)
```

### Adding Custom Listeners

If you intend to set up additional listeners, you can request more
random ports like this, giving the number of ports you need:

```java
additionalPorts(int numAdditionalPorts)
```

Then you can specify your listeners using this method, and a port
index between `0` and `numAdditionalPorts - 1`:

```java
additionalListenerWithPortIndex(String name, String securityProtocol, int portIndex)
```

There's a similar method for adding a listener with a fixed port:

```java
additionalListenerWithFixedPort(String name, String securityProtocol, int port)
```

For an example of using an additional listener for setting up SASL,
please see [one of the integration
tests](https://github.com/sverrehu/k3a-embedded/blob/main/src/test/java/no/shhsoft/k3aembedded/AbstractSaslK3aEmbeddedTest.java).

### Adding Custom Broker Configuration

`additionalConfiguration(Map<String, Object> additionalConfiguration)`
`additionalConfigurationProvider(AdditionalConfigurationProvider additionalConfigurationProvider)`
