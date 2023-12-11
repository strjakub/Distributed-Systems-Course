import org.apache.zookeeper.*;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import static java.lang.System.exit;

public class ZooWatcher implements Watcher {
    private final ZooKeeper zooKeeper;
    private final String execArg;
    private Process process = null;
    private final PrintStream realOut;

    public ZooWatcher(String host, String arg) throws IOException, KeeperException, InterruptedException {
        PrintStream pw = new PrintStream(new FileOutputStream("logs.txt"));
        this.realOut = System.out;
        System.setOut(pw);
        zooKeeper = new ZooKeeper(host, 5000, null);
        this.execArg = arg;
        zooKeeper.exists("/z", this);
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getPath().startsWith("/z")) {
            try {
                switch (event.getType()) {
                    case NodeDeleted -> {
                        this.realOut.println("Deleted " + event.getPath());
                        if (zooKeeper.exists("/z", false) == null && process != null && process.isAlive()) process.destroy();
                        watchWrapper("/z"); // --
                    }
                    case NodeChildrenChanged -> {
                        this.realOut.println("Changed " + event.getPath());
                        this.realOut.println("\t" + count("/z") + " nodes");
                        if (zooKeeper.exists("/z", false) != null) watchWrapper("/z");
                    }
                    case NodeCreated -> {
                        this.realOut.println("Created " + event.getPath());
                        try {
                            if (process == null || !process.isAlive()) process = Runtime.getRuntime().exec(execArg);
                        } catch (IOException e) {
                            this.realOut.println("Opening app failed");
                        }
                        if (zooKeeper.exists("/z", false) != null) watchWrapper("/z");
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void watchWrapper(String path) throws InterruptedException, KeeperException {
        zooKeeper.exists(path, this, null, null); // --
        watch(path);
    }

    private void watch(String path) throws KeeperException, InterruptedException {
        zooKeeper.getChildren(path, this);
        for (String child : zooKeeper.getChildren(path, this)) {
            watchWrapper(path + "/" + child);
        }
    }

    private int count(String path) throws KeeperException, InterruptedException {
        if(zooKeeper.getAllChildrenNumber(path) == 0) return 1;
        int counter = 0;
        for (String child : zooKeeper.getChildren(path, false)) {
            counter = counter + count(path + "/" + child);
        }
        return counter + 1;
    }

    private void recursivePrint(String path) throws KeeperException, InterruptedException {
        this.realOut.println("\t" + path);
        for (String child : zooKeeper.getChildren(path, false)) {
            String childPath = path + "/" + child;
            recursivePrint(childPath);
        }
    }

    public void startInputReader() throws IOException, KeeperException, InterruptedException {
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String input = inputReader.readLine();
            switch (input) {
                case "tree" -> {
                    if (zooKeeper.exists("/z", false) == null) break;
                    recursivePrint("/z");
                }
                case "exit" -> exit(0);
            }
        }
    }
}
