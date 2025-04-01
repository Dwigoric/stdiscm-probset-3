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
        System.out.println("Consumer running on port " + ConsumerConfig.SERVER_PORT);

        // Create the folder for storing videos if it doesn't exist
        File folder = new File(ConsumerConfig.DOWNLOAD_FOLDER);
        if (!folder.exists()) folder.mkdirs();

        try (ServerSocket serverSocket = new ServerSocket(ConsumerConfig.SERVER_PORT)) {
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

            // Read the first line of the input, which is the filename
            String fileName = reader.readLine();
            if (fileName == null) return;

            File file = new File(ConsumerConfig.DOWNLOAD_FOLDER + "/" + fileName);

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Video saved: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
