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
 * Service for handling user authentication - FIXED VERSION
 */
public class AuthenticationService {

    private static final Map<String, AttemptTracker> loginAttempts = new ConcurrentHashMap<>();
    private final Consumer<String> logger;

    public AuthenticationService(Consumer<String> logger) {
        this.logger = logger;
    }

    /**
     * ENHANCED: Handle user login with brute force protection and better error handling
     */
    public LoginResult login(String username, String password) {
        try {
            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                return new LoginResult(false, "Invalid credentials format", null);
            }

            // Normalize username (case insensitive)
            username = username.trim();

            // Check brute force protection
            AttemptTracker tracker = loginAttempts.computeIfAbsent(username.toLowerCase(), k -> new AttemptTracker());

            if (tracker.isLocked()) {
                long remainingSeconds = tracker.getRemainingLockoutSeconds();
                logger.accept("🚫 Blocked login attempt for locked account: " + username);
                return new LoginResult(false,
                        "Account temporarily locked. Try again in " + remainingSeconds + " seconds", null);
            }

            // Find user with retry mechanism
            User matchingUser = null;
            int attempts = 0;
            while (matchingUser == null && attempts < 3) {
                try {
                    List<User> users = UserPersistenceManager.loadUsers();
                    String finalUsername = username;
                    matchingUser = users.stream()
                            .filter(u -> u.getUsername().equalsIgnoreCase(finalUsername))
                            .findFirst()
                            .orElse(null);
                    break;
                } catch (Exception e) {
                    attempts++;
                    if (attempts < 3) {
                        try { Thread.sleep(50); } catch (InterruptedException ie) { break; }
                    }
                }
            }

            if (matchingUser != null) {
                boolean passwordValid = false;
                try {
                    passwordValid = SecurePasswordHasher.checkPassword(password, matchingUser.getPasswordHash());
                } catch (Exception e) {
                    logger.accept("💥 Password verification error for " + username + ": " + e.getMessage());
                    return new LoginResult(false, "Server error during password verification", null);
                }

                if (passwordValid) {
                    // Successful login
                    tracker.recordSuccessfulLogin();

                    // Migrate password if needed
                    if (SecurePasswordHasher.needsMigration(matchingUser.getPasswordHash())) {
                        try {
                            String newHash = SecurePasswordHasher.hashPassword(password);
                            matchingUser.setPasswordHash(newHash);
                            UserPersistenceManager.updateUser(matchingUser);
                            logger.accept("🔄 Password migrated for user: " + username);
                        } catch (Exception e) {
                            logger.accept("⚠️ Password migration failed for " + username + ": " + e.getMessage());
                            // Continue anyway - login should still work
                        }
                    }

                    try {
                        UserPersistenceManager.cleanupInvalidPlaylists(matchingUser);
                    } catch (Exception e) {
                        logger.accept("⚠️ Playlist cleanup failed for " + username + ": " + e.getMessage());
                        // Continue anyway
                    }

                    logger.accept("👤 User logged in: " + username);
                    return new LoginResult(true, "Login successful", matchingUser);

                } else {
                    // Failed login
                    tracker.recordFailedAttempt();

                    int remaining = tracker.getAttemptsRemaining();
                    String message = remaining > 0
                            ? "Incorrect credentials. " + remaining + " attempts remaining"
                            : "Too many failed attempts. Account locked for 15 minutes";

                    logger.accept("❌ Failed login attempt for: " + username +
                            " (attempts: " + tracker.getAttemptCount() + ")");
                    return new LoginResult(false, message, null);
                }
            } else {
                // User not found
                tracker.recordFailedAttempt();
                logger.accept("❌ Login attempt for non-existent user: " + username);
                return new LoginResult(false, "Incorrect credentials", null);
            }

        } catch (Exception e) {
            logger.accept("💥 Login error: " + e.getMessage());
            e.printStackTrace();
            return new LoginResult(false, "Server error during authentication", null);
        }
    }

    /**
     * FIXED: Handle user registration and return the created user
     */
    public RegistrationResult register(String username, String password, String accountType) {
        try {
            if (username == null || password == null || accountType == null ||
                    username.trim().isEmpty() || password.trim().isEmpty()) {
                return new RegistrationResult(false, "Invalid registration data", null);
            }

            // Check if user already exists
            if (UserPersistenceManager.doesUserExist(username)) {
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
                newUser = new FreeUser(username, hashedPassword);
            } else if ("premium".equalsIgnoreCase(accountType)) {
                newUser = new PremiumUser(username, hashedPassword);
            } else {
                return new RegistrationResult(false, "Invalid account type", null);
            }

            // Save user to persistence
            UserPersistenceManager.addUser(newUser);

            // IMPORTANT: Reload the user to ensure it's properly saved
            User savedUser = UserPersistenceManager.getUserByUsername(username);
            if (savedUser == null) {
                return new RegistrationResult(false, "Failed to save user", null);
            }

            logger.accept("👤 User created: " + username + " (" + accountType + ")");

            // FIXED: Return the saved user
            return new RegistrationResult(true, "Account created successfully", savedUser);

        } catch (Exception e) {
            logger.accept("💥 Registration error: " + e.getMessage());
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
        public final User user; // FIXED: Return the created user

        public RegistrationResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
    }
}