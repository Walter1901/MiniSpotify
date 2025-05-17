package server;

import persistence.UserPersistenceManager;
import server.music.Song;
import server.music.MusicLibrary;
import users.FreeUser;
import users.PremiumUser;
import users.User;
import server.music.Playlist;
import utils.PasswordHasher;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private Socket socket;
    private User loggedInUser; // Stores the connected user

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket s = this.socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            out.println("🎵 Welcome to MiniSpotify server!");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("DEBUG: Received command -> " + line); // Debug display

                if (line.startsWith("LOGIN")) {
                    handleLogin(line, out);
                } else if (line.startsWith("CREATE ")) {
                    handleCreateUser(line, out);
                } else if (line.startsWith("CREATE_PLAYLIST ")) {
                    handleCreatePlaylist(line, out);
                } else if (line.startsWith("GET_PLAYLISTS")) {
                    handleGetPlaylists(out);
                } else if (line.startsWith("GET_ALL_SONGS")) {
                    handleGetAllSongs(out);
                } else if (line.startsWith("ADD_SONG_TO_PLAYLIST ")) {
                    handleAddSongToPlaylist(line, out);
                } else if (line.startsWith("CHECK_PLAYLIST ")) {
                    handleCheckPlaylist(line, out);  // New command handler
                } else {
                    out.println("ERROR: Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Error in client handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLogin(String line, PrintWriter out) {
        String[] parts = line.split(" ");
        if (parts.length != 3) {
            out.println("LOGIN_FAIL Invalid format. Expected: LOGIN username password");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        List<User> users = UserPersistenceManager.loadUsers();
        User matchingUser = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);

        if (matchingUser != null && PasswordHasher.checkPassword(password, matchingUser.getPassword())) {
            loggedInUser = matchingUser;
            out.println("LOGIN_SUCCESS");
            System.out.println("DEBUG: User logged in: " + username);
            System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists");
        } else {
            out.println("LOGIN_FAIL Incorrect username or password");
        }
    }

    private void handleCreateUser(String line, PrintWriter out) {
        String[] parts = line.split(" ");
        if (parts.length != 4) {
            out.println("CREATE_FAIL Invalid arguments. Expected: CREATE username password accountType");
            return;
        }

        String username = parts[1];
        String password = parts[2];
        String accountType = parts[3];

        if (UserPersistenceManager.doesUserExist(username)) {
            out.println("CREATE_FAIL Username already exists");
            return;
        }

        try {
            String hashedPassword = PasswordHasher.hashPassword(password);
            User newUser;

            if ("free".equalsIgnoreCase(accountType)) {
                newUser = new FreeUser(username, hashedPassword);
            } else if ("premium".equalsIgnoreCase(accountType)) {
                newUser = new PremiumUser(username, hashedPassword);
            } else {
                out.println("CREATE_FAIL Invalid account type. Use 'free' or 'premium'");
                return;
            }

            UserPersistenceManager.addUser(newUser);
            out.println("CREATE_SUCCESS");
        } catch (Exception e) {
            System.err.println("❌ Error during CREATE: " + e.getMessage());
            e.printStackTrace();
            out.println("CREATE_FAIL Server error: " + e.getMessage());
        }
    }

    private void handleCreatePlaylist(String line, PrintWriter out) {
        if (loggedInUser == null) {
            out.println("CREATE_PLAYLIST_FAIL No user logged in");
            return;
        }

        String[] parts = line.split(" ", 2);
        if (parts.length != 2) {
            out.println("CREATE_PLAYLIST_FAIL Invalid arguments. Expected: CREATE_PLAYLIST playlistName");
            return;
        }

        String playlistName = parts[1].trim();
        System.out.println("DEBUG: Creating playlist: '" + playlistName + "'");

        if (loggedInUser.canAddPlaylist()) {
            boolean exists = loggedInUser.getPlaylists().stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

            if (!exists) {
                loggedInUser.addPlaylist(new Playlist(playlistName));

                // Update the user in the persistence system
                List<User> allUsers = UserPersistenceManager.loadUsers();
                List<User> updatedUsers = allUsers.stream()
                        .map(u -> u.getUsername().equalsIgnoreCase(loggedInUser.getUsername()) ? loggedInUser : u)
                        .collect(Collectors.toList());

                UserPersistenceManager.saveUsers(updatedUsers);

                // Sync user state after saving
                syncUserState();

                System.out.println("DEBUG: Playlist created: '" + playlistName + "'");
                System.out.println("DEBUG: User now has " + loggedInUser.getPlaylists().size() + " playlists");
                out.println("PLAYLIST_CREATED");
            } else {
                out.println("PLAYLIST_EXISTS");
            }
        } else {
            out.println("CREATE_PLAYLIST_FAIL Limit reached");
        }
    }

    private void handleGetPlaylists(PrintWriter out) {
        if (loggedInUser == null) {
            out.println("ERROR: Not logged in");
            out.println("END");
            return;
        }

        System.out.println("DEBUG: Getting playlists for user: " + loggedInUser.getUsername());
        System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists");

        if (loggedInUser.getPlaylists().isEmpty()) {
            out.println("END");
            return;
        }

        for (Playlist playlist : loggedInUser.getPlaylists()) {
            out.println(playlist.getName());
            System.out.println("DEBUG: Sending playlist: " + playlist.getName());
        }
        out.println("END");
    }

    private void handleGetAllSongs(PrintWriter out) {
        List<Song> songs = MusicLibrary.getInstance().getAllSongs();

        if (songs.isEmpty()) {
            out.println("No songs in the library.");
        } else {
            for (Song song : songs) {
                out.println(song.toString());
            }
        }
        out.println("END");
    }

    /**
     * Method to verify if a playlist exists
     */
    private void handleCheckPlaylist(String line, PrintWriter out) {
        if (loggedInUser == null) {
            out.println("ERROR: Not logged in");
            return;
        }

        // Parse the command
        String[] parts = line.split(" ", 2);
        if (parts.length != 2) {
            out.println("ERROR: Invalid arguments. Expected: CHECK_PLAYLIST playlistName");
            return;
        }

        String playlistName = parts[1].trim();
        System.out.println("DEBUG: Checking for playlist: '" + playlistName + "'");

        // Debug output all playlists
        System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists:");
        for (Playlist p : loggedInUser.getPlaylists()) {
            System.out.println("DEBUG: - '" + p.getName() + "'");
        }

        // Check if the playlist exists
        boolean exists = false;
        for (Playlist p : loggedInUser.getPlaylists()) {
            if (p.getName().equalsIgnoreCase(playlistName)) {
                exists = true;
                break;
            }
        }

        if (exists) {
            out.println("PLAYLIST_FOUND");
        } else {
            out.println("PLAYLIST_NOT_FOUND");
        }
    }

    /**
     * Method to handle synchronization between client and server
     */
    private void syncUserState() {
        // Make sure we have the latest data from persistence
        if (loggedInUser != null) {
            List<User> users = UserPersistenceManager.loadUsers();
            for (User u : users) {
                if (u.getUsername().equals(loggedInUser.getUsername())) {
                    // Update the logged-in user with the latest data from persistence
                    loggedInUser = u;
                    System.out.println("DEBUG: Synchronized user state for: " + loggedInUser.getUsername());
                    System.out.println("DEBUG: User now has " + loggedInUser.getPlaylists().size() + " playlists");
                    break;
                }
            }
        }
    }

    /**
     * Improved method to add songs to playlists
     */
    /**
     * Improved method to add songs to playlists
     */
    private void handleAddSongToPlaylist(String line, PrintWriter out) {
        if (loggedInUser == null) {
            out.println("ERROR: Not logged in");
            return;
        }

        // IMPORTANT: Sync with persistence first
        syncUserState();

        String[] parts = line.split(" ", 3);
        if (parts.length < 3) {
            out.println("ERROR: Invalid arguments. Expected: ADD_SONG_TO_PLAYLIST playlistName songTitle");
            return;
        }

        String playlistName = parts[1].trim();
        String songTitle = parts[2].trim();

        // Find playlist (case-insensitive)
        Playlist found = loggedInUser.getPlaylists().stream()
                .filter(p -> p.getName().equalsIgnoreCase(playlistName))
                .findFirst()
                .orElse(null);

        if (found == null) {
            out.println("Playlist not found.");
            return;
        }

        // Find song
        Song song = MusicLibrary.getInstance().searchByTitle(songTitle).stream()
                .findFirst()
                .orElse(null);

        if (song == null) {
            out.println("Song not found.");
            return;
        }

        // Add song and save changes
        found.addSong(song);

        // Update persistence
        List<User> allUsers = UserPersistenceManager.loadUsers();
        List<User> updatedUsers = allUsers.stream()
                .map(u -> u.getUsername().equalsIgnoreCase(loggedInUser.getUsername()) ? loggedInUser : u)
                .collect(Collectors.toList());
        UserPersistenceManager.saveUsers(updatedUsers);

        out.println("SUCCESS: Song added to playlist.");
    }
}