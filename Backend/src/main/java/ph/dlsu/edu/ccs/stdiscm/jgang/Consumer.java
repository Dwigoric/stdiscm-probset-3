package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consumer is responsible for receiving uploaded videos from producers
 * and saving them to the disk.
 */
public class Consumer {

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
                        executorService.submit(() -> handleUpload(clientChannel));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the upload process for a single video file from a producer.
     *
     * @param clientChannel The channel connected to the producer.
     */
    private static void handleUpload(SocketChannel clientChannel) {
        try {
            // Set up buffers
            ByteBuffer headerBuffer = ByteBuffer.allocate(1024);
            ByteBuffer contentBuffer = ByteBuffer.allocate(4096);

            // Read the file name (first line ending with newline)
            StringBuilder fileNameBuilder = new StringBuilder();
            boolean fileNameComplete = false;
            String fileName = null;
            FileChannel fileChannel = null;

            while (clientChannel.isOpen()) {
                if (!fileNameComplete) {
                    // Read data into the header buffer
                    int bytesRead = clientChannel.read(headerBuffer);
                    // If the channel is closed, break
                    if (bytesRead == -1) break;

                    // Process header data
                    headerBuffer.flip();
                    while (headerBuffer.hasRemaining() && !fileNameComplete) {
                        char c = (char) headerBuffer.get();
                        if (c == '\n') {
                            fileName = fileNameBuilder.toString();
                            fileNameComplete = true;

                            // Create and open the file
                            File file = new File(ConsumerConfig.get("video_directory") + "/" + fileName);
                            fileChannel = new FileOutputStream(file).getChannel();
                        } else {
                            fileNameBuilder.append(c);
                        }
                    }

                    // If we have remaining data after the filename, it's part of the content
                    if (headerBuffer.hasRemaining() && fileNameComplete) {
                        // Create a new buffer with remaining data
                        ByteBuffer remainingData = ByteBuffer.allocate(headerBuffer.remaining());
                        remainingData.put(headerBuffer);
                        remainingData.flip();

                        // Write the remaining data to the file
                        fileChannel.write(remainingData);
                    }

                    headerBuffer.clear();
                } else {
                    // Read file content directly to file
                    int bytesRead = clientChannel.read(contentBuffer);
                    // If the channel is closed, break
                    if (bytesRead == -1) break;

                    // Write content to file
                    contentBuffer.flip();
                    fileChannel.write(contentBuffer);
                    contentBuffer.clear();
                }
            }

            if (fileChannel != null) {
                fileChannel.close();
                System.out.println("Video saved: " + fileName);
            }

            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
