# k3a-embedded

Embedded Kafka server for running simple integration tests. Embedded
in the sense that the Kafka server is running in the same VM as the
tests. No container runtime required.

## Limitations

* Not at all configurable. Starts a single-node server with no
  security at all.
* No Zookeeper. The server runs in KRaft mode.

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
        <version>0.2.1+${kafka.version}</version>
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
        kafka = new K3aEmbedded();
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
