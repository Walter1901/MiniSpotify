package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized application logger
 * Provides consistent logging across the entire application
 */
public class AppLogger {

    // Different loggers for different purposes
    private static final Logger MAIN_LOGGER = LoggerFactory.getLogger("MINISPOTIFY");
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY");
    private static final Logger USER_ACTIVITY_LOGGER = LoggerFactory.getLogger("USER_ACTIVITY");

    // Main application logging
    public static void info(String message) {
        MAIN_LOGGER.info(message);
    }

    public static void info(String message, Object... args) {
        MAIN_LOGGER.info(message, args);
    }

    public static void warn(String message) {
        MAIN_LOGGER.warn(message);
    }

    public static void warn(String message, Object... args) {
        MAIN_LOGGER.warn(message, args);
    }

    public static void error(String message) {
        MAIN_LOGGER.error(message);
    }

    public static void error(String message, Throwable throwable) {
        MAIN_LOGGER.error(message, throwable);
    }

    public static void error(String message, Object... args) {
        MAIN_LOGGER.error(message, args);
    }

    public static void debug(String message) {
        MAIN_LOGGER.debug(message);
    }

    public static void debug(String message, Object... args) {
        MAIN_LOGGER.debug(message, args);
    }

    // Security-specific logging
    public static void security(String message) {
        SECURITY_LOGGER.info("🔒 " + message);
    }

    public static void securityWarn(String message) {
        SECURITY_LOGGER.warn("⚠️ " + message);
    }

    public static void securityError(String message) {
        SECURITY_LOGGER.error("🚨 " + message);
    }

    // User activity logging
    public static void userActivity(String username, String action) {
        USER_ACTIVITY_LOGGER.info("User: {} | Action: {}", username, action);
    }

    public static void userActivity(String username, String action, String details) {
        USER_ACTIVITY_LOGGER.info("User: {} | Action: {} | Details: {}", username, action, details);
    }

    // Client connection logging
    public static void clientConnected(String clientAddress) {
        info("🔗 Client connected: {}", clientAddress);
        userActivity("SYSTEM", "CLIENT_CONNECTED", clientAddress);
    }

    public static void clientDisconnected(String clientAddress) {
        info("🔌 Client disconnected: {}", clientAddress);
        userActivity("SYSTEM", "CLIENT_DISCONNECTED", clientAddress);
    }

    // Authentication logging
    public static void loginSuccess(String username) {
        security("Login successful: " + username);
        userActivity(username, "LOGIN_SUCCESS");
    }

    public static void loginFailed(String username, String reason) {
        securityWarn("Login failed for user: " + username + " - " + reason);
        userActivity(username, "LOGIN_FAILED", reason);
    }

    public static void accountLocked(String username) {
        securityError("Account locked due to brute force: " + username);
        userActivity(username, "ACCOUNT_LOCKED", "Too many failed attempts");
    }

    public static void passwordMigrated(String username) {
        security("Password migrated to secure format: " + username);
        userActivity(username, "PASSWORD_MIGRATED");
    }

    // Playlist operations
    public static void playlistCreated(String username, String playlistName) {
        info("📝 Playlist created: {} by {}", playlistName, username);
        userActivity(username, "PLAYLIST_CREATED", playlistName);
    }

    public static void playlistDeleted(String username, String playlistName) {
        info("🗑️ Playlist deleted: {} by {}", playlistName, username);
        userActivity(username, "PLAYLIST_DELETED", playlistName);
    }

    public static void songAddedToPlaylist(String username, String songTitle, String playlistName) {
        info("🎵 Song added: {} to playlist {} by {}", songTitle, playlistName, username);
        userActivity(username, "SONG_ADDED", songTitle + " -> " + playlistName);
    }

    // Server lifecycle
    public static void serverStarted(int port) {
        info("🚀 MiniSpotify server started on port {}", port);
        userActivity("SYSTEM", "SERVER_STARTED", "Port: " + port);
    }

    public static void serverStopped() {
        info("🛑 MiniSpotify server stopped");
        userActivity("SYSTEM", "SERVER_STOPPED");
    }

    public static void serverError(String message, Throwable throwable) {
        error("💥 Server error: {}", message, throwable);
        userActivity("SYSTEM", "SERVER_ERROR", message);
    }

    // Music library
    public static void musicLibraryLoaded(int songCount) {
        info("🎵 Music library loaded: {} songs", songCount);
        userActivity("SYSTEM", "MUSIC_LIBRARY_LOADED", songCount + " songs");
    }

    // Performance monitoring
    public static void performanceLog(String operation, long durationMs) {
        if (durationMs > 1000) { // Log slow operations
            warn("⏱️ Slow operation: {} took {}ms", operation, durationMs);
        } else {
            debug("⏱️ Operation: {} completed in {}ms", operation, durationMs);
        }
    }

    // Utility methods
    public static boolean isDebugEnabled() {
        return MAIN_LOGGER.isDebugEnabled();
    }

    public static boolean isInfoEnabled() {
        return MAIN_LOGGER.isInfoEnabled();
    }

    /**
     * Helper method to time operations
     */
    public static class OperationTimer {
        private final String operation;
        private final long startTime;

        public OperationTimer(String operation) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
            debug("⏱️ Starting operation: {}", operation);
        }

        public void complete() {
            long duration = System.currentTimeMillis() - startTime;
            performanceLog(operation, duration);
        }

        public void completeWithSuccess(String result) {
            long duration = System.currentTimeMillis() - startTime;
            info("✅ Operation completed: {} - {} ({}ms)", operation, result, duration);
        }

        public void completeWithError(String error) {
            long duration = System.currentTimeMillis() - startTime;
            error("❌ Operation failed: {} - {} ({}ms)", operation, error, duration);
        }
    }

    /**
     * Create a timer for an operation
     */
    public static OperationTimer startOperation(String operation) {
        return new OperationTimer(operation);
    }
}