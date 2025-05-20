package persistence;

import com.google.gson.*;
import server.music.Playlist;
import server.music.PlaylistAdapter;
import server.music.Song;
import users.User;
import users.FreeUser;
import users.PremiumUser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class UserPersistenceManager {

    private static final String USERS_FILE = "users.json";

    // Load all users from the JSON file
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);

        try {
            // Si le fichier n'existe pas, le créer avec un tableau vide
            if (!file.exists()) {
                System.out.println("Users file does not exist. Creating new file: " + USERS_FILE);
                createEmptyUsersFile();
                return users;
            }

            // Vérifier si le fichier est vide
            if (file.length() == 0) {
                System.out.println("Users file is empty. Initializing with empty array.");
                createEmptyUsersFile();
                return users;
            }

            try (Reader reader = new FileReader(USERS_FILE)) {
                // Tenter de lire le fichier en tant que JsonElement pour validation
                JsonElement rootElement = JsonParser.parseReader(reader);

                // Vérifier si l'élément racine est null
                if (rootElement == null) {
                    System.out.println("Invalid JSON file. Reinitializing with empty array.");
                    createEmptyUsersFile();
                    return users;
                }

                // Vérifier si l'élément est un tableau
                if (!rootElement.isJsonArray()) {
                    System.out.println("JSON file does not contain an array. Reinitializing with empty array.");
                    createEmptyUsersFile();
                    return users;
                }

                JsonArray jsonArray = rootElement.getAsJsonArray();

                // Créer un adaptateur pour les playlists
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

                        users.add(user);
                    } catch (Exception e) {
                        System.err.println("Error processing user entry: " + e.getMessage());
                    }
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Error loading users: " + e.getMessage());
            // Try to recover by creating a new empty file
            createEmptyUsersFile();
        }

        return users;
    }

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

    // Save all users to the JSON file
    public static void saveUsers(List<User> users) {
        // Créer un fichier temporaire pour l'écriture
        File tempFile = new File(USERS_FILE + ".tmp");
        File originalFile = new File(USERS_FILE);

        try {
            // Écrire d'abord dans le fichier temporaire
            try (Writer writer = new FileWriter(tempFile)) {
                GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
                gsonBuilder.registerTypeAdapter(Playlist.class, new PlaylistAdapter());
                Gson gson = gsonBuilder.create();

                JsonArray userArray = new JsonArray();
                for (User user : users) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("username", user.getUsername());
                    obj.addProperty("passwordHash", user.getPasswordHash());
                    obj.addProperty("accountType", user.getAccountType());

                    // Sauvegarder les playlists
                    if (!user.getPlaylists().isEmpty()) {
                        JsonArray playlistsArray = new JsonArray();
                        for (Playlist playlist : user.getPlaylists()) {
                            JsonElement playlistElement = gson.toJsonTree(playlist);
                            playlistsArray.add(playlistElement);
                        }
                        obj.add("playlists", playlistsArray);
                    }

                    userArray.add(obj);
                }
                gson.toJson(userArray, writer);
                writer.flush(); // S'assurer que toutes les données sont écrites
            }

            // Vérifier que le fichier temporaire existe et a une taille correcte
            if (tempFile.exists() && tempFile.length() > 0) {
                // Faire une sauvegarde du fichier original (si besoin)
                File backupFile = new File(USERS_FILE + ".bak");
                if (originalFile.exists()) {
                    Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // Remplacer le fichier original par le fichier temporaire
                Files.move(tempFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Supprimer la sauvegarde après un remplacement réussi (optionnel)
                // backupFile.delete();
            } else {
                throw new IOException("Failed to write temporary file or file is empty");
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());

            // En cas d'erreur, restaurer la sauvegarde si elle existe
            File backupFile = new File(USERS_FILE + ".bak");
            if (backupFile.exists() && originalFile.exists() && originalFile.length() == 0) {
                try {
                    Files.copy(backupFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Restored backup file after error.");
                } catch (IOException restoreError) {
                    System.err.println("Failed to restore backup: " + restoreError.getMessage());
                }
            }

            // Supprimer le fichier temporaire s'il existe encore
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // Add one user and save immediately
    public static void addUser(User user) {
        List<User> users = loadUsers();
        users.add(user);
        saveUsers(users);
    }

    // Check if a username already exists
    public static boolean doesUserExist(String username) {
        List<User> users = loadUsers();
        return users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    // Authenticate user by username and hashed password
    public static boolean authenticate(String username, String inputPasswordHash) {
        return loadUsers().stream()
                .anyMatch(u -> u.getUsername().equals(username) &&
                        u.getPasswordHash().equals(inputPasswordHash));
    }

    // Mettre à jour un utilisateur spécifique
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

    // Obtenir un utilisateur par son nom d'utilisateur
    public static User getUserByUsername(String username) {
        return loadUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}