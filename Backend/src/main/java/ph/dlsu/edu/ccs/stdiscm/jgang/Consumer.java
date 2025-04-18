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
import java.util.concurrent.TimeUnit;

/**
 * Consumer is responsible for receiving uploaded videos from producers
 * and saving them to the disk.
 */
public class Consumer {

    private static ExecutorService executorService;
    private static Thread processorThread;

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
        executorService = Executors.newFixedThreadPool(threadCount);

        // Create the folder for storing videos if it doesn't exist
        File folder = new File(ConsumerConfig.get("video_directory"));
        if (!folder.exists()) folder.mkdirs();

        // Start a processor thread to handle videos from the queue
        processorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    VideoFile video = VideoQueue.getVideo();
                    saveVideo(video.getHeader(), video.getClientChannel(), video.getLeftoverBuffer());// blocks until a video is available
                    video.close();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Error processing video: " + e.getMessage());
                }
            }
        });
        processorThread.setDaemon(true);
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
                        executorService.submit(() -> {
                            if (VideoQueue.isFull()) {
                                System.err.println("Queue is full, rejecting connection from " + clientChannel);

                            } else {
                                System.out.println("Accepted connection from " + clientChannel);
                                handleInbound(clientChannel);
                            }
                        });
                    }
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Ensure that the executor service is shut down gracefully
            shutdownExecutorService();
            System.out.println("Consumer shutdown completed.");
        }
    }

    private static void handleInbound(SocketChannel clientChannel) {
        try {
            // Set up buffer for the header only
            ByteBuffer headerBuffer = ByteBuffer.allocate(1024);
            boolean headerComplete = false;
            StringBuilder headerBuilder = new StringBuilder();
            String header = null;
            ByteBuffer leftoverBuffer = null;

            // Read until we get the complete header
            while (!headerComplete && clientChannel.isOpen()) {
                int bytesRead = clientChannel.read(headerBuffer);
                if (bytesRead == -1) break; // Channel closed

                headerBuffer.flip();
                while (headerBuffer.hasRemaining()) {
                    char c = (char) headerBuffer.get();
                    if (c == '\n') {
                        header = headerBuilder.toString();
                        headerComplete = true;
                    } else {
                        headerBuilder.append(c);
                    }
                }
                if (headerComplete) {
                    int remaining = headerBuffer.remaining();
                    leftoverBuffer = ByteBuffer.allocate(remaining);
                    leftoverBuffer.put(headerBuffer); // copy remaining bytes
                    leftoverBuffer.flip(); // prepare for reading
                }
                headerBuffer.clear();
            }

            // If we don't have a header, close channel
            if (header == null) {
                clientChannel.close();
                return;
            }

            // Queue the objects here:
            if (VideoQueue.addVideo(new VideoFile(header, clientChannel, leftoverBuffer))) {
                System.out.println("Video queued: " + header);
            } else {
                System.err.println("Queue is full, unable to add video: " + header);
                clientChannel.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveVideo(String header, SocketChannel clientChannel, ByteBuffer leftoverBuffer) throws IOException {
        if (header.startsWith("fileput:")) {
            // Extract filename
            String filename = header.substring(8).trim();

            if (filename.endsWith(".DS_Store")) {
                return;
            }
            //Problem is here
            File videoFile = new File(ConsumerConfig.get("video_directory"), filename);

            try (FileChannel fileChannel = FileChannel.open(videoFile.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

                long bytesWritten = 0;

                // Write leftover buffer (data after the header)
                if (leftoverBuffer != null && leftoverBuffer.hasRemaining()) {
                    fileChannel.write(leftoverBuffer);
                    bytesWritten += leftoverBuffer.remaining();
                }

                // Continue reading rest of the file from the socket
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                int bytesRead;
                while ((bytesRead = clientChannel.read(buffer)) != -1) {
                    buffer.flip();
                    fileChannel.write(buffer);
                    bytesWritten += bytesRead;
                    buffer.clear();
                }

                System.out.println("Received file: " + filename + " (" + bytesWritten + " bytes)");
            }

            // Acknowledge receipt
            String ackMessage = "Received: " + filename + "\n";
            ByteBuffer ackBuffer = ByteBuffer.wrap(ackMessage.getBytes());
            while (ackBuffer.hasRemaining()) {
                clientChannel.write(ackBuffer);
            }

        } else {
            System.err.println("Unknown header: " + header);
        }
    }

    private static void shutdownExecutorService() {
        // Interrupt the processor thread if it is running
        if (processorThread != null && processorThread.isAlive()) {
            processorThread.interrupt();
        }

        // Gracefully shut down the executor service
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Forcefully shut down after 60 seconds
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
