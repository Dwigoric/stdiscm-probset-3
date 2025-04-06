package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consumer is responsible for receiving uploaded videos from producers
 * and saving them to the disk.
 */
public class Consumer {
    private Consumer() {
    }

    public static void main(String[] args) {
        if (!ConsumerConfig.init()) {
            System.err.println("Failed to initialize Consumer configuration.");
            return;
        }

        // Convert the port from String to int
        int port;
        try {
            port = Integer.parseInt(ConsumerConfig.get("port"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in configuration: " + e.getMessage());
            return;
        }
        System.out.println("Consumer running on port " + port);

        int threadCount = Integer.parseInt(ConsumerConfig.get("threads"));
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Create the folder for storing videos if it doesn't exist
        File folder = new File(ConsumerConfig.get("video_directory"));
        if (!folder.exists()) folder.mkdirs();

        // Start background thread to drain the leaky bucket queue
        Runnable queueProcessor = () -> {
            while (true) {
                try {
                    File video = VideoQueue.getVideo(); // blocks if empty
                    executorService.submit(() -> {
                        saveVideo(video);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        };
        Thread processorThread = new Thread(queueProcessor);
        processorThread.start();

        // Start the server to accept incoming connections
        try {
            // Create a selector
            Selector selector = Selector.open();

            // Open a server socket channel
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);

            // Register the channel with the selector for accept operations
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                // Wait for events
                selector.select();

                // Process the events
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        // Accept new connection
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);

                        // Submit to thread pool
                        executorService.submit(() -> handleInbound(clientChannel));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleInbound(SocketChannel clientChannel) {
        try {
            // Set up buffer for the header only
            ByteBuffer headerBuffer = ByteBuffer.allocate(1024);
            boolean headerComplete = false;
            StringBuilder headerBuilder = new StringBuilder(256);
            String header = null;

            // Read until we get the complete header
            while (!headerComplete && clientChannel.isOpen()) {
                int bytesRead = clientChannel.read(headerBuffer);
                if (bytesRead == -1) break; // Channel closed

                headerBuffer.flip();
                while (headerBuffer.hasRemaining() && !headerComplete) {
                    char c = (char) headerBuffer.get();
                    if (c == '\n') {
                        header = headerBuilder.toString();
                        headerComplete = true;
                    } else {
                        headerBuilder.append(c);
                    }
                }
                headerBuffer.clear();
            }

            // If we don't have a header, close channel
            if (header == null) {
                clientChannel.close();
                return;
            }

            // Handle the header and content
            handleHeader(header, clientChannel);

            // Close the client channel
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleHeader(String header, SocketChannel clientChannel) throws IOException {
        if (header.startsWith("fileput:")) {
            // Extract filename from header
            String filename = header.substring(8).trim();
            File videoFile = new File(ConsumerConfig.get("video_directory"), filename);

            // Write the content directly to the file
            try (FileChannel fileChannel = FileChannel.open(videoFile.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

                ByteBuffer buffer = ByteBuffer.allocate(8192);
                long bytesWritten = 0;

                // Read from socket and write to file until end of stream
                while (true) {
                    int bytesRead = clientChannel.read(buffer);
                    if (bytesRead == -1) break;

                    buffer.flip();
                    fileChannel.write(buffer);
                    bytesWritten += bytesRead;
                    buffer.clear();
                }

                System.out.println("Received file: " + filename + " (" + bytesWritten + " bytes)");
            }

            // Add file to queue
            if (!VideoQueue.addVideo(videoFile)) {
                System.err.println("Queue is full, unable to add video: " + filename);
            }

            // Send acknowledgment back to the producer
            String ackMessage = "Received: " + filename + "\n";
            ByteBuffer ackBuffer = ByteBuffer.wrap(ackMessage.getBytes());
            while (ackBuffer.hasRemaining()) {
                clientChannel.write(ackBuffer);
            }
        } else {
            System.err.println("Unknown header: " + header);
        }
    }
    private static void saveVideo(File video) {
        if (video == null || !video.exists()) {
            System.err.println("Invalid video file: " + video);
            return;
        }

        // Create the target directory if it doesn't exist
        File targetDir = new File("../videostorage");
        if (!targetDir.exists()) {
            boolean created = targetDir.mkdirs();
            if (!created) {
                System.err.println("Failed to create ../videostorage directory.");
                return;
            }
        }
        File targetFile = new File(targetDir, video.getName());

        // Move the video to the target directory
        try {
            java.nio.file.Files.copy(
                    video.toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            System.out.println("Video moved to storage: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving video to ../videostorage: " + e.getMessage());
        }
    }

}
