# k3a-embedded

Embedded Kafka server for running simple integration tests. Embedded
in the sense that the Kafka server is running in the same VM as the
tests. No container runtime required.

## Limitations

* Only supports starting a single-node "cluster".

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
        <version>0.5.10+${kafka.version}</version>
        <scope>test</scope>
    </dependency>
```

Where `${kafka.version}` is the same version you intend to use for the
kafka-clients library. Since both the server, which uses kafka-clients
internally, and your test code runs in the same VM, the versions must
match.

The following Kafka versions are supported at the moment:

* 4.1.0
* 4.0.0
* 3.9.0
* 3.8.1
* 3.8.0
* 3.7.1
* 3.7.0
* 3.6.1
* 3.6.0
* 3.5.1

### The Classes

This library exposes two classes:

* [`K3aEmbedded`](https://github.com/sverrehu/k3a-embedded/blob/readme/src/main/java/no/shhsoft/k3aembedded/K3aEmbedded.java)
  is the main controller of the Kafka "cluster". To set it up, you use
  the `K3aEmbedded.Builder` class.

* [`K3aTestUtils`](https://github.com/sverrehu/k3a-embedded/blob/readme/src/main/java/no/shhsoft/k3aembedded/K3aTestUtils.java)
  is optional, but contains utilities for avoiding repeating
  often-used constructs.

For JUnit tests, do something like this for starting, stopping, and
getting the correct Kafka bootstrap servers, utilizing both classes
mentioned above:

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

    @Test
    public void shouldProduce() {
        final Producer<Integer, String> producer = new KafkaProducer<>(K3aTestUtils.producerProps(kafka));
        /* ... */
    }
```

## Using the Builder

Use `K3aEmbedded.Builder` to get an instance of `K3aEmbedded`. In its
simplest form with all defaults, it is invoked like this:

```java
new K3aEmbedded.Builder().build();
```

The following sections describe how to override the defaults using
Builder methods. For instance, to disable KRaft mode (enable
ZooKeeper) and specify a broker port, do something like this:

```java
new K3aEmbedded.Builder()
    .kraftMode(false)
    .brokerPort(9092)
    .build();
```

### Using ZooKeeper instead of KRaft Mode

By default, the broker is started in KRaft mode as a combined broker
and controller. If your testing requires use of ZooKeeper, you may
disable KRaft mode with this Builder method:

```java
kraftMode(boolean kraftMode)
```

### Specifying Non-random Ports

All network ports will be allocated at random to avoid collisions
when running multiple tests in parallel on the same system. If you
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

If you need to provide more detailed broker configuration than the
default, the simplest method is to pass a configuration map to this
builder method:

```java
additionalConfiguration(Map<String, Object> additionalConfiguration)
```

The map you provide will override or add to the default broker
configuration.

In the rare cases where your additional configuration relies on data
not available until after the Builder has run, for instance if you
need to know the value of a random port, you may pass an
implementation of `AdditionalConfigurationProvider` to build your map
after the `K3aEmbedded` object has been created:

```java
additionalConfigurationProvider(AdditionalConfigurationProvider additionalConfigurationProvider)
```
