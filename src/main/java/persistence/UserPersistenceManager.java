package persistence;

import com.google.gson.*;
import server.music.Playlist;
import server.music.PlaylistAdapter;
import server.music.Song;
import server.music.CollaborativePlaylist;
import users.User;
import users.FreeUser;
import users.PremiumUser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages user persistence using JSON files
 */
public class UserPersistenceManager {

    private static final String USERS_FILE = "users.json";

    /**
     * Load all users from the JSON file
     */
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);

        try {
            // If file doesn't exist, create it with an empty array
            if (!file.exists()) {
                System.out.println("Users file does not exist. Creating new file: " + USERS_FILE);
                createEmptyUsersFile();
                return users;
            }

            // Check if file is empty
            if (file.length() == 0) {
                System.out.println("Users file is empty. Initializing with empty array.");
                createEmptyUsersFile();
                return users;
            }

            try (Reader reader = new FileReader(USERS_FILE)) {
                // Try to read file as JsonElement for validation
                JsonElement rootElement = JsonParser.parseReader(reader);

                // Check if root element is null
                if (rootElement == null) {
                    System.out.println("Invalid JSON file. Reinitializing with empty array.");
                    createEmptyUsersFile();
                    return users;
                }

                // Check if element is an array
                if (!rootElement.isJsonArray()) {
                    System.out.println("JSON file does not contain an array. Reinitializing with empty array.");
                    createEmptyUsersFile();
                    return users;
                }

                JsonArray jsonArray = rootElement.getAsJsonArray();

                // Create adapter for playlists
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Playlist.class, new PlaylistAdapter());
                Gson gson = gsonBuilder.create();

                for (JsonElement element : jsonArray) {
                    try {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.get("username") == null || obj.get("passwordHash") == null || obj.get("accountType") == null) {
                            System.err.println("Incomplete or corrupt user entry in users.json, skipped.");
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
                            continue; // Unknown account type
                        }

                        // Load playlists if present
                        if (obj.has("playlists") && obj.get("playlists").isJsonArray()) {
                            JsonArray playlistsArray = obj.getAsJsonArray("playlists");
                            for (JsonElement playlistElement : playlistsArray) {
                                try {
                                    Playlist playlist = gson.fromJson(playlistElement, Playlist.class);
                                    user.addPlaylist(playlist);
                                } catch (Exception e) {
                                    System.err.println("Error loading a playlist: " + e.getMessage());
                                }
                            }
                        }

                        // Load followed users if present
                        if (obj.has("followedUsers") && obj.get("followedUsers").isJsonArray()) {
                            JsonArray followedArray = obj.getAsJsonArray("followedUsers");
                            // Store usernames to resolve after all users are loaded
                            List<String> followedUsernames = new ArrayList<>();
                            for (JsonElement followedElement : followedArray) {
                                followedUsernames.add(followedElement.getAsString());
                            }
                            // Store for later resolution
                            followedUsersMap.put(user, followedUsernames);
                        }

                        // Load sharing preferences
                        if (obj.has("sharePlaylistsPublicly")) {
                            user.setSharePlaylistsPublicly(obj.get("sharePlaylistsPublicly").getAsBoolean());
                        }

                        users.add(user);
                    } catch (Exception e) {
                        System.err.println("Error processing user entry: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Resolve followed users
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
                // Clear map after resolving
                followedUsersMap.clear();
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Error loading users: " + e.getMessage());
            // Try to recover by creating a new empty file
            createEmptyUsersFile();
        }

        return users;
    }

    // Temporary storage for followed users (needed for resolution after all users are loaded)
    private static Map<User, List<String>> followedUsersMap = new HashMap<>();

    /**
     * Create an empty users.json file with an empty JSON array
     */
    private static void createEmptyUsersFile() {
        try (Writer writer = new FileWriter(USERS_FILE)) {
            writer.write("[]");
            System.out.println("Created empty users file: " + USERS_FILE);
        } catch (IOException e) {
            System.err.println("Failed to create empty users file: " + e.getMessage());
        }
    }

    /**
     * Save all users to the JSON file
     */
    public static void saveUsers(List<User> users) {
        // Create a temporary file for writing
        File tempFile = new File(USERS_FILE + ".tmp");
        File originalFile = new File(USERS_FILE);

        try {
            // First write to temporary file
            try (Writer writer = new FileWriter(tempFile)) {
                // Important: Register the PlaylistAdapter with the type hierarchy adapter
                GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
                gsonBuilder.registerTypeAdapter(Playlist.class, new PlaylistAdapter());
                // Also register the specific subclass
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
                            // Skip special system playlists
                            if ("LOGIN_SUCCESS".equals(playlist.getName())) {
                                continue;
                            }

                            // Print debug info
                            System.out.println("Serializing playlist: " + playlist.getName() +
                                    " (type: " + (playlist instanceof CollaborativePlaylist ? "collaborative" : "standard") + ")");

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
                writer.flush(); // Ensure all data is written
            }

            // Check that temp file exists and has correct size
            if (tempFile.exists() && tempFile.length() > 0) {
                // Make backup of original file (if needed)
                File backupFile = new File(USERS_FILE + ".bak");
                if (originalFile.exists()) {
                    Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // Replace original file with temp file
                Files.move(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Delete backup after successful replacement (optional)
                // backupFile.delete();
            } else {
                throw new IOException("Failed to write temporary file or file is empty");
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());

            // In case of error, restore backup if it exists
            File backupFile = new File(USERS_FILE + ".bak");
            if (backupFile.exists() && originalFile.exists() && originalFile.length() == 0) {
                try {
                    Files.copy(backupFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Restored backup file after error.");
                } catch (IOException restoreError) {
                    System.err.println("Failed to restore backup: " + restoreError.getMessage());
                }
            }

            // Delete temp file if it still exists
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Add one user and save immediately
     */
    public static void addUser(User user) {
        List<User> users = loadUsers();
        users.add(user);
        saveUsers(users);
    }

    /**
     * Check if a username already exists
     */
    public static boolean doesUserExist(String username) {
        List<User> users = loadUsers();
        return users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    /**
     * Authenticate user by username and hashed password
     */
    public static boolean authenticate(String username, String inputPasswordHash) {
        return loadUsers().stream()
                .anyMatch(u -> u.getUsername().equals(username) &&
                        u.getPasswordHash().equals(inputPasswordHash));
    }

    /**
     * Update a specific user
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
                System.out.println("Removed invalid playlists for user: " + user.getUsername());
            }
        }
    }

    /**
     * Get a user by username
     */
    public static User getUserByUsername(String username) {
        return loadUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}