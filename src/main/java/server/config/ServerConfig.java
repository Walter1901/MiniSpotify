package server.config;

/**
 * Centralized server configuration.
 * Follows Single Responsibility Principle by only handling configuration.
 * Uses constants to ensure immutability and prevent magic numbers.
 */
public class ServerConfig {
    // Network configuration
    public static final int PORT = 12345;
    public static final int MAX_CLIENTS = 10;
    public static final int CONNECTION_TIMEOUT = 60000; // milliseconds

    // File system paths
    public static final String MP3_PATH = "src/main/resources/mp3";
    public static final String USERS_FILE = "users.json";
    public static final String PLAYLISTS_FILE = "playlists.json";

    // Date and time formats
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Private constructor to prevent instantiation
     */
    private ServerConfig() {
        // Utility class - no instances allowed
    }
}