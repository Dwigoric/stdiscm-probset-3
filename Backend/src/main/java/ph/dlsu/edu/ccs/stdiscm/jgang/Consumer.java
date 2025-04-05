package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

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

        // Create the folder for storing videos if it doesn't exist
        File folder = new File(ConsumerConfig.get("video_directory"));
        if (!folder.exists()) folder.mkdirs();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();  // Wait for a producer to connect
                new Thread(() -> handleUpload(socket)).start();  // Handle the upload in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the upload process for a single video file from a producer.
     *
     * @param socket The socket connection with the producer.
     */
    private static void handleUpload(Socket socket) {
        try (InputStream in = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            // Read the first line (filename) from the producer
            String fileName = reader.readLine();
            if (fileName == null) return;

            File file = new File(ConsumerConfig.get("video_directory") + "/" + fileName);

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                // Receive and write the file data in chunks
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }

                System.out.println("Video saved: " + file.getName());

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
