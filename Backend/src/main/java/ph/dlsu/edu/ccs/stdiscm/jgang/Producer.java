package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Producer is responsible for uploading files to the consumer. It reads
 * video files from the local system, and sens them to the consumer over
 * a network socket
 */
public class Producer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter number of producer threads (p): ");
        int numProducers = scanner.nextInt();

        File folder = new File("videos/");
        File[] videoFiles = folder.listFiles();

        if (videoFiles == null || videoFiles.length == 0) {
            System.out.println("⚠️ No video files found in 'videos/' folder.");
            return;
        }

        for (int i = 0; i < Math.min(numProducers, videoFiles.length); i++) {
            File videoFile = videoFiles[i];
            new Thread(() -> sendVideo(videoFile)).start();
        }
    }

    private static void sendVideo(File videoFile) {
        try (Socket socket = new Socket(Config.SERVER_IP, Config.SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             FileInputStream fileInput = new FileInputStream(videoFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            out.write((videoFile.getName() + "\n").getBytes());

            while ((bytesRead = fileInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("✅ Uploaded: " + videoFile.getName());

        } catch (IOException e) {
            System.err.println("❌ Upload error: " + videoFile.getName());
        }
    }
}