package no.shhsoft.k3aembedded;

import org.apache.zookeeper.ZooKeeperMain;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;

import java.nio.file.Path;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
final class ZooKeeper {

    private final int port;
    private Path zkDirectory;
    private ZooKeeperServerMain zooKeeperServerMain;

    public ZooKeeper(final int port) {
        this.port = port;
    }

    public void start() {
        try {
            zkDirectory = FileUtils.createTempDirectory("zk");
            final ServerConfig config = new ServerConfig();
            config.parse(new String[] { String.valueOf(port), zkDirectory.toString() });
            zooKeeperServerMain = new ZooKeeperServerMain() {
                @Override
                protected void serverStarted() {
                    synchronized (this) {
                        notifyAll();
                    }
                }
            };
            final Thread thread = new Thread(() -> {
                try {
                    zooKeeperServerMain.runFromConfig(config);
                    System.out.println("ZooKeeper stopped");
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
            thread.setDaemon(true);
            thread.start();
            synchronized (zooKeeperServerMain) {
                zooKeeperServerMain.wait();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (zooKeeperServerMain == null) {
            return;
        }
        zooKeeperServerMain.close();
        FileUtils.deleteRecursively(zkDirectory.toFile());
        zooKeeperServerMain = null;
    }

    public static void main(final String[] args) {
        final ZooKeeper zk = new ZooKeeper(2181);
        zk.start();
        System.out.println("Started");
        try {
            Thread.sleep(20000L);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Stopping...");
        zk.stop();
        System.out.println("Done stopping");
    }

}
