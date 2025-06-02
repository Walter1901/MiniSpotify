package users;

/**
 * Free user implementation with limited features.
 * Implements business rules specific to free account type.
 *
 * Follows Liskov Substitution Principle (LSP) - can be used
 * wherever User is expected without breaking functionality.
 */
public class FreeUser extends User {

    /**
     * Constructor for free user
     * @param username Unique username
     * @param passwordHash Hashed password
     */
    public FreeUser(String username, String passwordHash) {
        super(username, passwordHash);
    }

    /**
     * Get account type identifier
     * @return "free"
     */
    @Override
    public String getAccountType() {
        return "free";
    }

    /**
     * Free users are limited to 2 playlists maximum
     * Business rule specific to free account type
     * @return true if can add more playlists, false if limit reached
     */
    @Override
    public boolean canAddPlaylist() {
        return playlists.size() < 2; // Free users limited to 2 playlists
    }

    /**
     * Free users cannot use shuffle feature
     * Business rule specific to free account type
     * @return false (shuffle not allowed for free users)
     */
    @Override
    public boolean canUseShuffle() {
        return false; // Premium feature
    }
}