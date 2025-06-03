package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import persistence.UserPersistenceManager;
import server.command.ServerCommand;
import server.music.MusicLibrary;
import server.music.Playlist;
import server.music.CollaborativePlaylist;
import server.music.Song;
import server.protocol.ServerProtocol;
import server.config.ServerConfig;
import users.FreeUser;
import users.PremiumUser;
import users.User;
import utils.PasswordHasher;

/**
 * Client handler that processes requests from a connected client
 * Implements Command Factory pattern for handling different client commands
 * Manages user sessions and provides clean logging interface
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private User loggedInUser;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean isRunning = true;

    // Map of commands implementing the Command Factory Pattern
    private Map<String, CommandFactory> commandFactories;

    // Session timer variables
    private java.util.Timer sessionTimer;
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes in milliseconds
    private long lastActivityTime;

    // Static collection to track all active handlers
    private static final Map<String, ClientHandler> activeHandlers = new ConcurrentHashMap<>();

    /**
     * Constructor
     * @param socket Client socket connection
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            // Set connection timeout
            this.socket.setSoTimeout(ServerConfig.CONNECTION_TIMEOUT);
        } catch (Exception e) {
            // Silent fail - connection will be handled in run()
        }
        initializeCommandFactories();
    }

    /**
     * Initialize command factories using Factory pattern
     * Maps command strings to their corresponding factory methods
     */
    private void initializeCommandFactories() {
        commandFactories = new HashMap<>();

        // Authentication commands
        commandFactories.put("LOGIN", this::createLoginCommand);
        commandFactories.put("CREATE", this::createUserCommand);
        commandFactories.put("LOGOUT", args -> out -> {
            loggedInUser = null;
            out.println("LOGOUT_SUCCESS");
            return true;
        });

        // Playlist commands
        commandFactories.put("CREATE_PLAYLIST", this::createPlaylistCommand);
        commandFactories.put("GET_PLAYLISTS", this::getPlaylistsCommand);
        commandFactories.put("CHECK_PLAYLIST", this::checkPlaylistCommand);
        commandFactories.put("ADD_SONG_TO_PLAYLIST", this::addSongToPlaylistCommand);
        commandFactories.put("REMOVE_SONG_FROM_PLAYLIST", this::removeSongFromPlaylistCommand);
        commandFactories.put("REORDER_PLAYLIST_SONG", this::reorderPlaylistSongCommand);
        commandFactories.put("CREATE_COLLAB_PLAYLIST", this::createCollabPlaylistCommand);
        commandFactories.put("DELETE_PLAYLIST", this::deletePlaylistCommand);

        // Music library commands
        commandFactories.put("GET_PLAYLIST_SONGS", this::getPlaylistSongsCommand);
        commandFactories.put("GET_ALL_SONGS", args -> getAllSongsCommand());
        commandFactories.put("SEARCH_TITLE", this::searchTitleCommand);
        commandFactories.put("SEARCH_ARTIST", this::searchArtistCommand);

        // Player commands
        commandFactories.put("LOAD_PLAYLIST", this::loadPlaylistCommand);
        commandFactories.put("SET_PLAYBACK_MODE", this::setPlaybackModeCommand);
        commandFactories.put("PLAYER_PLAY", args -> playerPlayCommand(args));
        commandFactories.put("PLAYER_PAUSE", args -> playerPauseCommand(args));
        commandFactories.put("PLAYER_STOP", args -> playerStopCommand(args));
        commandFactories.put("PLAYER_NEXT", args -> playerNextCommand(args));
        commandFactories.put("PLAYER_PREV", args -> playerPrevCommand(args));
        commandFactories.put("PLAYER_EXIT", args -> playerExitCommand(args));

        // Social commands
        commandFactories.put("FOLLOW_USER", this::followUserCommand);
        commandFactories.put("UNFOLLOW_USER", this::unfollowUserCommand);
        commandFactories.put("GET_FOLLOWED_USERS", this::getFollowedUsersCommand);
        commandFactories.put("GET_SHARED_PLAYLISTS", this::getSharedPlaylistsCommand);
        commandFactories.put("GET_SHARED_PLAYLIST_SONGS", this::getSharedPlaylistSongsCommand);
        commandFactories.put("COPY_SHARED_PLAYLIST", this::copySharedPlaylistCommand);
        commandFactories.put("LOAD_SHARED_PLAYLIST", this::loadSharedPlaylistCommand);
        commandFactories.put("SET_PLAYLIST_SHARING", this::setPlaylistSharingCommand);
    }

    /**
     * Main execution method for the client handler thread
     * Handles client connection lifecycle and command processing
     */
    @Override
    public void run() {
        String clientAddress = socket.getInetAddress().getHostAddress();

        try {
            // Initialize streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Register this handler
            activeHandlers.put(clientAddress, this);

            // Send welcome message
            out.println("🎵 Welcome to MiniSpotify server!");

            // Start session timer
            startSessionTimer();

            String line;
            while (isRunning && (line = in.readLine()) != null) {
                // Refresh last activity time
                lastActivityTime = System.currentTimeMillis();

                try {
                    // Process command in try-catch to prevent server crash
                    processCommand(line);
                } catch (Exception e) {
                    // Send error message to client but don't interrupt connection
                    try {
                        if (out != null && !socket.isClosed()) {
                            out.println("ERROR: Server error processing your request");
                        }
                    } catch (Exception ex) {
                        // Silent fail
                    }
                }
            }

        } catch (SocketException e) {
            // Silent disconnect - normal client termination
        } catch (SocketTimeoutException e) {
            logActivity("⏱️  Connection timed out: " + clientAddress);
        } catch (IOException e) {
            // Silent I/O error - client disconnected unexpectedly
        } catch (Exception e) {
            logActivity("❌ Unexpected error: " + clientAddress);
        } finally {
            // Unregister this handler and clean up
            activeHandlers.remove(clientAddress);
            closeResources();
            logActivity("🔌 Client disconnected: " + clientAddress);
        }
    }

    /**
     * Process a command received from client
     * Uses Command Factory pattern to create and execute commands
     * @param commandLine Raw command line from client
     */
    private void processCommand(String commandLine) {
        try {
            String[] parts = commandLine.split(" ", 2);
            String command = parts[0];
            String args = parts.length > 1 ? parts[1] : "";

            CommandFactory factory = commandFactories.get(command);
            if (factory != null) {
                ServerCommand cmd = factory.createCommand(args);
                cmd.execute(out);
            } else {
                out.println(ServerProtocol.RESP_ERROR + ": Unknown command");
            }
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * Close all resources and cleanup
     * Ensures graceful shutdown of client connection
     */
    private void closeResources() {
        isRunning = false;
        try {
            // Stop timer if it exists
            if (sessionTimer != null) {
                sessionTimer.cancel();
                sessionTimer = null;
            }

            // Close streams and socket
            if (in != null) try { in.close(); } catch (Exception e) { /* ignore */ }
            if (out != null) try { out.close(); } catch (Exception e) { /* ignore */ }
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (Exception e) { /* ignore */ }
            }
        } catch (Exception e) {
            // Silent cleanup
        }
    }

    /**
     * Log activity with timestamp for server monitoring
     * @param message Message to log with timestamp
     */
    private void logActivity(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("[%s] %s%n", timestamp, message);
    }

    /**
     * Synchronize user state with persistence layer
     * Ensures user data is up-to-date across sessions
     */
    private void syncUserState() {
        if (loggedInUser != null) {
            try {
                List<User> users = UserPersistenceManager.loadUsers();
                for (User u : users) {
                    if (u.getUsername().equals(loggedInUser.getUsername())) {
                        loggedInUser = u;
                        break;
                    }
                }
            } catch (Exception e) {
                // Silent fail - continue with current user state
            }
        }
    }

    /**
     * Start session timer for automatic timeout handling
     * Monitors user activity and manages session lifecycle
     */
    public void startSessionTimer() {
        lastActivityTime = System.currentTimeMillis();

        sessionTimer = new java.util.Timer();
        sessionTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    if (!isRunning || socket == null || socket.isClosed() || !socket.isConnected()) {
                        closeResources();
                        cancel();
                        return;
                    }

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastActivityTime > SESSION_TIMEOUT) {
                        loggedInUser = null; // Auto-logout due to inactivity
                    }

                    checkUserSession();
                } catch (Exception e) {
                    // Silent error handling
                }
            }
        }, 10000, 60000); // Check every 60 seconds, first check after 10 seconds
    }

    /**
     * Check and validate current user session
     * Ensures user still exists in persistence layer
     */
    private void checkUserSession() {
        if (loggedInUser == null) {
            return;
        }

        try {
            User updatedUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
            if (updatedUser != null) {
                loggedInUser = updatedUser;
            } else {
                loggedInUser = null; // User no longer exists
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }

    /**
     * Stop this client handler gracefully
     */
    public void stop() {
        isRunning = false;
        closeResources();
    }

    // ===============================
    // COMMAND FACTORY INTERFACE
    // ===============================

    /**
     * Functional interface for Command Factory pattern
     * Creates commands based on arguments
     */
    @FunctionalInterface
    private interface CommandFactory {
        ServerCommand createCommand(String args);
    }

    // ===============================
    // AUTHENTICATION COMMANDS
    // ===============================

    /**
     * Create login command
     * Handles user authentication with password verification
     */
    private ServerCommand createLoginCommand(String args) {
        return out -> {
            try {
                String[] parts = args.split(" ");
                if (parts.length != 2) {
                    out.println(ServerProtocol.RESP_LOGIN_FAIL + " Invalid format");
                    return false;
                }

                String username = parts[0];
                String password = parts[1];

                List<User> users = UserPersistenceManager.loadUsers();

                User matchingUser = users.stream()
                        .filter(u -> u.getUsername().equalsIgnoreCase(username))
                        .findFirst()
                        .orElse(null);

                if (matchingUser != null && PasswordHasher.checkPassword(password, matchingUser.getPasswordHash())) {
                    UserPersistenceManager.cleanupInvalidPlaylists(matchingUser);
                    loggedInUser = matchingUser;
                    out.println(ServerProtocol.RESP_LOGIN_SUCCESS);
                    logActivity("👤 User logged in: " + username);
                    return true;
                }

                out.println(ServerProtocol.RESP_LOGIN_FAIL + " Incorrect credentials");
                return false;
            } catch (Exception e) {
                out.println(ServerProtocol.RESP_LOGIN_FAIL + " Server error");
                return false;
            }
        };
    }

    /**
     * Create user registration command
     * Handles new user account creation with validation
     */
    private ServerCommand createUserCommand(String args) {
        return out -> {
            try {
                String[] parts = args.split(" ");
                if (parts.length != 3) {
                    out.println(ServerProtocol.RESP_CREATE_FAIL + " Invalid arguments");
                    return false;
                }

                String username = parts[0];
                String password = parts[1];
                String accountType = parts[2];

                if (UserPersistenceManager.doesUserExist(username)) {
                    out.println(ServerProtocol.RESP_CREATE_FAIL + " Username already exists");
                    return false;
                }

                try {
                    String hashedPassword = PasswordHasher.hashPassword(password);
                    User newUser;

                    if ("free".equalsIgnoreCase(accountType)) {
                        newUser = new FreeUser(username, hashedPassword);
                    } else if ("premium".equalsIgnoreCase(accountType)) {
                        newUser = new PremiumUser(username, hashedPassword);
                    } else {
                        out.println(ServerProtocol.RESP_CREATE_FAIL + " Invalid account type");
                        return false;
                    }

                    UserPersistenceManager.addUser(newUser);
                    out.println(ServerProtocol.RESP_CREATE_SUCCESS);
                    logActivity("👤 User created: " + username + " (" + accountType + ")");
                    return true;
                } catch (Exception e) {
                    out.println(ServerProtocol.RESP_CREATE_FAIL + " Server error");
                    return false;
                }
            } catch (Exception e) {
                out.println(ServerProtocol.RESP_CREATE_FAIL + " Server error");
                return false;
            }
        };
    }

    // ===============================
    // PLAYLIST MANAGEMENT COMMANDS
    // ===============================

    /**
     * Create standard playlist command
     * Creates a new playlist for the logged-in user
     */
    private ServerCommand createPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("CREATE_PLAYLIST_FAIL No user logged in");
                    return false;
                }

                String playlistName = args.trim();

                if (loggedInUser.canAddPlaylist()) {
                    boolean exists = loggedInUser.getPlaylists().stream()
                            .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

                    if (!exists) {
                        loggedInUser.addPlaylist(new Playlist(playlistName));
                        UserPersistenceManager.updateUser(loggedInUser);
                        out.println(ServerProtocol.RESP_PLAYLIST_CREATED);
                        return true;
                    } else {
                        out.println(ServerProtocol.RESP_PLAYLIST_EXISTS);
                        return false;
                    }
                } else {
                    out.println("CREATE_PLAYLIST_FAIL Playlist limit reached");
                    return false;
                }
            } catch (Exception e) {
                out.println("CREATE_PLAYLIST_FAIL Server error");
                return false;
            }
        };
    }

    /**
     * Create collaborative playlist command
     * Creates a playlist that can be edited by multiple users
     */
    private ServerCommand createCollabPlaylistCommand(String args) {
        return out -> {
            try {
                checkUserSession();

                if (loggedInUser == null) {
                    out.println("CREATE_COLLAB_PLAYLIST_FAIL No user logged in");
                    return false;
                }

                String[] parts = args.split(" ", 2);
                if (parts.length < 1) {
                    out.println("CREATE_COLLAB_PLAYLIST_FAIL Invalid format");
                    return false;
                }

                String playlistName = parts[0];

                if (loggedInUser.canAddPlaylist()) {
                    boolean exists = loggedInUser.getPlaylists().stream()
                            .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

                    if (!exists) {
                        try {
                            CollaborativePlaylist playlist = new CollaborativePlaylist(playlistName, loggedInUser);

                            // Process collaborators if provided
                            if (parts.length > 1 && parts[1] != null && !parts[1].trim().isEmpty()) {
                                String[] collaboratorNames = parts[1].split(",");
                                for (String collaboratorName : collaboratorNames) {
                                    collaboratorName = collaboratorName.trim();
                                    if (!collaboratorName.isEmpty()) {
                                        try {
                                            User collaborator = UserPersistenceManager.getUserByUsername(collaboratorName);
                                            if (collaborator != null) {
                                                playlist.addCollaborator(collaborator);
                                            }
                                        } catch (Exception e) {
                                            // Continue processing other collaborators
                                        }
                                    }
                                }
                            }

                            loggedInUser.addPlaylist(playlist);
                            UserPersistenceManager.updateUser(loggedInUser);
                            out.println("COLLAB_PLAYLIST_CREATED");
                            return true;

                        } catch (Exception e) {
                            out.println("CREATE_COLLAB_PLAYLIST_FAIL Server error");
                            return false;
                        }
                    } else {
                        out.println("CREATE_COLLAB_PLAYLIST_FAIL Playlist already exists");
                        return false;
                    }
                } else {
                    out.println("CREATE_COLLAB_PLAYLIST_FAIL Playlist limit reached");
                    return false;
                }
            } catch (Exception e) {
                out.println("CREATE_COLLAB_PLAYLIST_FAIL Server error");
                return false;
            }
        };
    }

    /**
     * Get user's playlists command
     * Returns list of all playlists owned by the current user
     */
    private ServerCommand getPlaylistsCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                    out.println(ServerProtocol.RESP_END);
                    return false;
                }

                syncUserState();

                if (loggedInUser.getPlaylists().isEmpty()) {
                    out.println(ServerProtocol.RESP_END);
                    return true;
                }

                for (Playlist playlist : loggedInUser.getPlaylists()) {
                    out.println(playlist.getName());
                }
                out.println(ServerProtocol.RESP_END);
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println(ServerProtocol.RESP_END);
                return false;
            }
        };
    }

    /**
     * Check if playlist exists command
     * Verifies if a playlist with given name exists for current user
     */
    private ServerCommand checkPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                    return false;
                }

                String playlistName = args.trim();
                syncUserState();

                boolean exists = loggedInUser.getPlaylists().stream()
                        .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

                if (exists) {
                    out.println(ServerProtocol.RESP_PLAYLIST_FOUND);
                    return true;
                } else {
                    out.println(ServerProtocol.RESP_PLAYLIST_NOT_FOUND);
                    return false;
                }
            } catch (Exception e) {
                out.println(ServerProtocol.RESP_ERROR + ": Server error");
                return false;
            }
        };
    }

    /**
     * Delete playlist command
     * Removes a playlist from user's collection
     */
    private ServerCommand deletePlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String playlistName = args.trim();

                if (loggedInUser.removePlaylist(playlistName)) {
                    UserPersistenceManager.updateUser(loggedInUser);
                    out.println("SUCCESS: Playlist deleted successfully");
                    return true;
                } else {
                    out.println("ERROR: Playlist not found");
                    return false;
                }

            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Get songs in a playlist command
     * Returns all songs in the specified playlist
     */
    private ServerCommand getPlaylistSongsCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    out.println("END");
                    return false;
                }

                String playlistName = args.trim();

                // Find playlist
                Playlist foundPlaylist = null;
                for (Playlist p : loggedInUser.getPlaylists()) {
                    if (p.getName().equalsIgnoreCase(playlistName)) {
                        foundPlaylist = p;
                        break;
                    }
                }

                if (foundPlaylist == null) {
                    out.println("ERROR: Playlist not found");
                    out.println("END");
                    return false;
                }

                // Get songs
                List<Song> songs = foundPlaylist.getSongs();

                if (songs.isEmpty()) {
                    out.println("SUCCESS: Playlist is empty");
                    out.println("END");
                    return true;
                }

                out.println("SUCCESS: Found " + songs.size() + " songs");

                // Send each song with complete information
                for (Song song : songs) {
                    // Look for complete version of song in library
                    String songTitle = song.getTitle();
                    Song librarySong = null;

                    for (Song s : MusicLibrary.getInstance().getAllSongs()) {
                        if (s.getTitle().equals(songTitle)) {
                            librarySong = s;
                            break;
                        }
                    }

                    // Send song data in pipe-separated format
                    if (librarySong != null) {
                        out.println(librarySong.getTitle() + "|" +
                                librarySong.getArtist() + "|" +
                                librarySong.getAlbum() + "|" +
                                librarySong.getGenre() + "|" +
                                librarySong.getDuration() + "|" +
                                (librarySong.getFilePath() != null ? librarySong.getFilePath() : ""));
                    } else {
                        out.println(song.getTitle() + "|" +
                                song.getArtist() + "|" +
                                song.getAlbum() + "|" +
                                song.getGenre() + "|" +
                                song.getDuration() + "|" +
                                (song.getFilePath() != null ? song.getFilePath() : ""));
                    }
                }

                out.println("END");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    // ===============================
    // SONG MANAGEMENT COMMANDS
    // ===============================

    /**
     * Add song to playlist command
     * Adds a song from the music library to a specific playlist
     */
    private ServerCommand addSongToPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String[] parts = args.split(" ", 2);
                if (parts.length < 2) {
                    out.println("ERROR: Invalid arguments");
                    return false;
                }

                String playlistName = parts[0].trim();
                String songTitle = parts[1].trim();

                // Find playlist
                Playlist found = null;
                for (Playlist p : loggedInUser.getPlaylists()) {
                    if (p.getName().equalsIgnoreCase(playlistName)) {
                        found = p;
                        break;
                    }
                }

                if (found == null) {
                    out.println("ERROR: Playlist not found");
                    return false;
                }

                // Find song in library with fuzzy matching
                Song originalSong = null;

                // First try exact match (case insensitive)
                for (Song s : MusicLibrary.getInstance().getAllSongs()) {
                    if (s.getTitle().equalsIgnoreCase(songTitle)) {
                        originalSong = s;
                        break;
                    }
                }

                // If no exact match, try partial match
                if (originalSong == null) {
                    for (Song s : MusicLibrary.getInstance().getAllSongs()) {
                        if (s.getTitle().toLowerCase().contains(songTitle.toLowerCase()) ||
                                songTitle.toLowerCase().contains(s.getTitle().toLowerCase())) {
                            originalSong = s;
                            break;
                        }
                    }
                }

                if (originalSong == null) {
                    out.println("ERROR: Song not found in library");
                    return false;
                }

                // Check if song is already in playlist
                for (Song existingSong : found.getSongs()) {
                    if (existingSong.getTitle().equalsIgnoreCase(originalSong.getTitle())) {
                        out.println("INFO: Song is already in the playlist");
                        return true;
                    }
                }

                // Create a complete copy of the song to add to the playlist
                Song songCopy = new Song(
                        originalSong.getTitle(),
                        originalSong.getArtist(),
                        originalSong.getAlbum(),
                        originalSong.getGenre(),
                        originalSong.getDuration()
                );
                songCopy.setFilePath(originalSong.getFilePath());

                // Add to playlist and save
                found.addSong(songCopy);
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: Song added to playlist");
                return true;

            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Remove song from playlist command
     * Removes a song from the specified playlist
     */
    private ServerCommand removeSongFromPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                    return false;
                }

                String[] parts = args.split(" ", 2);
                if (parts.length < 2) {
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid arguments");
                    return false;
                }

                String playlistName = parts[0].trim();
                String songTitle = parts[1].trim();

                // Find playlist
                Playlist found = loggedInUser.getPlaylists().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(playlistName))
                        .findFirst()
                        .orElse(null);

                if (found == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Playlist not found");
                    return false;
                }

                // Remove song
                boolean removed = found.removeSong(songTitle);

                if (removed) {
                    UserPersistenceManager.updateUser(loggedInUser);
                    out.println(ServerProtocol.RESP_SUCCESS + ": Song removed from playlist");
                    return true;
                } else {
                    out.println(ServerProtocol.RESP_ERROR + ": Song not found in playlist");
                    return false;
                }
            } catch (Exception e) {
                out.println(ServerProtocol.RESP_ERROR + ": Server error");
                return false;
            }
        };
    }

    /**
     * Reorder songs in playlist command
     * Changes the position of a song within a playlist
     */
    private ServerCommand reorderPlaylistSongCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                    return false;
                }

                String[] parts = args.split(" ");
                if (parts.length != 3) {
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid arguments");
                    return false;
                }

                try {
                    String playlistName = parts[0];
                    int fromIndex = Integer.parseInt(parts[1]);
                    int toIndex = Integer.parseInt(parts[2]);

                    // Find playlist
                    Playlist foundPlaylist = null;
                    for (Playlist p : loggedInUser.getPlaylists()) {
                        if (p.getName().equalsIgnoreCase(playlistName)) {
                            foundPlaylist = p;
                            break;
                        }
                    }

                    if (foundPlaylist == null) {
                        out.println(ServerProtocol.RESP_ERROR + ": Playlist not found");
                        return false;
                    }

                    // Validate indices
                    if (fromIndex < 0 || toIndex < 0 || fromIndex >= foundPlaylist.size() || toIndex >= foundPlaylist.size()) {
                        out.println(ServerProtocol.RESP_ERROR + ": Invalid indices");
                        return false;
                    }

                    // Move song
                    foundPlaylist.moveSong(fromIndex, toIndex);
                    UserPersistenceManager.updateUser(loggedInUser);

                    out.println(ServerProtocol.RESP_SUCCESS + ": Song reordered");
                    return true;
                } catch (NumberFormatException e) {
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid indices format");
                    return false;
                }
            } catch (Exception e) {
                out.println(ServerProtocol.RESP_ERROR + ": Server error");
                return false;
            }
        };
    }

    // ===============================
    // MUSIC LIBRARY COMMANDS
    // ===============================

    /**
     * Get all songs command
     * Returns all songs available in the music library
     */
    private ServerCommand getAllSongsCommand() {
        return out -> {
            try {
                List<Song> songs = MusicLibrary.getInstance().getAllSongs();

                if (songs.isEmpty()) {
                    out.println("No songs in the library.");
                } else {
                    for (Song song : songs) {
                        out.println(song.toString());
                    }
                }
                out.println("END");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Search songs by title command
     * Searches for songs containing the specified title
     */
    private ServerCommand searchTitleCommand(String args) {
        return out -> {
            try {
                String title = args.trim();
                List<Song> results = MusicLibrary.getInstance().searchByTitle(title);

                if (results.isEmpty()) {
                    out.println("No songs found with title: " + title);
                } else {
                    for (Song song : results) {
                        out.println(song.toString());
                    }
                }
                out.println(ServerProtocol.RESP_END);
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println(ServerProtocol.RESP_END);
                return false;
            }
        };
    }

    /**
     * Search songs by artist command
     * Searches for songs by the specified artist
     */
    private ServerCommand searchArtistCommand(String args) {
        return out -> {
            try {
                String artist = args.trim();
                List<Song> results = MusicLibrary.getInstance().filterByArtist(artist);

                if (results.isEmpty()) {
                    out.println("No songs found by artist: " + artist);
                } else {
                    for (Song song : results) {
                        out.println(song.toString());
                    }
                }
                out.println(ServerProtocol.RESP_END);
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println(ServerProtocol.RESP_END);
                return false;
            }
        };
    }

    // ===============================
    // PLAYER COMMANDS
    // ===============================

    /**
     * Load playlist for playback command
     * Prepares a playlist for audio playback
     */
    private ServerCommand loadPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String playlistName = args.trim();

                // Find playlist in user's playlists
                Playlist foundPlaylist = null;
                for (Playlist p : loggedInUser.getPlaylists()) {
                    if (p.getName().equalsIgnoreCase(playlistName)) {
                        foundPlaylist = p;
                        break;
                    }
                }

                if (foundPlaylist == null) {
                    out.println("ERROR: Playlist not found");
                    return false;
                }

                out.println("SUCCESS: Playlist loaded successfully");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Set playback mode command
     * Sets the playback mode (sequential, shuffle, repeat)
     */
    private ServerCommand setPlaybackModeCommand(String args) {
        return out -> {
            try {
                String modeChoice = args.trim();

                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                boolean validMode = true;
                switch (modeChoice) {
                    case "1": // Sequential
                    case "2": // Shuffle
                    case "3": // Repeat
                        break;
                    default:
                        validMode = false;
                }

                if (validMode) {
                    out.println("SUCCESS: Playback mode set");
                    return true;
                } else {
                    out.println("ERROR: Invalid playback mode");
                    return false;
                }
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Player play command
     * Handles play requests from the client
     */
    private ServerCommand playerPlayCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                out.println("▶️ Playing music...");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Player pause command
     * Handles pause requests from the client
     */
    private ServerCommand playerPauseCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                out.println("⏸️ Music paused");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Player stop command
     * Handles stop requests from the client
     */
    private ServerCommand playerStopCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                out.println("⏹️ Music stopped");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Player next command
     * Handles next track requests from the client
     */
    private ServerCommand playerNextCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                out.println("⏭️ Next song");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Player previous command
     * Handles previous track requests from the client
     */
    private ServerCommand playerPrevCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                out.println("⏮️ Previous song");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Player exit command
     * Handles exit player mode requests from the client
     */
    private ServerCommand playerExitCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                out.println("Exiting player mode");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    // ===============================
    // SOCIAL FEATURES COMMANDS
    // ===============================

    /**
     * Follow user command
     * Allows current user to follow another user
     */
    private ServerCommand followUserCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String usernameToFollow = args.trim();

                // Check user is not following themselves
                if (loggedInUser.getUsername().equals(usernameToFollow)) {
                    out.println("ERROR: You cannot follow yourself");
                    return false;
                }

                // Check if user exists
                User userToFollow = UserPersistenceManager.getUserByUsername(usernameToFollow);
                if (userToFollow == null) {
                    out.println("ERROR: User not found");
                    return false;
                }

                // Check if already following
                if (loggedInUser.isFollowing(usernameToFollow)) {
                    out.println("INFO: You are already following this user");
                    return true;
                }

                // Follow user
                loggedInUser.follow(userToFollow);
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: You are now following " + usernameToFollow);
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Unfollow user command
     * Allows current user to unfollow another user
     */
    private ServerCommand unfollowUserCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String usernameToUnfollow = args.trim();

                // Check if user exists
                User userToUnfollow = UserPersistenceManager.getUserByUsername(usernameToUnfollow);
                if (userToUnfollow == null) {
                    out.println("ERROR: User not found");
                    return false;
                }

                // Check if user is followed
                if (!loggedInUser.isFollowing(usernameToUnfollow)) {
                    out.println("INFO: You are not following this user");
                    return true;
                }

                // Unfollow user
                loggedInUser.unfollow(userToUnfollow);
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: You are no longer following " + usernameToUnfollow);
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Get followed users command
     * Returns list of users that current user is following
     */
    private ServerCommand getFollowedUsersCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    out.println("END");
                    return false;
                }

                List<User> followedUsers = loggedInUser.getFollowedUsers();

                if (followedUsers.isEmpty()) {
                    out.println("END");
                    return true;
                }

                // Send list of followed users
                for (User user : followedUsers) {
                    out.println(user.getUsername());
                }

                out.println("END");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Get shared playlists command
     * Returns playlists shared by users that current user follows
     */
    private ServerCommand getSharedPlaylistsCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    out.println("END");
                    return false;
                }

                List<User> followedUsers = loggedInUser.getFollowedUsers();

                // Check each followed user for shared playlists
                for (User followedUser : followedUsers) {
                    if (followedUser.arePlaylistsSharedPublicly()) {
                        for (Playlist playlist : followedUser.getPlaylists()) {
                            out.println(playlist.getName() + "|" + followedUser.getUsername());
                        }
                    }
                }

                out.println("END");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Get shared playlist songs command
     * Returns songs from a specific shared playlist
     */
    private ServerCommand getSharedPlaylistSongsCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    out.println("END");
                    return false;
                }

                String[] parts = args.split(" ", 2);
                if (parts.length != 2) {
                    out.println("ERROR: Invalid arguments");
                    out.println("END");
                    return false;
                }

                String ownerUsername = parts[0];
                String playlistName = parts[1];

                // Check if user exists
                User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
                if (owner == null) {
                    out.println("ERROR: User not found");
                    out.println("END");
                    return false;
                }

                // Check if user is followed and playlists are shared
                if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                    out.println("ERROR: You cannot access this playlist");
                    out.println("END");
                    return false;
                }

                // Find playlist
                Playlist playlist = owner.getPlaylistByName(playlistName);
                if (playlist == null) {
                    out.println("ERROR: Playlist not found");
                    out.println("END");
                    return false;
                }

                out.println("SUCCESS: Found playlist");

                // Send songs in pipe-separated format
                for (Song song : playlist.getSongs()) {
                    out.println(song.getTitle() + "|" +
                            song.getArtist() + "|" +
                            song.getAlbum() + "|" +
                            song.getGenre() + "|" +
                            song.getDuration() + "|" +
                            (song.getFilePath() != null ? song.getFilePath() : ""));
                }

                out.println("END");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Copy shared playlist command
     * Creates a copy of a shared playlist in current user's library
     */
    private ServerCommand copySharedPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String[] parts = args.split(" ", 3);
                if (parts.length != 3) {
                    out.println("ERROR: Invalid arguments");
                    return false;
                }

                String ownerUsername = parts[0];
                String sourcePlaylistName = parts[1];
                String newPlaylistName = parts[2];

                // Check if user exists
                User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
                if (owner == null) {
                    out.println("ERROR: User not found");
                    return false;
                }

                // Check if user is followed
                if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                    out.println("ERROR: You cannot access this playlist");
                    return false;
                }

                // Find source playlist
                Playlist sourcePlaylist = owner.getPlaylistByName(sourcePlaylistName);
                if (sourcePlaylist == null) {
                    out.println("ERROR: Playlist not found");
                    return false;
                }

                // Check if playlist with new name already exists
                if (loggedInUser.getPlaylistByName(newPlaylistName) != null) {
                    out.println("ERROR: A playlist with this name already exists");
                    return false;
                }

                // Check if user can add more playlists
                if (!loggedInUser.canAddPlaylist()) {
                    out.println("ERROR: You have reached your playlist limit");
                    return false;
                }

                // Create new playlist with same songs
                Playlist newPlaylist = new Playlist(newPlaylistName);
                for (Song song : sourcePlaylist.getSongs()) {
                    newPlaylist.addSong(song);
                }

                // Add playlist to user
                loggedInUser.addPlaylist(newPlaylist);
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: Playlist copied successfully");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Load shared playlist command
     * Prepares a shared playlist for playback
     */
    private ServerCommand loadSharedPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String[] parts = args.split(" ", 2);
                if (parts.length != 2) {
                    out.println("ERROR: Invalid arguments");
                    return false;
                }

                String ownerUsername = parts[0];
                String playlistName = parts[1];

                // Check if user exists
                User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
                if (owner == null) {
                    out.println("ERROR: User not found");
                    return false;
                }

                // Check if user is followed
                if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                    out.println("ERROR: You cannot access this playlist");
                    return false;
                }

                // Find playlist
                Playlist playlist = owner.getPlaylistByName(playlistName);
                if (playlist == null) {
                    out.println("ERROR: Playlist not found");
                    return false;
                }

                out.println("SUCCESS: Playlist loaded");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }

    /**
     * Set playlist sharing command
     * Configures whether user's playlists are shared publicly
     */
    private ServerCommand setPlaylistSharingCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                boolean sharePublicly = Boolean.parseBoolean(args);

                loggedInUser.setSharePlaylistsPublicly(sharePublicly);
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: Playlist sharing preferences updated");
                return true;
            } catch (Exception e) {
                out.println("ERROR: Server error");
                return false;
            }
        };
    }
}