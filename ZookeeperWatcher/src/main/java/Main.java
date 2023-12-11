import org.apache.zookeeper.KeeperException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        if (args.length == 1) {
            ZooWatcher zooWatcher = new ZooWatcher("127.0.0.1:2181", args[0]);
            zooWatcher.startInputReader();
        }
    }

}
