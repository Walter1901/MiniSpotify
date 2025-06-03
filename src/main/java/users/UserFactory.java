package users;

import persistence.UserPersistenceManager;
import utils.PasswordHasher;
import utils.SecurePasswordHasher;

/**
 * Factory for creating different types of users.
 * Implements Factory pattern to encapsulate user creation logic.
 *
 * Follows SOLID principles:
 * - SRP: Single responsibility for user creation
 * - OCP: Open for extension (new user types), closed for modification
 * - DIP: Depends on UserPersistenceManager abstraction
 */
public class UserFactory {

    /**
     * Create a user based on account type.
     * Implements Factory pattern by encapsulating creation logic.
     *
     * @param username Unique username
     * @param password Plain text password (will be hashed)
     * @param accountType Type of account ("free" or "premium")
     * @return Created user instance
     * @throws IllegalArgumentException if account type is invalid
     */
    public static User createUser(String username, String password, String accountType) {
        // Hash password for security
        String hashedPassword = SecurePasswordHasher.hashPassword(password);

        User newUser;

        // Factory logic - create appropriate user type
        if (accountType.equalsIgnoreCase("free")) {
            newUser = new FreeUser(username, hashedPassword);
        } else if (accountType.equalsIgnoreCase("premium")) {
            newUser = new PremiumUser(username, hashedPassword);
        } else {
            throw new IllegalArgumentException("Invalid account type: " + accountType +
                    ". Valid types are 'free' or 'premium'.");
        }

        // Check for duplicates before adding to persistence
        if (!UserPersistenceManager.doesUserExist(username)) {
            UserPersistenceManager.addUser(newUser);
        } else {
            System.out.println("User already exists: " + username);
        }

        return newUser;
    }

    /**
     * Create a free user (convenience method)
     * @param username Unique username
     * @param password Plain text password
     * @return Created free user
     */
    public static User createFreeUser(String username, String password) {
        return createUser(username, password, "free");
    }

    /**
     * Create a premium user (convenience method)
     * @param username Unique username
     * @param password Plain text password
     * @return Created premium user
     */
    public static User createPremiumUser(String username, String password) {
        return createUser(username, password, "premium");
    }
}