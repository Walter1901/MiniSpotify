package users;

import java.util.ArrayList;
import java.util.List;
import server.music.Playlist;

public abstract class User {
    protected String username;
    protected String passwordHash;
    protected List<Playlist> playlists = new ArrayList<>();
    protected List<User> followedUsers = new ArrayList<>();

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
        if (!followedUsers.contains(user)) {
            followedUsers.add(user);
        }
    }

    public List<User> getFollowedUsers() {
        return followedUsers;
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
}