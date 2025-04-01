package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoQueue {
    private static final BlockingQueue<File> queue = new LinkedBlockingQueue<>(Config.MAX_QUEUE_SIZE);

    public static boolean addVideo(File file) {
        return queue.offer(file); // Returns false if full
    }
}
