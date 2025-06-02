package users;

/**
 * Premium user implementation with full features.
 * Implements business rules specific to premium account type.
 *
 * Follows Liskov Substitution Principle (LSP) - can be used
 * wherever User is expected without breaking functionality.
 */
public class PremiumUser extends User {

    /**
     * Constructor for premium user
     * @param username Unique username
     * @param passwordHash Hashed password
     */
    public PremiumUser(String username, String passwordHash) {
        super(username, passwordHash);
    }

    /**
     * Get account type identifier
     * @return "premium"
     */
    @Override
    public String getAccountType() {
        return "premium";
    }

    /**
     * Premium users have unlimited playlists
     * Business rule specific to premium account type
     * @return true (no playlist limit for premium users)
     */
    @Override
    public boolean canAddPlaylist() {
        return true; // No limit for premium users
    }

    /**
     * Premium users can use all features including shuffle
     * Business rule specific to premium account type
     * @return true (all features available for premium users)
     */
    @Override
    public boolean canUseShuffle() {
        return true; // All features available
    }
}