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
            // Set up buffers
            ByteBuffer headerBuffer = ByteBuffer.allocate(1024);
            ByteBuffer contentBuffer = ByteBuffer.allocate(4096);

            StringBuilder headerBuilder = new StringBuilder(256);
            boolean headerComplete = false;
            boolean contentComplete = false;
            String header = null;

            while (clientChannel.isOpen()) {
                if (!headerComplete) {
                    // Read data into the header buffer
                    int bytesRead = clientChannel.read(headerBuffer);
                    // If the channel is closed, break
                    if (bytesRead == -1) break;

                    // Process header data
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

                    // If we have remaining data after the header, it's part of the content
                    if (headerBuffer.hasRemaining()) {
                        // Create a new buffer with remaining data
                        ByteBuffer remainingData = ByteBuffer.allocate(headerBuffer.remaining());
                        remainingData.put(headerBuffer);
                        remainingData.flip();

                        // Put remaining data to the content buffer
                        contentBuffer.clear();
                        contentBuffer.put(remainingData);
                        contentBuffer.flip();
                    }

                    headerBuffer.clear();
                }

                if (!contentComplete) {
                    // Read content data
                    int bytesRead = clientChannel.read(contentBuffer);
                    if (bytesRead == -1) break;

                    // Check if we have received all content
                    if (bytesRead < contentBuffer.capacity()) {
                        contentComplete = true;
                    }
                }

                if (headerComplete && contentComplete) break;
            }

            // If we do not have a header, close channel
            if (header == null) {
                clientChannel.close();
                return;
            }

            // Handle the header and content
            handleHeader(header, contentBuffer, clientChannel);

            // Close the client channel
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleHeader(String header, ByteBuffer contentBuffer, SocketChannel clientChannel) throws IOException {
        if (header.startsWith("fileput:")) {
            // Extract filename from header
            String filename = header.substring(8).trim();
            File videoFile = new File(ConsumerConfig.get("video_directory"), filename);

            // Write the content to the file
            try (FileChannel fileChannel = FileChannel.open(videoFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                // Write the content buffer to the file
                while (contentBuffer.hasRemaining()) {
                    fileChannel.write(contentBuffer);
                }
            }

            // Add the file to the queue
            if (!VideoQueue.addVideo(videoFile)) {
                System.err.println("Queue is full, unable to add video: " + filename);
            }

            // Send acknowledgment back to the producer
            String ackMessage = "Received: " + filename + "\n";
            ByteBuffer ackBuffer = ByteBuffer.wrap(ackMessage.getBytes());
            while (ackBuffer.hasRemaining()) {
                clientChannel.write(ackBuffer);
            }
        } else if (header.startsWith("filelist")) {
            // This is a request from the producer for a list of files
            File videoDir = new File(ConsumerConfig.get("video_directory"));
            String[] videoFiles = videoDir.list();
            if (videoFiles != null) {
                StringBuilder fileList = new StringBuilder("Files:\n");
                for (String file : videoFiles) {
                    fileList.append(file).append("\n");
                }
                ByteBuffer fileListBuffer = ByteBuffer.wrap(fileList.toString().getBytes());
                while (fileListBuffer.hasRemaining()) {
                    clientChannel.write(fileListBuffer);
                }
            } else {
                System.err.println("No files found in directory.");
            }
        } else if (header.startsWith("fileget:")) {
            // This is a request from the producer for a file
            String filename = header.substring(8).trim();
            File videoFile = new File(ConsumerConfig.get("video_directory"), filename);
            if (videoFile.exists()) {
                // Send the file back to the producer
                ByteBuffer fileBuffer = ByteBuffer.allocate((int) videoFile.length());
                try (FileChannel fileChannel = FileChannel.open(videoFile.toPath(), StandardOpenOption.READ)) {
                    fileChannel.read(fileBuffer);
                }
                fileBuffer.flip();
                while (fileBuffer.hasRemaining()) {
                    clientChannel.write(fileBuffer);
                }
            } else {
                System.err.println("Requested file not found: " + filename);
            }
        } else {
            System.err.println("Unknown header: " + header);
        }
    }
}
