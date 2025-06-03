package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import persistence.UserPersistenceManager;
import server.command.ServerCommand;
import server.config.ServerConfig;
import server.music.MusicLibrary;
import server.music.Playlist;
import server.music.CollaborativePlaylist;
import server.music.Song;
import server.protocol.ServerProtocol;
import server.security.AttemptTracker;
import server.services.AuthenticationService;
import server.services.PlaylistService;
import users.User;
import utils.AppLogger;

/**
 * Refactored ClientHandler with improved architecture
 * Maintains original class name for compatibility
 *
 * Features:
 * - Service-oriented architecture
 * - Enhanced security with brute force protection
 * - Professional logging
 * - PBKDF2 password hashing with automatic migration
 * - Clean separation of concerns
 */
public class ClientHandler implements Runnable {

    // ===============================
    // CONNECTION AND NETWORKING
    // ===============================

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean isRunning = true;

    // ===============================
    // USER SESSION MANAGEMENT
    // ===============================

    private User loggedInUser;
    private long lastActivityTime;
    private java.util.Timer sessionTimer;
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    // ===============================
    // SERVICES (DEPENDENCY INJECTION)
    // ===============================

    private final AuthenticationService authService;
    private final PlaylistService playlistService;

    // ===============================
    // COMMAND PATTERN
    // ===============================

    private Map<String, CommandFactory> commandFactories;

    // ===============================
    // SECURITY & MONITORING
    // ===============================

    private static final Map<String, AttemptTracker> loginAttempts = new ConcurrentHashMap<>();
    private static final Map<String, ClientHandler> activeHandlers = new ConcurrentHashMap<>();

    /**
     * Constructor with dependency injection
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;

        // Initialize services with logger injection
        this.authService = new AuthenticationService(this::logActivity);
        this.playlistService = new PlaylistService(this::logActivity);

        // Setup connection timeout
        try {
            this.socket.setSoTimeout(ServerConfig.CONNECTION_TIMEOUT);
        } catch (Exception e) {
            AppLogger.debug("Could not set socket timeout: {}", e.getMessage());
        }

        // Initialize command registry
        initializeCommandFactories();
    }

    /**
     * Main execution thread - streamlined and robust
     */
    @Override
    public void run() {
        String clientAddress = socket.getInetAddress().getHostAddress();
        AppLogger.OperationTimer connectionTimer = AppLogger.startOperation("Client Connection");

        try {
            setupConnection(clientAddress);
            connectionTimer.completeWithSuccess("Connection established");

            processClientCommands();

        } catch (SocketException e) {
            AppLogger.debug("Client disconnected normally: {}", clientAddress);
        } catch (SocketTimeoutException e) {
            AppLogger.warn("Connection timed out: {}", clientAddress);
        } catch (IOException e) {
            AppLogger.warn("IO error with client {}: {}", clientAddress, e.getMessage());
        } catch (Exception e) {
            AppLogger.error("Unexpected error handling client {}", clientAddress, e);
        } finally {
            cleanup(clientAddress);
        }
    }

    /**
     * Setup client connection with proper error handling
     */
    private void setupConnection(String clientAddress) throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Register this handler
        activeHandlers.put(clientAddress, this);

        // Send welcome message
        out.println("🎵 Welcome to MiniSpotify server!");

        // Start session monitoring
        startSessionTimer();

