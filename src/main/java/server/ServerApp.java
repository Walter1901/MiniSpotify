package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import server.music.MusicLibrary;
import server.music.MusicLoader;
import server.config.ServerConfig;
import server.music.Song;
import server.streaming.StreamingServer;

/**
 * Main entry point for the MiniSpotify server
 */
public class ServerApp {
    // Singleton for the server
    private static volatile ServerApp instance;

    // Server configuration
    private final int port;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private StreamingServer streamingServer;

    /**
     * Private constructor (Singleton)
     */
    private ServerApp() {
        this.port = ServerConfig.PORT;
        this.running = false;
        // Create thread pool for client handlers
        this.threadPool = Executors.newFixedThreadPool(ServerConfig.MAX_CLIENTS);
    }

    /**
     * Getter for Singleton instance
     */
    public static ServerApp getInstance() {
        if (instance == null) {
            synchronized (ServerApp.class) {
                if (instance == null) {
                    instance = new ServerApp();
                }
            }
        }
        return instance;
    }

    /**
     * Server startup
     */
    public void start() {
        // Load songs via MusicLoader Singleton
        MusicLoader.getInstance().loadAllSongs();

        try {
            System.out.println("Attempting to start server on port " + port + "...");
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("✅ Main server started on port " + port);

            // ✨ NOUVEAU: Démarrer le serveur de streaming
            streamingServer = new StreamingServer();
            Thread streamingThread = new Thread(() -> streamingServer.start());
            streamingThread.setDaemon(true);
            streamingThread.start();
            System.out.println("✅ Streaming server started on port " + (port + 1));

            System.out.println("Maximum clients: " + ServerConfig.MAX_CLIENTS);
            System.out.println("Waiting for client connections...");

            // Démarrer le thread d'acceptation des connexions principales
            Thread acceptThread = new Thread(this::acceptConnections);
            acceptThread.setDaemon(true);
            acceptThread.start();

            // Hook d'arrêt modifié
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            try {
                acceptThread.join();
            } catch (InterruptedException e) {
                System.out.println("Server interrupted");
            }

        } catch (IOException e) {
            System.err.println("❌ ERROR: Unable to start server");
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    /**
     * Connection acceptance loop
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("🔗 Connected client: " + socket.getInetAddress());

                // Create and submit client handler to thread pool
                ClientHandler handler = new ClientHandler(socket);
                threadPool.submit(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                    // Short pause to avoid busy-waiting in case of persistent errors
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                // Catch any other exceptions to prevent server from crashing
                System.err.println("Unexpected error in connection acceptance: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Server shutdown
     */
    public void shutdown() {
            running = false;
            System.out.println("Server shutting down...");

            // ✨ NOUVEAU: Arrêter le serveur de streaming
            if (streamingServer != null) {
                streamingServer.stop();
                System.out.println("Streaming server stopped");
            }

            // Arrêter le pool de threads
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
                System.out.println("Thread pool shutdown initiated");
            }

            // Fermer le socket principal
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("Main server socket closed");
                } catch (IOException e) {
                    System.err.println("Error closing ServerSocket: " + e.getMessage());
                }
            }

            System.out.println("✅ Server shutdown completed");
        }

    /**
     * Main method
     */
    public static void main(String[] args) {
        // Load songs
        MusicLoader.getInstance().loadAllSongs();

        // Update file paths
        MusicLoader.getInstance().updateSongPaths();
        // Repair existing playlists
        MusicLoader.getInstance().repairExistingPlaylists();

        File mp3Dir = new File("src/main/resources/mp3");
        if (mp3Dir.exists() && mp3Dir.isDirectory()) {
            File[] mp3Files = mp3Dir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

            if (mp3Files != null && mp3Files.length > 0) {
                // Create mapping between current titles and found files
                Map<String, String> titleToPathMap = new HashMap<>();

                for (File file : mp3Files) {
                    String fileName = file.getName().toLowerCase();

                    // Try different matching approaches
                    if (fileName.contains("mussulo")) {
                        titleToPathMap.put("Mussulo", file.getAbsolutePath());
                    } else if (fileName.contains("ciel")) {
                        titleToPathMap.put("Ciel", file.getAbsolutePath());
                    } else if (fileName.contains("ninao")) {
                        titleToPathMap.put("NINAO", file.getAbsolutePath());
                    } else if (fileName.contains("mood")) {
                        titleToPathMap.put("Mood", file.getAbsolutePath());
                    } else if (fileName.contains("melrose")) {
                        titleToPathMap.put("Melrose Place", file.getAbsolutePath());
                    }
                }

                // Update all songs in the library
                for (Song song : MusicLibrary.getInstance().getAllSongs()) {
                    String path = titleToPathMap.get(song.getTitle());
                    if (path != null) {
                        song.setFilePath(path);
                        System.out.println("Updated path for song '" + song.getTitle() + "': " + path);
                    }
                }
            }
        }

        // Use Singleton to start server
        ServerApp.getInstance().start();
    }
}