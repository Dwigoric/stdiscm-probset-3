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
    private static final LinkedBlockingQueue<File> queue = new LinkedBlockingQueue<>(Integer.parseInt(ConsumerConfig.get("queue_size")));

    public static boolean addVideo(File file) {
        return queue.offer(file); // Returns false if full
    }

    public static File getVideo() throws InterruptedException {
        return queue.take(); // Blocks if empty
    }

    public static synchronized boolean isFull() {
        return queue.remainingCapacity() == 0;
    }

    public static synchronized void printQueueContent() {
        System.out.println("-----------------------------");
        System.out.println("Current queue size: " + queue.size());
        System.out.println("Remaining capacity: " + queue.remainingCapacity());
        System.out.println("Queue content: [" + queue.peek() + "]");
        System.out.println("-----------------------------");
    }
}
