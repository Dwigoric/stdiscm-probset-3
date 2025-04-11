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
    private static final BlockingQueue<VideoFile> queue = new LinkedBlockingQueue<>(Integer.parseInt(ConsumerConfig.get("queue_size")));

    public static boolean addVideo(VideoFile video) {
        return queue.add(video); // Returns false if full
    }

    public static VideoFile getVideo() throws InterruptedException {
        return queue.take(); // Blocks if empty
    }

    public static synchronized boolean isFull() {
        return queue.remainingCapacity() == 0;
    }
}
