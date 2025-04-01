package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Producer is responsible for uploading files to the consumer. It reads
 * video files from the local system and sends them to the consumer over
 * a network socket.
 */
public class Producer {
    public static void main(String[] args) {
        int numProducers = ProducerConfig.NUM_THREADS;

        // Define the folder where video files are stored
        File folder = new File(Config.UPLOAD_FOLDER);
        File[] videoFiles = folder.listFiles();

        if (videoFiles == null || videoFiles.length == 0) {
            System.out.println("No video files found in " + Config.UPLOAD_FOLDER + " folder.");
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
        try (Socket socket = new Socket(ProducerConfig.SERVER_IP, ProducerConfig.SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             FileInputStream fileInput = new FileInputStream(videoFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            // Send the filename first
            out.write((videoFile.getName() + "\n").getBytes());

            // Send the actual file content
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("Uploaded: " + videoFile.getName());

        } catch (IOException e) {
            System.err.println("Error while uploading: " + videoFile.getName());
            e.printStackTrace();
        }
    }
}