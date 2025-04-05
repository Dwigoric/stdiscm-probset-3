package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

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

        try (
                SocketChannel socketChannel = SocketChannel.open(
                        new InetSocketAddress(ProducerConfig.get("server.ip_addr"), port));
                FileChannel fileChannel = FileChannel.open(videoFile.toPath(), StandardOpenOption.READ)
        ) {
            // Send the filename first
            String header = "fileput:" + videoFile.getName() + "\n";
            ByteBuffer filenameBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8));
            while (filenameBuffer.hasRemaining()) {
                socketChannel.write(filenameBuffer);
            }

            // Use transferTo for efficient file transfer
            long position = 0;
            long fileSize = fileChannel.size();
            while (position < fileSize) {
                long bytesTransferred = fileChannel.transferTo(position, fileSize - position, socketChannel);
                if (bytesTransferred == 0) {
                    // Prevent infinite loop if no progress is made
                    break;
                }
                position += bytesTransferred;
            }

            System.out.println("Uploaded: " + videoFile.getName());
        } catch (IOException e) {
            System.err.println("Error while uploading: " + videoFile.getName());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the list of video files from the consumer.
     *
     * @return An array of video file names.
     */
    public static String[] getVideoFiles(String filter) {
        try (
                SocketChannel socketChannel = SocketChannel.open(
                        new InetSocketAddress(ProducerConfig.get("server.ip_addr"), Integer.parseInt(ProducerConfig.get("server.port"))))
        ) {
            // Send the file list request
            String header = "filelist:" + filter + "\n";
            ByteBuffer requestBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8));
            while (requestBuffer.hasRemaining()) {
                socketChannel.write(requestBuffer);
            }

            // Read the response
            ByteBuffer responseBuffer = ByteBuffer.allocate(4096);
            StringBuilder response = new StringBuilder();
            while (socketChannel.read(responseBuffer) > 0) {
                responseBuffer.flip();
                response.append(StandardCharsets.UTF_8.decode(responseBuffer));
                responseBuffer.clear();
            }

            return response.toString().split("\n");
        } catch (IOException e) {
            System.err.println("Error while retrieving video files.");
            e.printStackTrace();
            return new String[0];
        }
    }

    /**
     * Retrieves a specific video file from the consumer.
     *
     * @param filename The name of the video file to retrieve.
     * @return The video file.
     */
    public static File getVideoFile(String filename) {
        try (
                SocketChannel socketChannel = SocketChannel.open(
                        new InetSocketAddress(ProducerConfig.get("server.ip_addr"), Integer.parseInt(ProducerConfig.get("server.port"))))
        ) {
            // Send the file request
            String header = "fileget:" + filename + "\n";
            ByteBuffer requestBuffer = ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8));
            while (requestBuffer.hasRemaining()) {
                socketChannel.write(requestBuffer);
            }

            // Read the response
            ByteBuffer responseBuffer = ByteBuffer.allocate(4096);
            StringBuilder response = new StringBuilder();
            while (socketChannel.read(responseBuffer) > 0) {
                responseBuffer.flip();
                response.append(StandardCharsets.UTF_8.decode(responseBuffer));
                responseBuffer.clear();
            }

            // Save the file to cache
            File videoFile = new File(ProducerConfig.get("cache_directory"), filename);
            try (FileChannel fileChannel = FileChannel.open(videoFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                fileChannel.write(ByteBuffer.wrap(response.toString().getBytes(StandardCharsets.UTF_8)));
            }

            return videoFile;
        } catch (IOException e) {
            System.err.println("Error while retrieving video file: " + filename);
            e.printStackTrace();
            return null;
        }
    }
}