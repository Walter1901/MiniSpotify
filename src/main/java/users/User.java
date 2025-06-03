package users;

import java.util.ArrayList;
import java.util.List;
import server.music.Playlist;

/**
 * Abstract base class for all user types.
 * Implements Template Method pattern for common user operations.
 *
 * Follows SOLID principles:
 * - SRP: Single responsibility for user data and operations
 * - OCP: Open for extension (new user types), closed for modification
 * - LSP: Subclasses are substitutable for this base class
 */
public abstract class User {
    protected String username;
    protected String passwordHash;
    protected List<Playlist> playlists = new ArrayList<>();
    protected List<User> followedUsers = new ArrayList<>();
    protected boolean sharePlaylistsPublicly = false; // Social feature setting

    /**
     * Constructor for user with credentials
     * @param username Unique username
     * @param passwordHash Hashed password for security
     */
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // Social features implementation

    // ===============================
    // SOCIAL FEATURES - ALL METHODS CORRECTED
    // ===============================

    /**
     * Follow another user with robust validation
     */
    public void follow(User user) {
        if (user != null && !user.equals(this)) {
            // Case-insensitive duplicate check
            boolean alreadyFollowing = followedUsers.stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername()));

            if (!alreadyFollowing) {
                followedUsers.add(user);
                System.out.println("DEBUG: " + this.username + " now follows " + user.getUsername());
            } else {
                System.out.println("DEBUG: " + this.username + " already follows " + user.getUsername());
            }
        }
    }

    /**
     * Unfollow a user with case-insensitive search
     */
    public void unfollow(User user) {
        if (user == null) return;

        // Case-insensitive removal
        User toRemove = followedUsers.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(user.getUsername()))
                .findFirst()
                .orElse(null);

        if (toRemove != null) {
            followedUsers.remove(toRemove);
            System.out.println("DEBUG: " + this.username + " unfollowed " + user.getUsername());
        }
    }

    /**
     * Check if following with case-insensitive comparison
     */
    public boolean isFollowing(User user) {
        if (user == null) return false;

        return followedUsers.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername()));
    }

    /**
     * Check if following by username with case-insensitive comparison
     */
    public boolean isFollowing(String username) {
        if (username == null || username.trim().isEmpty()) return false;

        return followedUsers.stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(username.trim()));
    }

    /**
     * Get playlist by name with case-insensitive search
     */
    public Playlist getPlaylistByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        return playlists.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove playlist with case-insensitive matching
     */
    public boolean removePlaylist(String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            return false;
        }

        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getName().equalsIgnoreCase(playlistName.trim())) {
                playlists.remove(i);
                System.out.println("DEBUG: Removed playlist '" + playlistName + "' from user " + username);
                return true;
            }
        }

        return false;
    }

    /**
     * Validate and clean followed users list
     */
    public void validateFollowedUsers() {
        List<User> validUsers = new ArrayList<>();

        for (User user : followedUsers) {
            if (user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                validUsers.add(user);
            }
        }

        if (validUsers.size() != followedUsers.size()) {
            System.out.println("DEBUG: Cleaned " + (followedUsers.size() - validUsers.size()) +
                    " invalid followed users for " + username);
            followedUsers = validUsers;
        }
    }

    /**
     * Get followed users count for debugging
     */
    public int getFollowedUsersCount() {
        return followedUsers.size();
    }

    /**
     * Debug method for social features
     */
    public void debugSocialFeatures() {
        System.out.println("=== SOCIAL DEBUG for " + username + " ===");
        System.out.println("Following " + followedUsers.size() + " users:");

        for (User user : followedUsers) {
            if (user != null) {
                System.out.println("  -> " + user.getUsername());
            } else {
                System.out.println("  -> [NULL USER]");
            }
        }

        System.out.println("Shares playlists publicly: " + sharePlaylistsPublicly);
        System.out.println("Has " + playlists.size() + " playlists");
    }

    /**
     * Get list of followed users (defensive copy)
     * @return Copy of followed users list
     */
    public List<User> getFollowedUsers() {
        return new ArrayList<>(followedUsers);
    }

    /**
     * Set playlist sharing preference
     * @param sharePlaylistsPublicly true to share publicly, false for private
     */
    public void setSharePlaylistsPublicly(boolean sharePlaylistsPublicly) {
        this.sharePlaylistsPublicly = sharePlaylistsPublicly;
    }

    /**
     * Check if playlists are shared publicly
     * @return true if sharing publicly, false if private
     */
    public boolean arePlaylistsSharedPublicly() {
        return sharePlaylistsPublicly;
    }

    // Abstract methods to be implemented by subclasses (Template Method pattern)

    /**
     * Get the account type (Free, Premium, etc.)
     * @return Account type as string
     */
    public abstract String getAccountType();

    /**
     * Check if user can add more playlists (business rule varies by account type)
     * @return true if can add playlist, false if limit reached
     */
    public abstract boolean canAddPlaylist();

    /**
     * Check if user can use shuffle feature (business rule varies by account type)
     * @return true if shuffle allowed, false otherwise
     */
    public abstract boolean canUseShuffle();

    // Common user operations

    /**
     * Get username
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get password hash
     * @return Hashed password
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Set password hash (for migration and password resets)
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Set user's playlists
     * @param playlists List of playlists to set
     */
    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
    }

    /**
     * Get user's playlists
     * @return List of user's playlists
     */
    public List<Playlist> getPlaylists() {
        return playlists;
    }

    /**
     * Add a playlist if user has permission
     * @param playlist Playlist to add
     */
    public void addPlaylist(Playlist playlist) {
        if (canAddPlaylist()) {
            playlists.add(playlist);
        } else {
            System.out.println("Playlist limit reached for this user type.");
        }
    }


    /**
     * Equality based on username (unique identifier)
     * @param obj Object to compare
     * @return true if same user
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User other = (User) obj;
        return username.equals(other.username);
    }

    /**
     * Hash code based on username
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return username.hashCode();
    }
}