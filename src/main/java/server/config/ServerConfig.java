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


    /**
     * Private constructor to prevent instantiation
     */
    private ServerConfig() {
        // Utility class - no instances allowed
    }
}