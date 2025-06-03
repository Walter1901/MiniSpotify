package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        showServerBanner();

        // Initialize music library
        initializeMusicLibrary();

        try {
            serverSocket = new ServerSocket(port);
            running = true;
            showServerStarted();

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
                showServerInterrupted();
            }

        } catch (IOException e) {
            showServerStartError(e.getMessage());
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
                String clientAddress = socket.getInetAddress().getHostAddress();
                showClientConnected(clientAddress);

                // Create and submit client handler to thread pool
                ClientHandler handler = new ClientHandler(socket);
                threadPool.submit(handler);

            } catch (IOException e) {
                if (running) {
                    showConnectionError();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                showUnexpectedError();
            }
        }
    }

    /**
     * Gracefully shutdown the server
     */
    public void shutdown() {
        showShutdownInitiated();
        running = false;

        // Shutdown thread pool gracefully
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Silent close
            }
        }

        showShutdownComplete();
    }

    /**
     * Initialize music library
     */
    private void initializeMusicLibrary() {
        System.out.print("🎵 Loading music library... ");
        MusicLoader.getInstance().loadAllSongs();
        int songCount = MusicLibrary.getInstance().size();
        System.out.println("✅ " + songCount + " songs loaded");
    }

    /**
     * Display server banner
     */
    private void showServerBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                                  ║");
        System.out.println("║                          🎵 MINI SPOTIFY SERVER 🎵                              ║");
        System.out.println("║                                                                                  ║");
        System.out.println("║                              Starting up...                                     ║");
        System.out.println("║                                                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Display server started message
     */
    private void showServerStarted() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                              ✅ SERVER ONLINE                                    ║");
        System.out.println("║                                                                                  ║");
        System.out.printf("║  🌐 Address: localhost:%-8d                                            ║%n", port);
        System.out.printf("║  👥 Max Clients: %-4d                                                      ║%n", ServerConfig.MAX_CLIENTS);
        System.out.printf("║  🕒 Started: %s                                                       ║%n", timestamp);
        System.out.println("║                                                                                  ║");
        System.out.println("║                        🎧 Ready for connections! 🎧                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📋 SERVER LOG:");
        System.out.println("─".repeat(84));
    }

    /**
     * Show client connected
     */
    private void showClientConnected(String clientAddress) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("[%s] 🔗 Client connected: %s%n", timestamp, clientAddress);
    }

    /**
     * Show server start error
     */
    private void showServerStartError(String error) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                              ❌ STARTUP FAILED                                   ║");
        System.out.println("║                                                                                  ║");
        System.out.printf("║  Error: %-72s ║%n", error.length() > 72 ? error.substring(0, 69) + "..." : error);
        System.out.println("║                                                                                  ║");
        System.out.println("║  Possible solutions:                                                            ║");
        System.out.println("║  • Check if port is already in use                                              ║");
        System.out.println("║  • Run with administrator privileges                                            ║");
        System.out.println("║  • Change port in ServerConfig.java                                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Show server interrupted
     */
    private void showServerInterrupted() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("[%s] ⚠️  Server interrupted%n", timestamp);
    }

    /**
     * Show connection error
     */
    private void showConnectionError() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("[%s] ⚠️  Connection error occurred%n", timestamp);
    }

    /**
     * Show unexpected error
     */
    private void showUnexpectedError() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("[%s] ❌ Unexpected error in connection handling%n", timestamp);
    }

    /**
     * Show shutdown initiated
     */
    private void showShutdownInitiated() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println();
        System.out.println("─".repeat(84));
        System.out.printf("[%s] 🔄 Initiating graceful shutdown...%n", timestamp);
    }

    /**
     * Show shutdown complete
     */
    private void showShutdownComplete() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("[%s] ✅ Server shutdown completed%n", timestamp);
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         🛑 MINI SPOTIFY SERVER OFFLINE                          ║");
        System.out.println("║                                                                                  ║");
        System.out.println("║                        Thank you for using MiniSpotify!                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        ServerApp.getInstance().start();
    }
}