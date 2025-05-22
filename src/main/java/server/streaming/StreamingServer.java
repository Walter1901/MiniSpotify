package server.streaming;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serveur dédié au streaming audio
 * Fonctionne en parallèle du serveur principal
 */
public class StreamingServer {
    private static final int STREAMING_PORT = 12346; // Port du serveur de streaming
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean isRunning = false;

    // Map pour gérer les sessions de streaming actives
    private final ConcurrentHashMap<String, AudioStreamer> activeStreamers = new ConcurrentHashMap<>();

    public StreamingServer() {
        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Démarre le serveur de streaming
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(STREAMING_PORT);
            isRunning = true;

            System.out.println("🎵 Serveur de streaming démarré sur le port " + STREAMING_PORT);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Nouvelle connexion streaming: " + clientSocket.getInetAddress());

                // Gérer chaque client dans un thread séparé
                threadPool.submit(() -> handleStreamingClient(clientSocket));
            }

        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Erreur du serveur de streaming: " + e.getMessage());
            }
        }
    }

    /**
     * Gère un client de streaming
     */
    private void handleStreamingClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String command;
            AudioStreamer streamer = null;
            String sessionId = clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort();

            while ((command = in.readLine()) != null) {
                String[] parts = command.split(" ", 2);
                String action = parts[0];

                switch (action) {
                    case "STREAM_AUDIO":
                        if (parts.length > 1) {
                            String audioPath = parts[1];

                            // Créer un nouveau streamer
                            streamer = new AudioStreamer(clientSocket, audioPath);
                            activeStreamers.put(sessionId, streamer);

                            // Démarrer le streaming
                            if (streamer.startStreaming()) {
                                System.out.println("🎵 Streaming démarré pour: " + audioPath);
                            } else {
                                System.err.println("❌ Échec du démarrage du streaming pour: " + audioPath);
                            }
                        }
                        break;

                    case "PAUSE_STREAM":
                        if (streamer != null) {
                            streamer.pauseStreaming();
                        }
                        break;

                    case "RESUME_STREAM":
                        if (streamer != null) {
                            streamer.resumeStreaming();
                        }
                        break;

                    case "STOP_STREAM":
                        if (streamer != null) {
                            streamer.stopStreaming();
                            activeStreamers.remove(sessionId);
                            streamer = null;
                        }
                        break;

                    case "GET_POSITION":
                        if (streamer != null) {
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            out.println("POSITION " + streamer.getCurrentPosition());
                        }
                        break;

                    default:
                        System.err.println("Commande streaming inconnue: " + command);
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de la gestion du client streaming: " + e.getMessage());
        } finally {
            // Nettoyer les ressources
            String sessionId = clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort();
            AudioStreamer streamer = activeStreamers.remove(sessionId);
            if (streamer != null) {
                streamer.stopStreaming();
            }

            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignorer
            }
        }
    }

    /**
     * Arrête le serveur de streaming
     */
    public void stop() {
        isRunning = false;

        // Arrêter tous les streamers actifs
        for (AudioStreamer streamer : activeStreamers.values()) {
            streamer.stopStreaming();
        }
        activeStreamers.clear();

        // Fermer le serveur
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture du serveur de streaming: " + e.getMessage());
            }
        }

        // Arrêter le pool de threads
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }

        System.out.println("🛑 Serveur de streaming arrêté");
    }

    /**
     * Point d'entrée pour démarrer le serveur de streaming séparément
     */
    public static void main(String[] args) {
        StreamingServer server = new StreamingServer();

        // Ajouter un hook pour arrêter proprement le serveur
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}