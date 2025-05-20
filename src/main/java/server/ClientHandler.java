package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            // Set connection timeout
            this.socket.setSoTimeout(ServerConfig.CONNECTION_TIMEOUT);
        } catch (Exception e) {
            System.err.println("Could not set socket timeout: " + e.getMessage());
        }
        initializeCommandFactories();
    }

    /**
     * Initialize command factories
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

    @Override
    public void run() {
        String clientAddress = socket.getInetAddress().toString();

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
                final String command = line; // Make final for thread safety
                System.out.println("DEBUG: Received command from " + clientAddress + " -> " + command);

                // Refresh last activity time
                lastActivityTime = System.currentTimeMillis();

                try {
                    // Process command in try-catch to prevent server crash
                    processCommand(command);
                } catch (Exception e) {
                    System.err.println("Error processing command '" + command + "': " + e.getMessage());
                    e.printStackTrace();

                    // Send error message to client but don't interrupt connection
                    try {
                        if (out != null && !socket.isClosed()) {
                            out.println("ERROR: Server error processing your request: " + e.getMessage());
                        }
                    } catch (Exception ex) {
                        System.err.println("Could not send error message to client: " + ex.getMessage());
                    }
                }
            }

            // If we exit the loop normally, client disconnected properly
            System.out.println("DEBUG: Client " + clientAddress + " disconnected normally");
        } catch (SocketException e) {
            System.err.println("Socket error for client " + clientAddress + ": " + e.getMessage());
            // Don't print stack trace for ordinary socket exceptions
        } catch (SocketTimeoutException e) {
            System.err.println("Connection timed out for client " + clientAddress);
        } catch (IOException e) {
            System.err.println("I/O error for client " + clientAddress + ": " + e.getMessage());
            if (!(e instanceof SocketException)) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error for client " + clientAddress + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Unregister this handler and clean up
            activeHandlers.remove(clientAddress);
            closeResources();
            System.out.println("DEBUG: Client handler for " + clientAddress + " terminated");
        }
    }

    /**
     * Process a command received from client
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
            System.err.println("Error processing command: " + e.getMessage());
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * Close resources
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

            System.out.println("DEBUG: Resources closed and cleaned up");
        } catch (Exception e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Synchronize user state with persistence
     */
    private void syncUserState() {
        if (loggedInUser != null) {
            try {
                List<User> users = UserPersistenceManager.loadUsers();
                for (User u : users) {
                    if (u.getUsername().equals(loggedInUser.getUsername())) {
                        loggedInUser = u;
                        System.out.println("DEBUG: Synchronized user state for: " + loggedInUser.getUsername());
                        System.out.println("DEBUG: User now has " + loggedInUser.getPlaylists().size() + " playlists");
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error synchronizing user state: " + e.getMessage());
            }
        }
    }

    /**
     * Creates a command for creating a collaborative playlist with robust error handling
     */
    private ServerCommand createCollabPlaylistCommand(String args) {
        return out -> {
            try {
                System.out.println("DEBUG: Processing collaborative playlist creation");
                // Check session state before processing command
                checkUserSession();

                if (loggedInUser == null) {
                    System.out.println("DEBUG: No user logged in");
                    out.println("CREATE_COLLAB_PLAYLIST_FAIL No user logged in");
                    return false;
                }

                String[] parts = args.split(" ", 2);
                if (parts.length < 1) {
                    System.out.println("DEBUG: Invalid format for collab playlist creation");
                    out.println("CREATE_COLLAB_PLAYLIST_FAIL Invalid format");
                    return false;
                }

                String playlistName = parts[0];
                System.out.println("DEBUG: Creating collaborative playlist: '" + playlistName + "'");

                if (loggedInUser.canAddPlaylist()) {
                    boolean exists = loggedInUser.getPlaylists().stream()
                            .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

                    if (!exists) {
                        try {
                            // Explicitly create a CollaborativePlaylist - not a regular Playlist
                            CollaborativePlaylist playlist = new CollaborativePlaylist(playlistName, loggedInUser);
                            System.out.println("DEBUG: Created CollaborativePlaylist object: " + playlist.getClass().getName());

                            // Verify it's the correct class
                            if (!(playlist instanceof CollaborativePlaylist)) {
                                throw new RuntimeException("Failed to create a CollaborativePlaylist instance");
                            }

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
                                                System.out.println("DEBUG: Added collaborator: " + collaboratorName);
                                            } else {
                                                System.out.println("DEBUG: Collaborator not found: " + collaboratorName);
                                            }
                                        } catch (Exception e) {
                                            // Just log the error but continue processing
                                            System.err.println("Error processing collaborator " + collaboratorName + ": " + e.getMessage());
                                        }
                                    }
                                }
                            }

                            // Debug again to verify collaborators were added
                            System.out.println("DEBUG: Playlist is collaborative: " + (playlist instanceof CollaborativePlaylist));
                            System.out.println("DEBUG: Collaborators: " + String.join(", ", playlist.getCollaboratorUsernames()));

                            // Add playlist to user
                            loggedInUser.addPlaylist(playlist);

                            // Save user state
                            UserPersistenceManager.updateUser(loggedInUser);

                            out.println("COLLAB_PLAYLIST_CREATED");
                            return true;
                        } catch (Exception e) {
                            System.err.println("Error creating collaborative playlist: " + e.getMessage());
                            e.printStackTrace();
                            out.println("CREATE_COLLAB_PLAYLIST_FAIL Server error: " + e.getMessage());
                            return false;
                        }
                    } else {
                        out.println("CREATE_COLLAB_PLAYLIST_FAIL Playlist already exists");
                        return false;
                    }
                } else {
                    out.println("CREATE_COLLAB_PLAYLIST_FAIL You cannot have more than 2 playlists with the free account");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Unexpected error in createCollabPlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("CREATE_COLLAB_PLAYLIST_FAIL Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Start session timer
     */
    public void startSessionTimer() {
        // Initialize activity time
        lastActivityTime = System.currentTimeMillis();

        // Create and configure timer
        sessionTimer = new java.util.Timer();
        sessionTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    // Check if connection is still active
                    if (!isRunning || socket == null || socket.isClosed() || !socket.isConnected()) {
                        System.out.println("DEBUG: Socket closed or disconnected - cleaning up");
                        closeResources();
                        cancel(); // Stop timer
                        return;
                    }

                    // Check if session has timed out due to inactivity
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastActivityTime > SESSION_TIMEOUT) {
                        System.out.println("DEBUG: Session expired due to inactivity");
                        loggedInUser = null; // Log out user
                    }

                    // Check user session state
                    checkUserSession();
                } catch (Exception e) {
                    System.err.println("Error in session timer: " + e.getMessage());
                }
            }
        }, 10000, 60000); // Check every 60 seconds, first check after 10 seconds
    }

    /**
     * Check user session state
     */
    private void checkUserSession() {
        if (loggedInUser == null) {
            // No user logged in, nothing to do
            return;
        }

        try {
            // Reload user from persistence to ensure it's up to date
            User updatedUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
            if (updatedUser != null) {
                loggedInUser = updatedUser;
            } else {
                System.out.println("DEBUG: User no longer exists in persistence - clearing session");
                loggedInUser = null;
            }
        } catch (Exception e) {
            System.err.println("Error checking user session: " + e.getMessage());
        }
    }

    /**
     * Stop this client handler
     */
    public void stop() {
        isRunning = false;
        closeResources();
    }

    /**
     * Interface for Command Factory
     */
    @FunctionalInterface
    private interface CommandFactory {
        ServerCommand createCommand(String args);
    }

    /**
     * Login command
     */
    private ServerCommand createLoginCommand(String args) {
        return out -> {
            try {
                String[] parts = args.split(" ");
                if (parts.length != 2) {
                    out.println(ServerProtocol.RESP_LOGIN_FAIL + " Invalid format. Expected: LOGIN username password");
                    return false;
                }

                String username = parts[0];
                String password = parts[1];

                // Debug the login attempt
                System.out.println("DEBUG: Login attempt - Username: " + username + ", Password hash: " + PasswordHasher.hashPassword(password));

                List<User> users = UserPersistenceManager.loadUsers();

                // Print all users and their password hashes for debugging
                for (User u : users) {
                    System.out.println("DEBUG: User in DB: " + u.getUsername() + ", Password hash: " + u.getPasswordHash());
                }

                User matchingUser = users.stream()
                        .filter(u -> u.getUsername().equalsIgnoreCase(username))
                        .findFirst()
                        .orElse(null);

                if (matchingUser != null) {
                    // More detailed check for debugging
                    boolean usernameMatch = matchingUser.getUsername().equalsIgnoreCase(username);
                    boolean passwordMatch = PasswordHasher.checkPassword(password, matchingUser.getPasswordHash());

                    System.out.println("DEBUG: Username match: " + usernameMatch + ", Password match: " + passwordMatch);

                    if (passwordMatch) {
                        // Clean up invalid playlists before setting logged in user
                        UserPersistenceManager.cleanupInvalidPlaylists(matchingUser);

                        loggedInUser = matchingUser;
                        out.println(ServerProtocol.RESP_LOGIN_SUCCESS);
                        System.out.println("DEBUG: User logged in: " + username);
                        return true;
                    }
                }

                out.println(ServerProtocol.RESP_LOGIN_FAIL + " Incorrect username or password");
                return false;
            } catch (Exception e) {
                System.err.println("Error in login command: " + e.getMessage());
                e.printStackTrace();
                out.println(ServerProtocol.RESP_LOGIN_FAIL + " Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Create user command
     */
    private ServerCommand createUserCommand(String args) {
        return out -> {
            try {
                String[] parts = args.split(" ");
                if (parts.length != 3) {
                    out.println(ServerProtocol.RESP_CREATE_FAIL + " Invalid arguments. Expected: CREATE username password accountType");
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
                        out.println(ServerProtocol.RESP_CREATE_FAIL + " Invalid account type. Use 'free' or 'premium'");
                        return false;
                    }

                    UserPersistenceManager.addUser(newUser);
                    out.println(ServerProtocol.RESP_CREATE_SUCCESS);
                    return true;
                } catch (Exception e) {
                    System.err.println("❌ Error during CREATE: " + e.getMessage());
                    e.printStackTrace();
                    out.println(ServerProtocol.RESP_CREATE_FAIL + " Server error: " + e.getMessage());
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error in create user command: " + e.getMessage());
                e.printStackTrace();
                out.println(ServerProtocol.RESP_CREATE_FAIL + " Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Create playlist command
     */
    private ServerCommand createPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("CREATE_PLAYLIST_FAIL No user logged in");
                    return false;
                }

                String playlistName = args.trim();
                System.out.println("DEBUG: Creating playlist: '" + playlistName + "'");

                if (loggedInUser.canAddPlaylist()) {
                    boolean exists = loggedInUser.getPlaylists().stream()
                            .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

                    if (!exists) {
                        loggedInUser.addPlaylist(new Playlist(playlistName));

                        // Update user in persistence system
                        UserPersistenceManager.updateUser(loggedInUser);

                        System.out.println("DEBUG: Playlist created: '" + playlistName + "'");
                        System.out.println("DEBUG: User now has " + loggedInUser.getPlaylists().size() + " playlists");
                        out.println(ServerProtocol.RESP_PLAYLIST_CREATED);
                        return true;
                    } else {
                        out.println(ServerProtocol.RESP_PLAYLIST_EXISTS);
                        return false;
                    }
                } else {
                    out.println("CREATE_PLAYLIST_FAIL You cannot have more than 2 playlists with the free account");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error in create playlist command: " + e.getMessage());
                e.printStackTrace();
                out.println("CREATE_PLAYLIST_FAIL Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Get playlists command
     */
    private ServerCommand getPlaylistsCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                    out.println(ServerProtocol.RESP_END);
                    return false;
                }

                // Synchronize user state to get latest playlists
                syncUserState();

                System.out.println("DEBUG: Getting playlists for user: " + loggedInUser.getUsername());
                System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists");

                if (loggedInUser.getPlaylists().isEmpty()) {
                    out.println(ServerProtocol.RESP_END);
                    return true;
                }

                for (Playlist playlist : loggedInUser.getPlaylists()) {
                    out.println(playlist.getName());
                    System.out.println("DEBUG: Sending playlist: " + playlist.getName());
                }
                out.println(ServerProtocol.RESP_END);
                return true;
            } catch (Exception e) {
                System.err.println("Error in getPlaylistsCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
                out.println(ServerProtocol.RESP_END);
                return false;
            }
        };
    }

    /**
     * Check playlist command
     */
    private ServerCommand checkPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                    return false;
                }

                String playlistName = args.trim();
                System.out.println("DEBUG: Checking for playlist: '" + playlistName + "'");

                // Sync user state first
                syncUserState();

                // Debug output all playlists
                System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists:");
                for (Playlist p : loggedInUser.getPlaylists()) {
                    System.out.println("DEBUG: - '" + p.getName() + "'");
                }

                // Check if the playlist exists
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
                System.err.println("Error in checkPlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println(ServerProtocol.RESP_ERROR + ": Server error");
                return false;
            }
        };
    }

    /**
     * Add song to playlist command
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
                    out.println("ERROR: Invalid arguments. Expected: ADD_SONG_TO_PLAYLIST playlistName songTitle");
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
                    out.println("Playlist not found.");
                    return false;
                }

                // Find song with all its attributes in the library
                Song originalSong = null;
                for (Song s : MusicLibrary.getInstance().getAllSongs()) {
                    if (s.getTitle().equals(songTitle)) {
                        originalSong = s;
                        break;
                    }
                }

                if (originalSong == null) {
                    out.println("Song not found.");
                    return false;
                }

                System.out.println("DEBUG: Adding song '" + originalSong.getTitle() +
                        "' with path: " + originalSong.getFilePath() +
                        " to playlist '" + playlistName + "'");

                // Create a complete copy of the song to add to the playlist
                Song songCopy = new Song(
                        originalSong.getTitle(),
                        originalSong.getArtist(),
                        originalSong.getAlbum(),
                        originalSong.getGenre(),
                        originalSong.getDuration()
                );
                // Preserve file path!
                songCopy.setFilePath(originalSong.getFilePath());

                // Add to playlist
                found.addSong(songCopy);

                // Update persistence
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: Song added to playlist.");
                return true;
            } catch (Exception e) {
                System.err.println("Error in addSongToPlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Remove song from playlist command
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
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid arguments. Expected: REMOVE_SONG_FROM_PLAYLIST playlistName songTitle");
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
                    // Update persistence
                    UserPersistenceManager.updateUser(loggedInUser);

                    out.println(ServerProtocol.RESP_SUCCESS + ": Song removed from playlist");
                    return true;
                } else {
                    out.println(ServerProtocol.RESP_ERROR + ": Song not found in playlist");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error in removeSongFromPlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println(ServerProtocol.RESP_ERROR + ": Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Reorder playlist song command
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
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid arguments. Expected: REORDER_PLAYLIST_SONG playlistName fromIndex toIndex");
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

                    // Check indices
                    if (fromIndex < 0 || toIndex < 0 || fromIndex >= foundPlaylist.size() || toIndex >= foundPlaylist.size()) {
                        out.println(ServerProtocol.RESP_ERROR + ": Invalid indices");
                        return false;
                    }

                    // Move song
                    foundPlaylist.moveSong(fromIndex, toIndex);

                    // Update persistence
                    UserPersistenceManager.updateUser(loggedInUser);

                    out.println(ServerProtocol.RESP_SUCCESS + ": Song reordered");
                    return true;
                } catch (NumberFormatException e) {
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid indices format");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error in reorderPlaylistSongCommand: " + e.getMessage());
                e.printStackTrace();
                out.println(ServerProtocol.RESP_ERROR + ": Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Get playlist songs command
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
                System.out.println("DEBUG: Getting songs for playlist: " + playlistName);

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

                // For each song in playlist
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

                    // If song exists in library, use its complete details
                    if (librarySong != null) {
                        // SIMPLE and CLEAR formatting to avoid confusion
                        out.println(librarySong.getTitle() + "|" +
                                librarySong.getArtist() + "|" +
                                librarySong.getAlbum() + "|" +
                                librarySong.getGenre() + "|" +
                                librarySong.getDuration() + "|" +
                                (librarySong.getFilePath() != null ? librarySong.getFilePath() : ""));

                        System.out.println("DEBUG: Sending song '" + librarySong.getTitle() +
                                "' with path: " + librarySong.getFilePath());
                    } else {
                        // If song not in library, use playlist version
                        out.println(song.getTitle() + "|" +
                                song.getArtist() + "|" +
                                song.getAlbum() + "|" +
                                song.getGenre() + "|" +
                                song.getDuration() + "|" +
                                (song.getFilePath() != null ? song.getFilePath() : ""));

                        System.out.println("DEBUG: Sending song '" + song.getTitle() +
                                "' with path: " + song.getFilePath() + " (not found in library)");
                    }
                }

                out.println("END");
                return true;
            } catch (Exception e) {
                System.err.println("Error in getPlaylistSongsCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Set playback mode command
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
                        System.out.println("DEBUG: Setting playback mode to Sequential");
                        break;
                    case "2": // Shuffle
                        System.out.println("DEBUG: Setting playback mode to Shuffle");
                        break;
                    case "3": // Repeat
                        System.out.println("DEBUG: Setting playback mode to Repeat");
                        break;
                    default:
                        validMode = false;
                        System.out.println("DEBUG: Unknown playback mode: " + modeChoice);
                }

                if (validMode) {
                    out.println("SUCCESS: Playback mode set");
                    return true;
                } else {
                    out.println("ERROR: Invalid playback mode");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error in setPlaybackModeCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Player play command
     */
    private ServerCommand playerPlayCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                System.out.println("DEBUG: Player play command received");
                out.println("▶️ Playing music...");
                return true;
            } catch (Exception e) {
                System.err.println("Error in playerPlayCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Player pause command
     */
    private ServerCommand playerPauseCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                System.out.println("DEBUG: Player pause command received");
                out.println("⏸️ Music paused");
                return true;
            } catch (Exception e) {
                System.err.println("Error in playerPauseCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Player stop command
     */
    private ServerCommand playerStopCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                System.out.println("DEBUG: Player stop command received");
                out.println("⏹️ Music stopped");
                return true;
            } catch (Exception e) {
                System.err.println("Error in playerStopCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Player next command
     */
    private ServerCommand playerNextCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                System.out.println("DEBUG: Player next command received");
                out.println("⏭️ Next song");
                return true;
            } catch (Exception e) {
                System.err.println("Error in playerNextCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Player previous command
     */
    private ServerCommand playerPrevCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                System.out.println("DEBUG: Player previous command received");
                out.println("⏮️ Previous song");
                return true;
            } catch (Exception e) {
                System.err.println("Error in playerPrevCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Player exit command
     */
    private ServerCommand playerExitCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                System.out.println("DEBUG: Player exit command received");
                out.println("Exiting player mode");
                return true;
            } catch (Exception e) {
                System.err.println("Error in playerExitCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Load playlist command
     */
    private ServerCommand loadPlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String playlistName = args.trim();
                System.out.println("DEBUG: Loading playlist: " + playlistName);

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

                System.out.println("DEBUG: Playlist found: " + foundPlaylist.getName()
                        + " with " + foundPlaylist.size() + " songs");
                out.println("SUCCESS: Playlist loaded successfully");
                return true;
            } catch (Exception e) {
                System.err.println("Error in loadPlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Get all songs command
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
                System.err.println("Error in getAllSongsCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Search title command
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
                System.err.println("Error in searchTitleCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
                out.println(ServerProtocol.RESP_END);
                return false;
            }
        };
    }

    /**
     * Search artist command
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
                System.err.println("Error in searchArtistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
                out.println(ServerProtocol.RESP_END);
                return false;
            }
        };
    }

    /**
     * Follow user command
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

                // Update persistence
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: You are now following " + usernameToFollow);
                return true;
            } catch (Exception e) {
                System.err.println("Error in followUserCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Unfollow user command
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

                // Update persistence
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: You are no longer following " + usernameToUnfollow);
                return true;
            } catch (Exception e) {
                System.err.println("Error in unfollowUserCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Get followed users command
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
                System.err.println("Error in getFollowedUsersCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
                out.println("END");
                return false;
            }
        };
    }

    /**
     * Get shared playlists command
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
                boolean foundPlaylists = false;

                // For each followed user
                for (User followedUser : followedUsers) {
                    // Check if they share playlists
                    if (followedUser.arePlaylistsSharedPublicly()) {
                        // Send this user's playlists
                        for (Playlist playlist : followedUser.getPlaylists()) {
                            out.println(playlist.getName() + "|" + followedUser.getUsername());
                            foundPlaylists = true;
                        }
                    }
                }

                if (!foundPlaylists) {
                    // No shared playlists found
                }

                out.println("END");
                return true;
            } catch (Exception e) {
                System.err.println("Error in getSharedPlaylistsCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
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

                // Check if user is followed
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

                // Send songs
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
                System.err.println("Error in getSharedPlaylistSongsCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error occurred");
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

                // Update persistence
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: Playlist copied successfully");
                return true;
            } catch (Exception e) {
                System.err.println("Error in copySharedPlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Load shared playlist command
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
                System.err.println("Error in loadSharedPlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Set playlist sharing command
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

                // Update persistence
                UserPersistenceManager.updateUser(loggedInUser);

                out.println("SUCCESS: Playlist sharing preferences updated");
                return true;
            } catch (Exception e) {
                System.err.println("Error in setPlaylistSharingCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }
    private ServerCommand deletePlaylistCommand(String args) {
        return out -> {
            try {
                if (loggedInUser == null) {
                    out.println("ERROR: Not logged in");
                    return false;
                }

                String playlistName = args.trim();

                // Version simplifiée utilisant la méthode removePlaylist()
                if (loggedInUser.removePlaylist(playlistName)) {
                    // Mettre à jour la persistence
                    UserPersistenceManager.updateUser(loggedInUser);
                    out.println("SUCCESS: Playlist '" + playlistName + "' deleted successfully");
                    return true;
                } else {
                    out.println("ERROR: Playlist '" + playlistName + "' not found");
                    return false;
                }

            } catch (Exception e) {
                System.err.println("Error in deletePlaylistCommand: " + e.getMessage());
                e.printStackTrace();
                out.println("ERROR: Server error: " + e.getMessage());
                return false;
            }
        };
    }



}