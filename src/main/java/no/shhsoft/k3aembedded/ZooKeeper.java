package no.shhsoft.k3aembedded;

import org.apache.zookeeper.ZooKeeperMain;

import java.io.IOException;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
final class ZooKeeper {

    private final int port;

    public ZooKeeper(final int port) {
        this.port = port;
    }

    public void start() {
        try {
            ZooKeeperMain.main(new String[] {});
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
    }

    public static void main(final String[] args) {
        final ZooKeeper zk = new ZooKeeper(2181);
        zk.start();
        try {
            Thread.sleep(20000L);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        zk.stop();
    }

}
