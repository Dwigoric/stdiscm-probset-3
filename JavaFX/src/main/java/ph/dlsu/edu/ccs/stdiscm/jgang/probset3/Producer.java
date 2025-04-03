package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Producer is responsible for uploading files to the consumer. It reads
 * video files from the local system and sends them to the consumer over
 * a network socket.
 */
public class Producer {
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
     * Sends the video file to the consumer over a socket connection.
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

        try (Socket socket = new Socket(ProducerConfig.get("server.ip_addr"), port);
             OutputStream out = socket.getOutputStream();
             FileInputStream fileInput = new FileInputStream(videoFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            // Send the filename first (ensure the file name is properly sent)
            out.write((videoFile.getName() + "\n").getBytes());
            out.flush();  // Ensure the filename is fully sent before file content

            // Send the actual file content in chunks
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();  // Ensure the last chunk is fully sent

            System.out.println("Uploaded: " + videoFile.getName());

        } catch (IOException e) {
            System.err.println("Error while uploading: " + videoFile.getName());
            e.printStackTrace();
        }
    }

}