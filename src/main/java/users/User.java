package users;

import java.util.ArrayList;
import java.util.List;
import server.music.Playlist;

public abstract class User {
    protected String username;
    protected String passwordHash;
    protected List<Playlist> playlists = new ArrayList<>();
    protected List<User> followedUsers = new ArrayList<>();
    protected boolean sharePlaylistsPublicly = false; // Option pour partager les playlists publiquement

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getPassword() {
        return passwordHash;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void follow(User user) {
        if (user != null && !followedUsers.contains(user) && !user.equals(this)) {
            followedUsers.add(user);
        }
    }

    public void unfollow(User user) {
        followedUsers.remove(user);
    }

    public boolean isFollowing(User user) {
        return followedUsers.contains(user);
    }

    public boolean isFollowing(String username) {
        return followedUsers.stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    public List<User> getFollowedUsers() {
        return new ArrayList<>(followedUsers); // Copie défensive
    }

    public void setSharePlaylistsPublicly(boolean sharePlaylistsPublicly) {
        this.sharePlaylistsPublicly = sharePlaylistsPublicly;
    }

    public boolean arePlaylistsSharedPublicly() {
        return sharePlaylistsPublicly;
    }

    public abstract String getAccountType();

    public abstract boolean canAddPlaylist();

    public abstract boolean canUseShuffle();

    public String getUsername() {
        return username;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public void addPlaylist(Playlist playlist) {
        if (canAddPlaylist()) {
            playlists.add(playlist);
        } else {
            System.out.println("Playlist limit reached for this user.");
        }
    }

    public Playlist getPlaylistByName(String name) {
        return playlists.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User other = (User) obj;
        return username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

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
}
