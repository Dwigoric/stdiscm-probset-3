package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

/**
 * Producer is responsible for uploading files to the consumer. It reads
 * video files from the local system and sends them to the consumer over
 * a network socket using NIO socket channels.
 */
public class Producer {
    private Producer() {
    }

    public static void main(String[] args) {
        if (!ProducerConfig.init()) {
            System.err.println("Failed to initialize Producer configuration.");
            return;
        }

        int numProducers;
        try {
            numProducers = Integer.parseInt(ProducerConfig.get("threads"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid number of producers specified. Defaulting to 1.");
            numProducers = 1;
        }

        // Define the folder where video files are stored
        File folder = new File(ProducerConfig.get("video_directory"));
        File[] videoFiles = folder.listFiles();

        if (videoFiles == null || videoFiles.length == 0) {
            System.out.println("No video files found in " + ProducerConfig.get("video_directory") + " folder.");
            return;
        }

        // Start sending files to the consumer
        for (int i = 0; i < Math.min(numProducers, videoFiles.length); i++) {
            File videoFile = videoFiles[i];
            new Thread(() -> sendVideoToConsumer(videoFile)).start();
        }
    }

    /**
     * Sends the video file to the consumer over a socket channel connection.
     *
     * @param videoFile The video file to send.
     */
    private static void sendVideoToConsumer(File videoFile) {
        int port;
        try {
            port = Integer.parseInt(ProducerConfig.get("server.port"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in configuration: " + e.getMessage());
            return;
        }

        boolean uploaded = false;

        while (!uploaded) {
            try (
                    SocketChannel socketChannel = SocketChannel.open(
                            new InetSocketAddress(ProducerConfig.get("server.ip_addr"), port));
                    FileChannel fileChannel = FileChannel.open(videoFile.toPath(), StandardOpenOption.READ)
            ) {
                // Step 1: Ask the consumer if the queue has space
                String checkMsg = "queuecheck\n";
                socketChannel.write(ByteBuffer.wrap(checkMsg.getBytes(StandardCharsets.UTF_8)));

                // Step 2: Wait for consumer response
                ByteBuffer responseBuffer = ByteBuffer.allocate(32);
                int bytesRead = socketChannel.read(responseBuffer);
                if (bytesRead <= 0) {
                    System.err.println("No response from consumer for queuecheck. Retrying...");
                    Thread.sleep(1000);
                    continue;
                }

                responseBuffer.flip();
                String response = StandardCharsets.UTF_8.decode(responseBuffer).toString().trim();

                if ("full".equalsIgnoreCase(response)) {
                    System.out.println("Queue is full. Will retry uploading: " + videoFile.getName());
                    Thread.sleep(2000); // Wait before retrying
                    continue;
                }

                // Step 3: Send the file header
                String header = "fileput:" + videoFile.getName() + "\n";
                ByteBuffer filenameBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8));
                while (filenameBuffer.hasRemaining()) {
                    socketChannel.write(filenameBuffer);
                }

                // Step 4: Send file data using transferTo
                long position = 0;
                long fileSize = fileChannel.size();
                while (position < fileSize) {
                    long bytesTransferred = fileChannel.transferTo(position, fileSize - position, socketChannel);
                    if (bytesTransferred == 0) break; // Avoid infinite loop
                    position += bytesTransferred;
                }

                System.out.println("Uploaded: " + videoFile.getName());
                uploaded = true;

            } catch (IOException | InterruptedException e) {
                System.err.println("Error uploading " + videoFile.getName() + ": " + e.getMessage());
                try {
                    Thread.sleep(2000); // Retry after delay
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}