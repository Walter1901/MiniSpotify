package server.services;

import persistence.UserPersistenceManager;
import server.security.AttemptTracker;
import users.FreeUser;
import users.PremiumUser;
import users.User;
import utils.SecurePasswordHasher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Service for handling user authentication with enhanced security and reliability
 */
public class AuthenticationService {

    private static final Map<String, AttemptTracker> loginAttempts = new ConcurrentHashMap<>();
    private final Consumer<String> logger;

    public AuthenticationService(Consumer<String> logger) {
        this.logger = logger;
    }

    /**
     * Handle user login with robust error handling and security measures
     */
    public LoginResult login(String username, String password) {
        try {
            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                return new LoginResult(false, "Invalid credentials format", null);
            }

            // Consistent normalization for all username operations
            String normalizedUsername = username.trim();
            String lookupKey = normalizedUsername.toLowerCase();

            // Check brute force protection with normalized key
            AttemptTracker tracker = loginAttempts.computeIfAbsent(lookupKey, k -> new AttemptTracker());

            if (tracker.isLocked()) {
                long remainingSeconds = tracker.getRemainingLockoutSeconds();
                logger.accept("🚫 Blocked login attempt for locked account: " + normalizedUsername);
                return new LoginResult(false,
                        "Account temporarily locked. Try again in " + remainingSeconds + " seconds", null);
            }

            // Robust user loading with retry and synchronization
            User matchingUser = findUserWithRetry(normalizedUsername);

            if (matchingUser != null) {
                boolean passwordValid = false;
                try {
                    passwordValid = SecurePasswordHasher.checkPassword(password, matchingUser.getPasswordHash());
                } catch (Exception e) {
                    logger.accept("💥 Password verification error for " + normalizedUsername + ": " + e.getMessage());
                    return new LoginResult(false, "Server error during password verification", null);
                }

                if (passwordValid) {
                    // Immediate tracker reset
                    tracker.recordSuccessfulLogin();

                    // Migrate password if needed
                    migratePasswordIfNeeded(matchingUser, password, normalizedUsername);

                    // Enhanced playlist cleanup with error handling
                    cleanupUserPlaylists(matchingUser, normalizedUsername);

                    // Reload user after modifications to ensure consistency
                    User refreshedUser = UserPersistenceManager.getUserByUsername(normalizedUsername);
                    if (refreshedUser != null) {
                        matchingUser = refreshedUser;
                    }

                    logger.accept("👤 User logged in: " + normalizedUsername);
                    return new LoginResult(true, "Login successful", matchingUser);

                } else {
                    // Failed login
                    tracker.recordFailedAttempt();

                    int remaining = tracker.getAttemptsRemaining();
                    String message = remaining > 0
                            ? "Incorrect credentials. " + remaining + " attempts remaining"
                            : "Too many failed attempts. Account locked for 15 minutes";

                    logger.accept("❌ Failed login attempt for: " + normalizedUsername +
                            " (attempts: " + tracker.getAttemptCount() + ")");
                    return new LoginResult(false, message, null);
                }
            } else {
                // User not found
                tracker.recordFailedAttempt();
                logger.accept("❌ Login attempt for non-existent user: " + normalizedUsername);
                return new LoginResult(false, "Incorrect credentials", null);
            }

        } catch (Exception e) {
            logger.accept("💥 Login error: " + e.getMessage());
            e.printStackTrace();
            return new LoginResult(false, "Server error during authentication", null);
        }
    }

    /**
     * Robust user search with retry and proper error handling
     * Implements progressive backoff on failures
     */
    private User findUserWithRetry(String username) {
        int maxAttempts = 3;
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                synchronized (UserPersistenceManager.class) {
                    List<User> users = UserPersistenceManager.loadUsers();

                    // Case-insensitive search but preserve original username
                    return users.stream()
                            .filter(u -> u.getUsername().equalsIgnoreCase(username))
                            .findFirst()
                            .orElse(null);
                }
            } catch (Exception e) {
                attempts++;
                logger.accept("⚠️ User loading attempt " + attempts + " failed: " + e.getMessage());

                if (attempts < maxAttempts) {
                    try {
                        Thread.sleep(100 * attempts); // Progressive backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Secure password migration
     */
    private void migratePasswordIfNeeded(User user, String password, String username) {
        try {
            if (SecurePasswordHasher.needsMigration(user.getPasswordHash())) {
                String newHash = SecurePasswordHasher.hashPassword(password);
                user.setPasswordHash(newHash);

                synchronized (UserPersistenceManager.class) {
                    UserPersistenceManager.updateUser(user);
                }

                logger.accept("🔄 Password migrated for user: " + username);
            }
        } catch (Exception e) {
            logger.accept("⚠️ Password migration failed for " + username + ": " + e.getMessage());
            // Continue anyway - login should still work
        }
    }

    /**
     * Secure playlist cleanup
     */
    private void cleanupUserPlaylists(User user, String username) {
        try {
            synchronized (UserPersistenceManager.class) {
                UserPersistenceManager.cleanupInvalidPlaylists(user);
            }
        } catch (Exception e) {
            logger.accept("⚠️ Playlist cleanup failed for " + username + ": " + e.getMessage());
            // Continue anyway
        }
    }

    /**
     * Enhanced registration with improved error handling
     */
    public RegistrationResult register(String username, String password, String accountType) {
        try {
            if (username == null || password == null || accountType == null ||
                    username.trim().isEmpty() || password.trim().isEmpty()) {
                return new RegistrationResult(false, "Invalid registration data", null);
            }

            // Consistent normalization
            String normalizedUsername = username.trim();

            // Thread-safe existence check
            synchronized (UserPersistenceManager.class) {
                if (UserPersistenceManager.doesUserExist(normalizedUsername)) {
                    return new RegistrationResult(false, "Username already exists", null);
                }

                // Validate password strength
                if (password.length() < 6) {
                    return new RegistrationResult(false, "Password must be at least 6 characters", null);
                }

                // Create user with SECURE hash
                String hashedPassword = SecurePasswordHasher.hashPassword(password);
                User newUser;

                if ("free".equalsIgnoreCase(accountType)) {
                    newUser = new FreeUser(normalizedUsername, hashedPassword);
                } else if ("premium".equalsIgnoreCase(accountType)) {
                    newUser = new PremiumUser(normalizedUsername, hashedPassword);
                } else {
                    return new RegistrationResult(false, "Invalid account type", null);
                }

                // Thread-safe save
                UserPersistenceManager.addUser(newUser);

                // Save verification with retry
                User savedUser = null;
                int attempts = 0;
                while (savedUser == null && attempts < 3) {
                    savedUser = UserPersistenceManager.getUserByUsername(normalizedUsername);
                    if (savedUser == null) {
                        attempts++;
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }

                if (savedUser == null) {
                    return new RegistrationResult(false, "Failed to save user - please try again", null);
                }

                logger.accept("👤 User created: " + normalizedUsername + " (" + accountType + ")");
                return new RegistrationResult(true, "Account created successfully", savedUser);
            }

        } catch (Exception e) {
            logger.accept("💥 Registration error: " + e.getMessage());
            e.printStackTrace();
            return new RegistrationResult(false, "Server error during registration", null);
        }
    }

    /**
     * Handle user logout
     */
    public void logout(String username) {
        if (username != null) {
            logger.accept("👋 User logged out: " + username);
        }
    }

    // Result classes
    public static class LoginResult {
        public final boolean success;
        public final String message;
        public final User user;

        public LoginResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
    }

    public static class RegistrationResult {
        public final boolean success;
        public final String message;
        public final User user;

        public RegistrationResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
    }
}