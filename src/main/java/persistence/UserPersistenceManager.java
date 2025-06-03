package persistence;

import com.google.gson.*;
import server.music.Playlist;
import server.music.PlaylistAdapter;
import server.music.CollaborativePlaylist;
import users.User;
import users.FreeUser;
import users.PremiumUser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Manager for user data persistence using JSON format.
 * Implements data access layer with file-based storage.
 *
 * Follows SOLID principles:
 * - SRP: Single responsibility for user data persistence
 * - DIP: Provides abstraction for data storage (could be extended to database)
 */
public class UserPersistenceManager {

    private static final String USERS_FILE = "users.json";

    // Temporary storage for followed users during deserialization
    private static Map<User, List<String>> followedUsersMap = new HashMap<>();

    /**
     * Load all users from JSON file
     * Handles file creation, validation, and error recovery
     * @return List of loaded users (empty list if no users or errors)
     */
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);

        try {
            // Create empty file if it doesn't exist
            if (!file.exists()) {
                createEmptyUsersFile();
                return users;
            }

            // Check if file is empty
            if (file.length() == 0) {
                createEmptyUsersFile();
                return users;
            }

            try (Reader reader = new FileReader(USERS_FILE)) {
                // Parse and validate JSON structure
                JsonElement rootElement = JsonParser.parseReader(reader);

                if (rootElement == null || !rootElement.isJsonArray()) {
                    createEmptyUsersFile();
                    return users;
                }

                JsonArray jsonArray = rootElement.getAsJsonArray();

                // Configure Gson with custom adapters
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Playlist.class, new PlaylistAdapter());
                Gson gson = gsonBuilder.create();

                // Process each user in the JSON array
                for (JsonElement element : jsonArray) {
                    try {
                        JsonObject obj = element.getAsJsonObject();

                        // Validate required fields
                        if (obj.get("username") == null || obj.get("passwordHash") == null ||
                                obj.get("accountType") == null) {
                            continue; // Skip invalid entries
                        }

                        String username = obj.get("username").getAsString();
                        String passwordHash = obj.get("passwordHash").getAsString();
                        String accountType = obj.get("accountType").getAsString();

                        // Create appropriate user type using Factory pattern
                        User user;
                        if ("free".equalsIgnoreCase(accountType)) {
                            user = new FreeUser(username, passwordHash);
                        } else if ("premium".equalsIgnoreCase(accountType)) {
                            user = new PremiumUser(username, passwordHash);
                        } else {
                            continue; // Skip unknown account types
                        }

                        // Load playlists if present
                        if (obj.has("playlists") && obj.get("playlists").isJsonArray()) {
                            JsonArray playlistsArray = obj.getAsJsonArray("playlists");
                            for (JsonElement playlistElement : playlistsArray) {
                                try {
                                    Playlist playlist = gson.fromJson(playlistElement, Playlist.class);
                                    user.addPlaylist(playlist);
                                } catch (Exception e) {
                                    // Skip invalid playlists
                                }
                            }
                        }

                        // Load followed users (store for later resolution)
                        if (obj.has("followedUsers") && obj.get("followedUsers").isJsonArray()) {
                            JsonArray followedArray = obj.getAsJsonArray("followedUsers");
                            List<String> followedUsernames = new ArrayList<>();
                            for (JsonElement followedElement : followedArray) {
                                followedUsernames.add(followedElement.getAsString());
                            }
                            followedUsersMap.put(user, followedUsernames);
                        }

                        // Load sharing preferences
                        if (obj.has("sharePlaylistsPublicly")) {
                            user.setSharePlaylistsPublicly(obj.get("sharePlaylistsPublicly").getAsBoolean());
                        }

                        users.add(user);

                    } catch (Exception e) {
                        // Skip problematic user entries
                    }
                }

                // Resolve followed user references
                resolveFollowedUsers(users);

            }
        } catch (IOException | JsonSyntaxException e) {
            createEmptyUsersFile();
        }

        return users;
    }

    /**
     * Resolve followed user references after all users are loaded
     * @param users List of all loaded users
     */
    private static void resolveFollowedUsers(List<User> users) {
        for (Map.Entry<User, List<String>> entry : followedUsersMap.entrySet()) {
            User follower = entry.getKey();
            for (String username : entry.getValue()) {
                for (User potentialFollowed : users) {
                    if (potentialFollowed.getUsername().equals(username)) {
                        follower.follow(potentialFollowed);
                        break;
                    }
                }
            }
        }
        followedUsersMap.clear(); // Clean up temporary storage
    }

    /**
     * Create an empty users.json file with valid JSON array
     */
    private static void createEmptyUsersFile() {
        try (Writer writer = new FileWriter(USERS_FILE)) {
            writer.write("[]");
        } catch (IOException e) {
            // Silent fail
        }
    }

    /**
     * Save all users to JSON file with atomic write operation
     * @param users List of users to save
     */
    public static void saveUsers(List<User> users) {
        // Use temporary file for atomic write operation
        File tempFile = new File(USERS_FILE + ".tmp");
        File originalFile = new File(USERS_FILE);

        try {
            // Write to temporary file first
            try (Writer writer = new FileWriter(tempFile)) {
                // Configure Gson with custom adapters
                GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
                gsonBuilder.registerTypeAdapter(Playlist.class, new PlaylistAdapter());
                gsonBuilder.registerTypeAdapter(CollaborativePlaylist.class, new PlaylistAdapter());
                Gson gson = gsonBuilder.create();

                JsonArray userArray = new JsonArray();

                for (User user : users) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("username", user.getUsername());
                    obj.addProperty("passwordHash", user.getPasswordHash());
                    obj.addProperty("accountType", user.getAccountType());
                    obj.addProperty("sharePlaylistsPublicly", user.arePlaylistsSharedPublicly());

                    // Save playlists
                    if (!user.getPlaylists().isEmpty()) {
                        JsonArray playlistsArray = new JsonArray();
                        for (Playlist playlist : user.getPlaylists()) {
                            // Skip invalid playlists
                            if ("LOGIN_SUCCESS".equals(playlist.getName())) {
                                continue;
                            }

                            JsonElement playlistElement = gson.toJsonTree(playlist);
                            playlistsArray.add(playlistElement);
                        }
                        obj.add("playlists", playlistsArray);
                    }

                    // Save followed users
                    if (!user.getFollowedUsers().isEmpty()) {
                        JsonArray followedArray = new JsonArray();
                        for (User followed : user.getFollowedUsers()) {
                            followedArray.add(followed.getUsername());
                        }
                        obj.add("followedUsers", followedArray);
                    }

                    userArray.add(obj);
                }

                gson.toJson(userArray, writer);
                writer.flush();
            }

            // Atomic replacement - only if temp file was written successfully
            if (tempFile.exists() && tempFile.length() > 0) {
                // Create backup of original file
                File backupFile = new File(USERS_FILE + ".bak");
                if (originalFile.exists()) {
                    Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // Replace original with temp file
                Files.move(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new IOException("Failed to write temporary file or file is empty");
            }

        } catch (IOException e) {
            // Restore backup if available
            File backupFile = new File(USERS_FILE + ".bak");
            if (backupFile.exists() && originalFile.exists() && originalFile.length() == 0) {
                try {
                    Files.copy(backupFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreError) {
                    // Silent restore failure
                }
            }

            // Clean up temp file
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Add a single user and save immediately
     * @param user User to add
     */
    public static void addUser(User user) {
        List<User> users = loadUsers();
        users.add(user);
        saveUsers(users);
    }

    /**
     * Check if a username already exists
     * @param username Username to check
     * @return true if user exists, false otherwise
     */
    public static boolean doesUserExist(String username) {
        List<User> users = loadUsers();
        return users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    /**
     * Authenticate user by username and password
     * @param username Username to authenticate
     * @param inputPasswordHash Hashed password to verify
     * @return true if authentication successful, false otherwise
     */
    public static boolean authenticate(String username, String inputPasswordHash) {
        return loadUsers().stream()
                .anyMatch(u -> u.getUsername().equals(username) &&
                        u.getPasswordHash().equals(inputPasswordHash));
    }

    /**
     * Update an existing user's data
     * @param updatedUser User with updated information
     */
    public static void updateUser(User updatedUser) {
        List<User> users = loadUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(updatedUser.getUsername())) {
                users.set(i, updatedUser);
                break;
            }
        }
        saveUsers(users);
    }

    /**
     * Get a user by username
     * @param username Username to search for
     * @return User if found, null otherwise
     */
    public static User getUserByUsername(String username) {
        return loadUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Clean up invalid playlists for a user
     * @param user User to clean up
     */
    public static void cleanupInvalidPlaylists(User user) {
        if (user != null) {
            List<Playlist> validPlaylists = new ArrayList<>();
            for (Playlist p : user.getPlaylists()) {
                if (!"LOGIN_SUCCESS".equals(p.getName())) {
                    validPlaylists.add(p);
                }
            }

            // Only update if something was removed
            if (validPlaylists.size() < user.getPlaylists().size()) {
                user.setPlaylists(validPlaylists);
                updateUser(user);
            }
        }
    }
}