        AppLogger.clientConnected(clientAddress);
    }

    /**
     * Main command processing loop - simplified and robust
     */
    private void processClientCommands() throws IOException {
        String commandLine;

        while (isRunning && (commandLine = in.readLine()) != null) {
            // Update activity timestamp
            lastActivityTime = System.currentTimeMillis();

            try {
                processCommand(commandLine.trim());
            } catch (Exception e) {
                AppLogger.error("Error processing command: {}", commandLine, e);
                sendErrorResponse("Server error processing your request");
            }
        }
    }

    /**
     * Process individual command with enhanced error handling
     */
    private void processCommand(String commandLine) {
        if (commandLine == null || commandLine.isEmpty()) {
            return;
        }

        AppLogger.OperationTimer cmdTimer = AppLogger.startOperation("Command: " + commandLine.split(" ")[0]);

        try {
            String[] parts = commandLine.split(" ", 2);
            String command = parts[0].toUpperCase();
            String args = parts.length > 1 ? parts[1] : "";

            CommandFactory factory = commandFactories.get(command);
            if (factory != null) {
                ServerCommand cmd = factory.createCommand(args);
                boolean success = cmd.execute(out);

                if (success) {
                    cmdTimer.completeWithSuccess("Command executed");
                } else {
                    cmdTimer.completeWithError("Command failed");
                }
            } else {
                out.println(ServerProtocol.RESP_ERROR + ": Unknown command '" + command + "'");
                cmdTimer.completeWithError("Unknown command");
                AppLogger.warn("Unknown command received: {}", command);
            }

        } catch (Exception e) {
            cmdTimer.completeWithError("Exception: " + e.getMessage());
            AppLogger.error("Command processing error", e);
            sendErrorResponse("Error: " + e.getMessage());
        }
    }

    /**
     * Initialize command factories with all available commands
     */
    private void initializeCommandFactories() {
        commandFactories = new HashMap<>();

        // ===== AUTHENTICATION COMMANDS =====
        commandFactories.put("LOGIN", this::createLoginCommand);
        commandFactories.put("CREATE", this::createUserCommand);
        commandFactories.put("LOGOUT", this::createLogoutCommand);

        // ===== PLAYLIST COMMANDS =====
        commandFactories.put("CREATE_PLAYLIST", this::createPlaylistCommand);
        commandFactories.put("CREATE_COLLAB_PLAYLIST", this::createCollabPlaylistCommand);
        commandFactories.put("GET_PLAYLISTS", this::getPlaylistsCommand);
        commandFactories.put("CHECK_PLAYLIST", this::checkPlaylistCommand);
        commandFactories.put("ADD_SONG_TO_PLAYLIST", this::addSongToPlaylistCommand);
        commandFactories.put("REMOVE_SONG_FROM_PLAYLIST", this::removeSongFromPlaylistCommand);
        commandFactories.put("REORDER_PLAYLIST_SONG", this::reorderPlaylistSongCommand);
        commandFactories.put("DELETE_PLAYLIST", this::deletePlaylistCommand);
        commandFactories.put("GET_PLAYLIST_SONGS", this::getPlaylistSongsCommand);

        // ===== MUSIC LIBRARY COMMANDS =====
        commandFactories.put("GET_ALL_SONGS", args -> getAllSongsCommand());
        commandFactories.put("SEARCH_TITLE", this::searchTitleCommand);
        commandFactories.put("SEARCH_ARTIST", this::searchArtistCommand);

        // ===== PLAYER COMMANDS =====
        commandFactories.put("LOAD_PLAYLIST", this::loadPlaylistCommand);
        commandFactories.put("SET_PLAYBACK_MODE", this::setPlaybackModeCommand);
        commandFactories.put("PLAYER_PLAY", this::createPlayerPlayCommand);
        commandFactories.put("PLAYER_PAUSE", this::createPlayerPauseCommand);
        commandFactories.put("PLAYER_STOP", this::createPlayerStopCommand);
        commandFactories.put("PLAYER_NEXT", this::createPlayerNextCommand);
        commandFactories.put("PLAYER_PREV", this::createPlayerPrevCommand);
        commandFactories.put("PLAYER_EXIT", this::createPlayerExitCommand);

        // ===== SOCIAL COMMANDS =====
        commandFactories.put("FOLLOW_USER", this::followUserCommand);
        commandFactories.put("UNFOLLOW_USER", this::unfollowUserCommand);
        commandFactories.put("GET_FOLLOWED_USERS", this::getFollowedUsersCommand);
        commandFactories.put("GET_SHARED_PLAYLISTS", this::getSharedPlaylistsCommand);
        commandFactories.put("GET_SHARED_PLAYLIST_SONGS", this::getSharedPlaylistSongsCommand);
        commandFactories.put("COPY_SHARED_PLAYLIST", this::copySharedPlaylistCommand);
        commandFactories.put("LOAD_SHARED_PLAYLIST", this::loadSharedPlaylistCommand);
        commandFactories.put("SET_PLAYLIST_SHARING", this::setPlaylistSharingCommand);
    }

    // ===============================
    // AUTHENTICATION COMMANDS
    // ===============================

    /**
     * Enhanced login command with brute force protection and automatic migration
     */
    private ServerCommand createLoginCommand(String args) {
        return out -> {
            String[] parts = args.split(" ");
            if (parts.length != 2) {
                out.println(ServerProtocol.RESP_LOGIN_FAIL + " Invalid format");
                return false;
            }

            String username = parts[0];
            String password = parts[1];

            AppLogger.userActivity(username, "LOGIN_ATTEMPT");

            // Use authentication service with enhanced security
            AuthenticationService.LoginResult result = authService.login(username, password);

            if (result.success) {
                loggedInUser = result.user;
                out.println(ServerProtocol.RESP_LOGIN_SUCCESS);
                AppLogger.loginSuccess(username);
                return true;
            } else {
                out.println(ServerProtocol.RESP_LOGIN_FAIL + " " + result.message);
                AppLogger.loginFailed(username, result.message);
                return false;
            }
        };
    }

    /**
     * FIXED: Enhanced user creation command with automatic login
     */
    private ServerCommand createUserCommand(String args) {
        return out -> {
            String[] parts = args.split(" ");
            if (parts.length != 3) {
                out.println(ServerProtocol.RESP_CREATE_FAIL + " Invalid arguments");
                return false;
            }

            String username = parts[0];
            String password = parts[1];
            String accountType = parts[2];

            AppLogger.userActivity(username, "REGISTRATION_ATTEMPT", accountType);

            AuthenticationService.RegistrationResult result =
                    authService.register(username, password, accountType);

            if (result.success) {
                // FIXED: Set the logged in user immediately after successful registration
                loggedInUser = result.user;
                out.println(ServerProtocol.RESP_CREATE_SUCCESS);
                AppLogger.userActivity(username, "REGISTRATION_SUCCESS", accountType);
                AppLogger.userActivity(username, "AUTO_LOGIN_AFTER_REGISTRATION");
                return true;
            } else {
                out.println(ServerProtocol.RESP_CREATE_FAIL + " " + result.message);
                AppLogger.userActivity(username, "REGISTRATION_FAILED", result.message);
                return false;
            }
        };
    }

    /**
     * Logout command with proper cleanup
     */
    private ServerCommand createLogoutCommand(String args) {
        return out -> {
            if (loggedInUser != null) {
                String username = loggedInUser.getUsername();
                authService.logout(username);
                AppLogger.userActivity(username, "LOGOUT");
                loggedInUser = null;
            }
            out.println("LOGOUT_SUCCESS");
            return true;
        };
    }

    // ===============================
    // PLAYLIST COMMANDS
    // ===============================

    /**
     * Create standard playlist using service
     */
    private ServerCommand createPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("CREATE_PLAYLIST_FAIL No user logged in");
                return false;
            }

            PlaylistService.PlaylistResult result =
                    playlistService.createPlaylist(loggedInUser, args.trim());

            if (result.success) {
                out.println(ServerProtocol.RESP_PLAYLIST_CREATED);
                AppLogger.playlistCreated(loggedInUser.getUsername(), args.trim());
            } else {
                out.println("CREATE_PLAYLIST_FAIL " + result.message);
            }

            return result.success;
        };
    }

    /**
     * Create collaborative playlist using service
     */
    private ServerCommand createCollabPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("CREATE_COLLAB_PLAYLIST_FAIL No user logged in");
                return false;
            }

            String[] parts = args.split(" ", 2);
            String playlistName = parts[0];
            String collaborators = parts.length > 1 ? parts[1] : "";

            PlaylistService.PlaylistResult result =
                    playlistService.createCollaborativePlaylist(loggedInUser, playlistName, collaborators);

            if (result.success) {
                out.println("COLLAB_PLAYLIST_CREATED");
                AppLogger.userActivity(loggedInUser.getUsername(), "COLLAB_PLAYLIST_CREATED", playlistName);
            } else {
                out.println("CREATE_COLLAB_PLAYLIST_FAIL " + result.message);
            }

            return result.success;
        };
    }

    /**
     * Get user playlists using service
     */
    private ServerCommand getPlaylistsCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                out.println(ServerProtocol.RESP_END);
                return false;
            }

            List<String> playlists = playlistService.getUserPlaylists(loggedInUser);

            for (String playlist : playlists) {
                out.println(playlist);
            }
            out.println(ServerProtocol.RESP_END);

            return true;
        };
    }

    /**
     * Check if playlist exists using service
     */
    private ServerCommand checkPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                return false;
            }

            boolean exists = playlistService.playlistExists(loggedInUser, args.trim());

            if (exists) {
                out.println(ServerProtocol.RESP_PLAYLIST_FOUND);
            } else {
                out.println(ServerProtocol.RESP_PLAYLIST_NOT_FOUND);
            }

            return exists;
        };
    }

    /**
     * Add song to playlist using service
     */
    private ServerCommand addSongToPlaylistCommand(String args) {
        return out -> {
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

            PlaylistService.PlaylistResult result =
                    playlistService.addSongToPlaylist(loggedInUser, playlistName, songTitle);

            if (result.success) {
                out.println("SUCCESS: " + result.message);
                AppLogger.songAddedToPlaylist(loggedInUser.getUsername(), songTitle, playlistName);
            } else {
                out.println("ERROR: " + result.message);
            }

            return result.success;
        };
    }

    /**
     * Remove song from playlist using service
     */
    private ServerCommand removeSongFromPlaylistCommand(String args) {
        return out -> {
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

            PlaylistService.PlaylistResult result =
                    playlistService.removeSongFromPlaylist(loggedInUser, playlistName, songTitle);

            if (result.success) {
                out.println(ServerProtocol.RESP_SUCCESS + ": " + result.message);
                AppLogger.userActivity(loggedInUser.getUsername(), "SONG_REMOVED",
                        songTitle + " from " + playlistName);
            } else {
                out.println(ServerProtocol.RESP_ERROR + ": " + result.message);
            }

            return result.success;
        };
    }

    /**
     * Reorder songs in playlist (maintained from original)
     */
    private ServerCommand reorderPlaylistSongCommand(String args) {
        return out -> {
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
                if (fromIndex < 0 || toIndex < 0 || fromIndex >= foundPlaylist.size() ||
                        toIndex >= foundPlaylist.size()) {
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid indices");
                    return false;
                }

                // Move song
                foundPlaylist.moveSong(fromIndex, toIndex);
                UserPersistenceManager.updateUser(loggedInUser);

                out.println(ServerProtocol.RESP_SUCCESS + ": Song reordered");
                AppLogger.userActivity(loggedInUser.getUsername(), "SONG_REORDERED",
                        playlistName + " (" + fromIndex + "->" + toIndex + ")");
                return true;

            } catch (NumberFormatException e) {
                out.println(ServerProtocol.RESP_ERROR + ": Invalid indices format");
                return false;
            } catch (Exception e) {
                AppLogger.error("Error reordering playlist song", e);
                out.println(ServerProtocol.RESP_ERROR + ": Server error");
                return false;
            }
        };
    }

    /**
     * Delete playlist using service
     */
    private ServerCommand deletePlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            PlaylistService.PlaylistResult result =
                    playlistService.deletePlaylist(loggedInUser, args.trim());

            if (result.success) {
                out.println("SUCCESS: " + result.message);
                AppLogger.playlistDeleted(loggedInUser.getUsername(), args.trim());
            } else {
                out.println("ERROR: " + result.message);
            }

            return result.success;
        };
    }

    /**
     * Get playlist songs using service
     */
    private ServerCommand getPlaylistSongsCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                out.println("END");
                return false;
            }

            PlaylistService.PlaylistSongsResult result =
                    playlistService.getPlaylistSongs(loggedInUser, args.trim());

            if (result.success) {
                out.println("SUCCESS: " + result.message);

                // Send songs in pipe-separated format
                for (Song song : result.songs) {
                    out.println(song.getTitle() + "|" +
                            song.getArtist() + "|" +
                            song.getAlbum() + "|" +
                            song.getGenre() + "|" +
                            song.getDuration() + "|" +
                            (song.getFilePath() != null ? song.getFilePath() : ""));
                }
            } else {
                out.println("ERROR: " + result.message);
            }

            out.println("END");
            return result.success;
        };
    }

    // ===============================
    // MUSIC LIBRARY COMMANDS
    // ===============================

    /**
     * Get all songs from library
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
                AppLogger.error("Error getting all songs", e);
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Search songs by title
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
                AppLogger.error("Error searching by title", e);
                out.println("ERROR: Server error occurred");
                out.println(ServerProtocol.RESP_END);
                return false;
            }
        };
    }

    /**
     * Search songs by artist
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
                AppLogger.error("Error searching by artist", e);
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
     * Load playlist for playback
     */
    private ServerCommand loadPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            boolean exists = playlistService.playlistExists(loggedInUser, args.trim());

            if (exists) {
                out.println("SUCCESS: Playlist loaded successfully");
                AppLogger.userActivity(loggedInUser.getUsername(), "PLAYLIST_LOADED", args.trim());
            } else {
                out.println("ERROR: Playlist not found");
            }

            return exists;
        };
    }

    /**
     * Set playback mode
     */
    private ServerCommand setPlaybackModeCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String modeChoice = args.trim();
            boolean validMode = modeChoice.equals("1") || modeChoice.equals("2") || modeChoice.equals("3");

            if (validMode) {
                out.println("SUCCESS: Playback mode set");
                String modeName;
                if (modeChoice.equals("1")) {
                    modeName = "Sequential";
                } else if (modeChoice.equals("2")) {
                    modeName = "Shuffle";
                } else if (modeChoice.equals("3")) {
                    modeName = "Repeat";
                } else {
                    modeName = "Unknown";
                }
                AppLogger.userActivity(loggedInUser.getUsername(), "PLAYBACK_MODE_SET", modeName);
            } else {
                out.println("ERROR: Invalid playback mode");
            }

            return validMode;
        };
    }

    /**
     * Player play command
     */
    private ServerCommand createPlayerPlayCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            out.println("▶️ Playing music...");
            AppLogger.userActivity(loggedInUser.getUsername(), "PLAYER_PLAY", args);
            return true;
        };
    }

    /**
     * Player pause command
     */
    private ServerCommand createPlayerPauseCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            out.println("⏸️ Music paused");
            AppLogger.userActivity(loggedInUser.getUsername(), "PLAYER_PAUSE");
            return true;
        };
    }

    /**
     * Player stop command
     */
    private ServerCommand createPlayerStopCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            out.println("⏹️ Music stopped");
            AppLogger.userActivity(loggedInUser.getUsername(), "PLAYER_STOP");
            return true;
        };
    }

    /**
     * Player next command
     */
    private ServerCommand createPlayerNextCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            out.println("⏭️ Next song");
            AppLogger.userActivity(loggedInUser.getUsername(), "PLAYER_NEXT");
            return true;
        };
    }

    /**
     * Player previous command
     */
    private ServerCommand createPlayerPrevCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            out.println("⏮️ Previous song");
            AppLogger.userActivity(loggedInUser.getUsername(), "PLAYER_PREV");
            return true;
        };
    }

    /**
     * Player exit command
     */
    private ServerCommand createPlayerExitCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            out.println("Exiting player mode");
            AppLogger.userActivity(loggedInUser.getUsername(), "PLAYER_EXIT");
            return true;
        };
    }

    // ===============================
    // SOCIAL FEATURES COMMANDS
    // ===============================

    /**
     * Follow user command with proper debugging and correct messages
     */
    private ServerCommand followUserCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String usernameToFollow = args.trim();
            AppLogger.debug("🔍 Follow request: '{}' wants to follow '{}'", loggedInUser.getUsername(), usernameToFollow);

            // Case-insensitive self-follow check
            if (loggedInUser.getUsername().equalsIgnoreCase(usernameToFollow)) {
                out.println("ERROR: You cannot follow yourself");
                return false;
            }

            // Case-insensitive user search
            User userToFollow = null;
            try {
                synchronized (UserPersistenceManager.class) {
                    List<User> allUsers = UserPersistenceManager.loadUsers();
                    userToFollow = allUsers.stream()
                            .filter(u -> u.getUsername().equalsIgnoreCase(usernameToFollow))
                            .findFirst()
                            .orElse(null);
                }
            } catch (Exception e) {
                AppLogger.error("Error loading users for follow operation", e);
                out.println("ERROR: Server error while searching for user");
                return false;
            }

            if (userToFollow == null) {
                AppLogger.debug("❌ User not found for follow: '{}'", usernameToFollow);
                out.println("ERROR: User not found");
                return false;
            }

            AppLogger.debug("✅ Found user to follow: '{}'", userToFollow.getUsername());

            // More robust follow status check
            User finalUserToFollow = userToFollow;
            boolean alreadyFollowing = loggedInUser.getFollowedUsers().stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(finalUserToFollow.getUsername()));

            if (alreadyFollowing) {
                out.println("INFO: You are already following this user");
                return true;
            }

            try {
                // Follow with synchronization
                synchronized (UserPersistenceManager.class) {
                    loggedInUser.follow(userToFollow);
                    UserPersistenceManager.updateUser(loggedInUser);

                    // Reload to verify save
                    User updatedUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
                    if (updatedUser != null) {
                        loggedInUser = updatedUser;
                    }
                }

                // Correct success message
                out.println("SUCCESS: You are now following " + userToFollow.getUsername());
                AppLogger.userActivity(loggedInUser.getUsername(), "USER_FOLLOWED", userToFollow.getUsername());
                AppLogger.debug("✅ Follow successful: '{}' now follows '{}'",
                        loggedInUser.getUsername(), userToFollow.getUsername());
                return true;

            } catch (Exception e) {
                AppLogger.error("Error during follow operation", e);
                out.println("ERROR: Server error during follow operation");
                return false;
            }
        };
    }

    /**
     * Unfollow user command with improved error handling
     */
    private ServerCommand unfollowUserCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String usernameToUnfollow = args.trim();
            AppLogger.debug("🔍 Unfollow request: '{}' wants to unfollow '{}'",
                    loggedInUser.getUsername(), usernameToUnfollow);

            // Case-insensitive search in followed users list
            User userToUnfollow = loggedInUser.getFollowedUsers().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(usernameToUnfollow))
                    .findFirst()
                    .orElse(null);

            if (userToUnfollow == null) {
                out.println("INFO: You are not following this user");
                return true;
            }

            try {
                synchronized (UserPersistenceManager.class) {
                    loggedInUser.unfollow(userToUnfollow);
                    UserPersistenceManager.updateUser(loggedInUser);

                    // Reload to verify
                    User updatedUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
                    if (updatedUser != null) {
                        loggedInUser = updatedUser;
                    }
                }

                out.println("SUCCESS: You are no longer following " + userToUnfollow.getUsername());
                AppLogger.userActivity(loggedInUser.getUsername(), "USER_UNFOLLOWED", userToUnfollow.getUsername());
                return true;

            } catch (Exception e) {
                AppLogger.error("Error during unfollow operation", e);
                out.println("ERROR: Server error during unfollow operation");
                return false;
            }
        };
    }

    /**
     * Get followed users command with robust validation
     */
    private ServerCommand getFollowedUsersCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                out.println("END");
                return false;
            }

            try {
                // Reload user for fresh data
                synchronized (UserPersistenceManager.class) {
                    User freshUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
                    if (freshUser != null) {
                        loggedInUser = freshUser;
                    }
                }

                List<User> followedUsers = loggedInUser.getFollowedUsers();
                AppLogger.debug("📋 User '{}' has {} followed users",
                        loggedInUser.getUsername(), followedUsers.size());

                // Validation and cleanup of followed users
                for (User user : followedUsers) {
                    if (user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                        // Verify user still exists
                        User existingUser = UserPersistenceManager.getUserByUsername(user.getUsername());
                        if (existingUser != null) {
                            out.println(existingUser.getUsername());
                            AppLogger.debug("📤 Sent followed user: '{}'", existingUser.getUsername());
                        } else {
                            AppLogger.warn("⚠️ Followed user no longer exists: '{}'", user.getUsername());
                            // Note: Could cleanup here, but keeping to avoid concurrent modifications
                        }
                    }
                }

                out.println("END");
                return true;

            } catch (Exception e) {
                AppLogger.error("Error getting followed users", e);
                out.println("ERROR: Server error retrieving followed users");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Get shared playlists command with validation
     */
    private ServerCommand getSharedPlaylistsCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                out.println("END");
                return false;
            }

            try {
                // Reload user for fresh data
                synchronized (UserPersistenceManager.class) {
                    User freshUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
                    if (freshUser != null) {
                        loggedInUser = freshUser;
                    }
                }

                List<User> followedUsers = loggedInUser.getFollowedUsers();
                AppLogger.debug("🔍 Checking shared playlists from {} followed users", followedUsers.size());

                int playlistCount = 0;
                for (User followedUser : followedUsers) {
                    if (followedUser == null) continue;

                    // Revalidate followed user
                    User currentFollowedUser = UserPersistenceManager.getUserByUsername(followedUser.getUsername());
                    if (currentFollowedUser == null) {
                        AppLogger.debug("⚠️ Followed user no longer exists: '{}'", followedUser.getUsername());
                        continue;
                    }

                    if (currentFollowedUser.arePlaylistsSharedPublicly()) {
                        for (Playlist playlist : currentFollowedUser.getPlaylists()) {
                            if (playlist != null && playlist.getName() != null) {
                                out.println(playlist.getName() + "|" + currentFollowedUser.getUsername());
                                playlistCount++;
                                AppLogger.debug("📤 Sent shared playlist: '{}' by '{}'",
                                        playlist.getName(), currentFollowedUser.getUsername());
                            }
                        }
                    }
                }

                AppLogger.debug("✅ Sent {} shared playlists total", playlistCount);
                out.println("END");
                return true;

            } catch (Exception e) {
                AppLogger.error("Error getting shared playlists", e);
                out.println("ERROR: Server error retrieving shared playlists");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Get shared playlist songs command
     */
    private ServerCommand getSharedPlaylistSongsCommand(String args) {
        return out -> {
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

            try {
                // Case-insensitive owner search
                User owner = null;
                synchronized (UserPersistenceManager.class) {
                    List<User> allUsers = UserPersistenceManager.loadUsers();
                    owner = allUsers.stream()
                            .filter(u -> u.getUsername().equalsIgnoreCase(ownerUsername))
                            .findFirst()
                            .orElse(null);
                }

                if (owner == null) {
                    out.println("ERROR: User not found");
                    out.println("END");
                    return false;
                }

                // Access permission check
                User finalOwner = owner;
                boolean canAccess = loggedInUser.getFollowedUsers().stream()
                        .anyMatch(u -> u.getUsername().equalsIgnoreCase(finalOwner.getUsername()))
                        || owner.arePlaylistsSharedPublicly();

                if (!canAccess) {
                    out.println("ERROR: You cannot access this playlist");
                    out.println("END");
                    return false;
                }

                // Case-insensitive playlist search
                Playlist playlist = owner.getPlaylists().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(playlistName))
                        .findFirst()
                        .orElse(null);

                if (playlist == null) {
                    out.println("ERROR: Playlist not found");
                    out.println("END");
                    return false;
                }

                out.println("SUCCESS: Found playlist");

                for (Song song : playlist.getSongs()) {
                    if (song != null) {
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
                AppLogger.error("Error getting shared playlist songs", e);
                out.println("ERROR: Server error retrieving playlist songs");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Copy shared playlist command
     */
    private ServerCommand copySharedPlaylistCommand(String args) {
        return out -> {
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

            User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
            if (owner == null) {
                out.println("ERROR: User not found");
                return false;
            }

            if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                out.println("ERROR: You cannot access this playlist");
                return false;
            }

            Playlist sourcePlaylist = owner.getPlaylistByName(sourcePlaylistName);
            if (sourcePlaylist == null) {
                out.println("ERROR: Playlist not found");
                return false;
            }

            if (loggedInUser.getPlaylistByName(newPlaylistName) != null) {
                out.println("ERROR: A playlist with this name already exists");
                return false;
            }

            if (!loggedInUser.canAddPlaylist()) {
                out.println("ERROR: You have reached your playlist limit");
                return false;
            }

            Playlist newPlaylist = new Playlist(newPlaylistName);
            for (Song song : sourcePlaylist.getSongs()) {
                newPlaylist.addSong(song);
            }

            loggedInUser.addPlaylist(newPlaylist);
            UserPersistenceManager.updateUser(loggedInUser);

            out.println("SUCCESS: Playlist copied successfully");
            AppLogger.userActivity(loggedInUser.getUsername(), "PLAYLIST_COPIED",
                    sourcePlaylistName + " -> " + newPlaylistName);
            return true;
        };
    }

    /**
     * Load shared playlist command
     */
    private ServerCommand loadSharedPlaylistCommand(String args) {
        return out -> {
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

            User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
            if (owner == null) {
                out.println("ERROR: User not found");
                return false;
            }

            if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                out.println("ERROR: You cannot access this playlist");
                return false;
            }

            Playlist playlist = owner.getPlaylistByName(playlistName);
            if (playlist == null) {
                out.println("ERROR: Playlist not found");
                return false;
            }

            out.println("SUCCESS: Playlist loaded");
            AppLogger.userActivity(loggedInUser.getUsername(), "SHARED_PLAYLIST_LOADED",
                    ownerUsername + "/" + playlistName);
            return true;
        };
    }

    /**
     * Set playlist sharing command
     */
    private ServerCommand setPlaylistSharingCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            boolean sharePublicly = Boolean.parseBoolean(args);
            loggedInUser.setSharePlaylistsPublicly(sharePublicly);
            UserPersistenceManager.updateUser(loggedInUser);

            out.println("SUCCESS: Playlist sharing preferences updated");
            AppLogger.userActivity(loggedInUser.getUsername(), "SHARING_PREFERENCES_SET",
                    String.valueOf(sharePublicly));
            return true;
        };
    }

    // ===============================
    // UTILITY AND HELPER METHODS
    // ===============================

    /**
     * Send error response to client
     */
    private void sendErrorResponse(String message) {
        if (out != null && !socket.isClosed()) {
            out.println("ERROR: " + message);
        }
    }

    /**
     * Log activity with consistent format
     */
    private void logActivity(String message) {
        AppLogger.info(message);
    }

    /**
     * Start session monitoring timer
     */
    private void startSessionTimer() {
        lastActivityTime = System.currentTimeMillis();
        sessionTimer = new java.util.Timer();
        sessionTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    if (!isRunning || socket == null || socket.isClosed() || !socket.isConnected()) {
                        cleanup(socket.getInetAddress().getHostAddress());
                        cancel();
                        return;
                    }

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastActivityTime > SESSION_TIMEOUT) {
                        if (loggedInUser != null) {
                            AppLogger.userActivity(loggedInUser.getUsername(), "SESSION_TIMEOUT");
                            loggedInUser = null; // Auto-logout due to inactivity
                        }
                    }

                    // Periodic user state sync
                    syncUserState();

                } catch (Exception e) {
                    AppLogger.debug("Session timer error: {}", e.getMessage());
                }
            }
        }, 10000, 60000); // Check every 60 seconds, first check after 10 seconds
    }

    /**
     * Synchronize user state with persistence layer
     */
    private void syncUserState() {
        if (loggedInUser != null) {
            try {
                User updatedUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
                if (updatedUser != null) {
                    loggedInUser = updatedUser;
                } else {
                    AppLogger.warn("User {} no longer exists in persistence", loggedInUser.getUsername());
                    loggedInUser = null; // User was deleted
                }
            } catch (Exception e) {
                AppLogger.debug("Error syncing user state: {}", e.getMessage());
            }
        }
    }

    /**
     * Clean up resources and connections
     */
    private void cleanup(String clientAddress) {
        isRunning = false;

        // Remove from active handlers
        activeHandlers.remove(clientAddress);

        // Stop session timer
        if (sessionTimer != null) {
            sessionTimer.cancel();
            sessionTimer = null;
        }

        // Log final user activity if logged in
        if (loggedInUser != null) {
            AppLogger.userActivity(loggedInUser.getUsername(), "SESSION_ENDED");
        }

        // Close all resources
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            AppLogger.debug("Error during cleanup: {}", e.getMessage());
        }

        AppLogger.clientDisconnected(clientAddress);
    }

    /**
     * Stop this client handler gracefully
     */
    public void stop() {
        isRunning = false;
        if (socket != null) {
            cleanup(socket.getInetAddress().getHostAddress());
        }
    }

    // ===============================
    // GETTERS FOR MONITORING
    // ===============================

    /**
     * Get logged in user (for monitoring)
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Check if handler is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get last activity time
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Get all active handlers (for server monitoring)
     */
    public static Map<String, ClientHandler> getActiveHandlers() {
        return new HashMap<>(activeHandlers);
    }

    // ===============================
    // COMMAND FACTORY INTERFACE
    // ===============================

    /**
     * Functional interface for Command Factory pattern
     */
    @FunctionalInterface
    private interface CommandFactory {
        ServerCommand createCommand(String args);
    }
}