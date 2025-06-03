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
 * Manager for user data persistence with case-insensitive consistency
 * Handles JSON serialization/deserialization of user data with robust error handling
 */
public class UserPersistenceManager {

    private static final String USERS_FILE = "users.json";

    // Temporary storage for followed users during deserialization
    private static Map<User, List<String>> followedUsersMap = new HashMap<>();

    /**
     * Load all users from JSON file
     * Enhanced with better error handling and validation
     */
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);

        try {
            if (!file.exists()) {
                createEmptyUsersFile();
                return users;
            }

            if (file.length() == 0) {
                createEmptyUsersFile();
                return users;
            }

            try (Reader reader = new FileReader(USERS_FILE)) {
                JsonElement rootElement = JsonParser.parseReader(reader);

                if (rootElement == null || !rootElement.isJsonArray()) {
                    createEmptyUsersFile();
                    return users;
                }

                JsonArray jsonArray = rootElement.getAsJsonArray();

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Playlist.class, new PlaylistAdapter());
                Gson gson = gsonBuilder.create();

                for (JsonElement element : jsonArray) {
                    try {
                        JsonObject obj = element.getAsJsonObject();

                        if (obj.get("username") == null || obj.get("passwordHash") == null ||
                                obj.get("accountType") == null) {
                            continue;
                        }

                        String username = obj.get("username").getAsString();
                        String passwordHash = obj.get("passwordHash").getAsString();
                        String accountType = obj.get("accountType").getAsString();

                        User user;
                        if ("free".equalsIgnoreCase(accountType)) {
                            user = new FreeUser(username, passwordHash);
                        } else if ("premium".equalsIgnoreCase(accountType)) {
                            user = new PremiumUser(username, passwordHash);
                        } else {
                            continue;
                        }

                        // Load playlists
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

                        // Load followed users
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

                // Robust followed users resolution
                resolveFollowedUsersRobust(users);

            }
        } catch (IOException | JsonSyntaxException e) {
            createEmptyUsersFile();
        }

        return users;
    }

    /**
     * Resolve followed user references with case-insensitive matching
     * Enhanced validation and error handling
     */
    private static void resolveFollowedUsersRobust(List<User> users) {
        for (Map.Entry<User, List<String>> entry : followedUsersMap.entrySet()) {
            User follower = entry.getKey();
            for (String username : entry.getValue()) {
                // Case-insensitive search and validation
                User potentialFollowed = users.stream()
                        .filter(u -> u.getUsername().equalsIgnoreCase(username))
                        .findFirst()
                        .orElse(null);

                if (potentialFollowed != null && !potentialFollowed.equals(follower)) {
                    follower.follow(potentialFollowed);
                    System.out.println("DEBUG: Resolved follow relationship: " +
                            follower.getUsername() + " -> " + potentialFollowed.getUsername());
                } else if (potentialFollowed == null) {
                    System.out.println("WARNING: Could not resolve followed user: " + username);
                }
            }
        }
        followedUsersMap.clear();
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
     */
    public static void saveUsers(List<User> users) {
        File tempFile = new File(USERS_FILE + ".tmp");
        File originalFile = new File(USERS_FILE);

        try {
            try (Writer writer = new FileWriter(tempFile)) {
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
                            if (followed != null && followed.getUsername() != null) {
                                followedArray.add(followed.getUsername());
                            }
                        }
                        obj.add("followedUsers", followedArray);
                    }

                    userArray.add(obj);
                }

                gson.toJson(userArray, writer);
                writer.flush();
            }

            // Atomic replacement
            if (tempFile.exists() && tempFile.length() > 0) {
                File backupFile = new File(USERS_FILE + ".bak");
                if (originalFile.exists()) {
                    Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

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

            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Add user with case-insensitive duplicate check
     */
    public static void addUser(User user) {
        List<User> users = loadUsers();

        // Case-insensitive duplicate check
        boolean exists = users.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername()));

        if (exists) {
            System.out.println("DEBUG: User already exists (case-insensitive), updating instead: " + user.getUsername());
            updateUser(user);
            return;
        }

        users.add(user);

        // Save with retry mechanism
        boolean saved = false;
        int attempts = 0;
        int maxAttempts = 3;

        while (!saved && attempts < maxAttempts) {
            try {
                saveUsers(users);
                saved = true;
                System.out.println("DEBUG: User saved successfully on attempt " + (attempts + 1));
            } catch (Exception e) {
                attempts++;
                System.out.println("DEBUG: Save attempt " + attempts + " failed: " + e.getMessage());
                if (attempts < maxAttempts) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!saved) {
            System.err.println("ERROR: Failed to save user after " + maxAttempts + " attempts");
        }
    }

    /**
     * Get user by username with case-insensitive search and better error handling
     */
    public static User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        try {
            List<User> users = loadUsers();
            return users.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to get user by username: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if username exists with case-insensitive comparison
     */
    public static boolean doesUserExist(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        List<User> users = loadUsers();
        return users.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()));
    }

    /**
     * Authenticate user with case-insensitive username
     */
    public static boolean authenticate(String username, String inputPasswordHash) {
        if (username == null || inputPasswordHash == null) {
            return false;
        }

        return loadUsers().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()) &&
                        u.getPasswordHash().equals(inputPasswordHash));
    }

    /**
     * Update user with case-insensitive search
     */
    public static void updateUser(User updatedUser) {
        if (updatedUser == null || updatedUser.getUsername() == null) {
            return;
        }

        List<User> users = loadUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equalsIgnoreCase(updatedUser.getUsername())) {
                users.set(i, updatedUser);
                break;
            }
        }
        saveUsers(users);
    }

    /**
     * Clean up orphaned followed users
     * Removes references to users that no longer exist
     */
    public static void cleanupOrphanedFollows() {
        List<User> users = loadUsers();
        boolean changesMade = false;

        for (User user : users) {
            List<User> validFollows = new ArrayList<>();

            for (User followed : user.getFollowedUsers()) {
                // Check if followed user still exists
                boolean stillExists = users.stream()
                        .anyMatch(u -> u.getUsername().equalsIgnoreCase(followed.getUsername()));

                if (stillExists) {
                    validFollows.add(followed);
                } else {
                    System.out.println("DEBUG: Removing orphaned follow: " +
                            user.getUsername() + " -> " + followed.getUsername());
                    changesMade = true;
                }
            }

            if (validFollows.size() != user.getFollowedUsers().size()) {
                user.getFollowedUsers().clear();
                user.getFollowedUsers().addAll(validFollows);
            }
        }

        if (changesMade) {
            saveUsers(users);
            System.out.println("DEBUG: Cleaned up orphaned follows");
        }
    }

    /**
     * Clean up invalid playlists for a user
     */
    public static void cleanupInvalidPlaylists(User user) {
        if (user != null) {
            List<Playlist> validPlaylists = new ArrayList<>();
            for (Playlist p : user.getPlaylists()) {
                if (!"LOGIN_SUCCESS".equals(p.getName()) && p.getName() != null) {
                    validPlaylists.add(p);
                }
            }

            if (validPlaylists.size() < user.getPlaylists().size()) {
                user.setPlaylists(validPlaylists);
                updateUser(user);
                System.out.println("DEBUG: Cleaned up invalid playlists for user: " + user.getUsername());
            }
        }
    }

    /**
     * Debug method to verify data consistency
     */
    public static void debugUserData() {
        try {
            List<User> users = loadUsers();
            System.out.println("=== USER DATA DEBUG ===");

            for (User user : users) {
                System.out.println("User: " + user.getUsername() + " (" + user.getAccountType() + ")");
                System.out.println("  Playlists: " + user.getPlaylists().size());
                System.out.println("  Following: " + user.getFollowedUsers().size());

                for (User followed : user.getFollowedUsers()) {
                    System.out.println("    -> " + followed.getUsername());
                }

                System.out.println("  Shares publicly: " + user.arePlaylistsSharedPublicly());
                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("DEBUG ERROR: " + e.getMessage());
        }
    }
}