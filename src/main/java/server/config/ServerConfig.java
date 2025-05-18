package server.config;

/**
 * Configuration centralisée du serveur
 */
public class ServerConfig {
    // Port du serveur
    public static final int PORT = 12345;

    // Nombre maximum de clients simultanés
    public static final int MAX_CLIENTS = 10;

    // Timeout des connexions (en millisecondes)
    public static final int CONNECTION_TIMEOUT = 60000;

    // Chemin vers les ressources
    public static final String MP3_PATH = "src/main/resources/mp3";

    // Chemin vers les fichiers de persistance
    public static final String USERS_FILE = "users.json";
    public static final String PLAYLISTS_FILE = "playlists.json";

    // Formats de date
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private ServerConfig() {
        // Constructeur privé pour empêcher l'instanciation
    }
}