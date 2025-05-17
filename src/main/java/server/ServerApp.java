package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    public static final int PORT = 12345; // Modified port

    public static void main(String[] args) {
        server.music.MusicLoader.loadAllSongs();
        ServerSocket serverSocket = null;
        try {
            System.out.println("Attempting to start server on port " + PORT + "...");
            serverSocket = new ServerSocket(PORT);
            System.out.println("✅ Server started on port " + PORT);

            System.out.println("Waiting for client connections...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔗 Connected client: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("❌ ERROR: Unable to start server");
            System.out.println("Error details: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("ServerSocket closed");
                } catch (IOException e) {
                    System.out.println("Error closing ServerSocket: " + e.getMessage());
                }
            }
        }
    }
}