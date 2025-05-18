package server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import server.music.MusicLibrary;
import server.music.MusicLoader;
import server.config.ServerConfig;
import server.music.Song;

/**
 * Point d'entrée principal du serveur MiniSpotify
 */
public class ServerApp {
    // Singleton pour le serveur
    private static volatile ServerApp instance;

    // Configuration du serveur
    private final int port;
    private boolean running;
    private ServerSocket serverSocket;

    /**
     * Constructeur privé (Singleton)
     */
    private ServerApp() {
        this.port = ServerConfig.PORT;
        this.running = false;
    }

    /**
     * Getter pour l'instance Singleton
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
     * Démarrage du serveur
     */
    public void start() {
        // Chargement des chansons via le Singleton MusicLoader
        MusicLoader.getInstance().loadAllSongs();

        try {
            System.out.println("Attempting to start server on port " + port + "...");
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("✅ Server started on port " + port);

            System.out.println("Waiting for client connections...");
            acceptConnections();

        } catch (IOException e) {
            System.err.println("❌ ERROR: Unable to start server");
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    /**
     * Boucle d'acceptation des connexions
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("🔗 Connected client: " + socket.getInetAddress());

                // Création et démarrage d'un thread pour gérer le client
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Arrêt du serveur
     */
    public void shutdown() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server shutdown completed");
            } catch (IOException e) {
                System.err.println("Error closing ServerSocket: " + e.getMessage());
            }
        }
    }

    /**
     * Méthode principale
     */
    public static void main(String[] args) {
        // Chargement des chansons
        MusicLoader.getInstance().loadAllSongs();

        // Mise à jour des chemins de fichiers
        MusicLoader.getInstance().updateSongPaths();
        // Réparation des playlists existantes
        MusicLoader.getInstance().repairExistingPlaylists();

        File mp3Dir = new File("src/main/resources/mp3");
        if (mp3Dir.exists() && mp3Dir.isDirectory()) {
            File[] mp3Files = mp3Dir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

            if (mp3Files != null && mp3Files.length > 0) {
                // Créer un mapping entre les titres courants et les fichiers trouvés
                Map<String, String> titleToPathMap = new HashMap<>();

                for (File file : mp3Files) {
                    String fileName = file.getName().toLowerCase();

                    // Essayer différentes correspondances
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

                // Mettre à jour toutes les chansons dans la bibliothèque
                for (Song song : MusicLibrary.getInstance().getAllSongs()) {
                    String path = titleToPathMap.get(song.getTitle());
                    if (path != null) {
                        song.setFilePath(path);
                        System.out.println("Updated path for song '" + song.getTitle() + "': " + path);
                    }
                }
            }
        }
        // Utilisation du Singleton
        ServerApp.getInstance().start();
    }
}