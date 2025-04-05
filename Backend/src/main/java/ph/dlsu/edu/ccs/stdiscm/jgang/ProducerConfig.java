package ph.dlsu.edu.ccs.stdiscm.jgang;

public class ProducerConfig {
    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 3005;
    public static final int NUM_THREADS = 3;
    public static final String UPLOAD_FOLDER = "../upload_videos/"; // Storage location
    public static final int MAX_QUEUE_SIZE = 5; // Leaky bucket queue size
}
