package ph.dlsu.edu.ccs.stdiscm.jgang;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Consumer {
    public static void main(String[] args) {
        System.out.println("📡 Consumer running on port " + Config.SERVER_PORT);

        File folder = new File(Config.VIDEO_FOLDER);
        if (!folder.exists()) folder.mkdirs(); // Ensure storage folder exists

        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleUpload(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleUpload(Socket socket) {
        try (InputStream in = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            String fileName = reader.readLine();
            if (fileName == null) return;

            File file = new File(Config.VIDEO_FOLDER + fileName);

            if (!VideoQueue.addVideo(file)) {
                System.out.println("⚠️ Queue full! Dropping " + fileName);
                return;
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("✅ Video saved: " + file.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
