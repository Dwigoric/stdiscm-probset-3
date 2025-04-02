package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class implements a leaky bucket queue, which controls the flow of videos
 * being uploaded by ensuring the queue doesn't exceed a max size. It helps prevent
 * overloading the system with too many videos at once.
 */
public class VideoQueue {
    private static final BlockingQueue<File> queue = new LinkedBlockingQueue<>(ConsumerConfig.MAX_QUEUE_SIZE);

    public static boolean addVideo(File file) {
        return queue.offer(file); // Returns false if full
    }
}
