package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import server.music.MusicLibrary;
import server.music.MusicLoader;
import server.config.ServerConfig;

/**
 * Main server application implementing Singleton pattern.
 * Handles client connections using thread pool for scalability.
 * Follows Single Responsibility Principle - only manages server lifecycle.
 */
public class ServerApp {
    // Singleton instance
    private static volatile ServerApp instance;

    // Server configuration and state
    private final int port;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    /**
     * Private constructor for Singleton pattern
     */
    private ServerApp() {
        this.port = ServerConfig.PORT;
        this.running = false;
        this.threadPool = Executors.newFixedThreadPool(ServerConfig.MAX_CLIENTS);
    }

    /**
     * Get Singleton instance with thread safety
     * @return Single ServerApp instance
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
     * Start the server and begin accepting client connections
     */
    public void start() {
        // Initialize music library using Singleton MusicLoader
        MusicLoader.getInstance().loadAllSongs();

        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("✅ Server started on port " + port);
            System.out.println("Maximum clients: " + ServerConfig.MAX_CLIENTS);

            // Start connection acceptance in separate thread
            Thread acceptThread = new Thread(this::acceptConnections);
            acceptThread.setDaemon(true);
            acceptThread.start();

            // Add graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            // Keep main thread alive
            try {
                acceptThread.join();
            } catch (InterruptedException e) {
                System.out.println("Server interrupted");
            }

        } catch (IOException e) {
            System.err.println("❌ ERROR: Unable to start server");
            System.err.println("Error details: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * Accept incoming client connections in a loop
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();

                // Create and submit client handler to thread pool
                ClientHandler handler = new ClientHandler(socket);
                threadPool.submit(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                System.err.println("Unexpected error in connection acceptance: " + e.getMessage());
            }
        }
    }

    /**
     * Gracefully shutdown the server
     */
    public void shutdown() {
        running = false;

        // Shutdown thread pool
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }

        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing ServerSocket: " + e.getMessage());
            }
        }

        System.out.println("Server shutdown completed");
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        ServerApp.getInstance().start();
    }
}