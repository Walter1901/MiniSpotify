package persistence;

import com.google.gson.*;
import server.music.Playlist;
import server.music.PlaylistAdapter;
import server.music.Song;
import users.User;
import users.FreeUser;
import users.PremiumUser;

import java.io.*;
import java.util.*;

public class UserPersistenceManager {

    private static final String USERS_FILE = "users.json";

    // Load all users from the JSON file
    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) {
                file.createNewFile();
                // Créer un tableau JSON vide et l'écrire dans le fichier
                try (Writer writer = new FileWriter(USERS_FILE)) {
                    writer.write("[]");
                }
                return users;
            }

            try (Reader reader = new FileReader(USERS_FILE)) {
                JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();

                // Créer un adaptateur pour les playlists
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Playlist.class, new PlaylistAdapter());
                Gson gson = gsonBuilder.create();

                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.get("username") == null || obj.get("passwordHash") == null || obj.get("accountType") == null) {
                        System.err.println("Entrée utilisateur incomplète ou corrompue dans users.json, ignorée.");
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
                        continue; // Type de compte inconnu
                    }

                    // Charger les playlists si présentes
                    if (obj.has("playlists") && obj.get("playlists").isJsonArray()) {
                        JsonArray playlistsArray = obj.getAsJsonArray("playlists");
                        for (JsonElement playlistElement : playlistsArray) {
                            try {
                                Playlist playlist = gson.fromJson(playlistElement, Playlist.class);
                                user.addPlaylist(playlist);
                            } catch (Exception e) {
                                System.err.println("Erreur lors du chargement d'une playlist: " + e.getMessage());
                            }
                        }
                    }

                    users.add(user);
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }

        return users;
    }

    // Save all users to the JSON file
    public static void saveUsers(List<User> users) {
        try (Writer writer = new FileWriter(USERS_FILE)) {
            // Créer un adaptateur pour les playlists
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
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
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