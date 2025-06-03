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

    /**
     * Follow another user for social features
     * @param user User to follow
     */
    public void follow(User user) {
        if (user != null && !followedUsers.contains(user) && !user.equals(this)) {
            followedUsers.add(user);
        }
    }

    /**
     * Unfollow a user
     * @param user User to unfollow
     */
    public void unfollow(User user) {
        followedUsers.remove(user);
    }

    /**
     * Check if following a specific user
     * @param user User to check
     * @return true if following, false otherwise
     */
    public boolean isFollowing(User user) {
        return followedUsers.contains(user);
    }

    /**
     * Check if following a user by username
     * @param username Username to check
     * @return true if following, false otherwise
     */
    public boolean isFollowing(String username) {
        return followedUsers.stream()
                .anyMatch(user -> user.getUsername().equals(username));
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
     * Get playlist by name
     * @param name Playlist name to search for
     * @return Playlist if found, null otherwise
     */
    public Playlist getPlaylistByName(String name) {
        return playlists.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove a playlist by name
     * @param playlistName Name of playlist to remove
     * @return true if removed, false if not found
     */
    public boolean removePlaylist(String playlistName) {
        if (playlistName == null || playlistName.isEmpty()) {
            return false;
        }

        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getName().equalsIgnoreCase(playlistName)) {
                playlists.remove(i);
                return true;
            }
        }

        return false;
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