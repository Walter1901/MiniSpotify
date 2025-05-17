package persistence;

import com.google.gson.*;
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
        try (Reader reader = new FileReader(USERS_FILE)) {
            JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
            // Java
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.get("username") == null || obj.get("passwordHash") == null || obj.get("accountType") == null) {
                    System.err.println("Entrée utilisateur incomplète ou corrompue dans users.json, ignorée.");
                    continue;
                }
                String username = obj.get("username").getAsString();
                String passwordHash = obj.get("passwordHash").getAsString();
                String accountType = obj.get("accountType").getAsString();
                if ("free".equalsIgnoreCase(accountType)) {
                    users.add(new FreeUser(username, passwordHash));
                } else if ("premium".equalsIgnoreCase(accountType)) {
                    users.add(new PremiumUser(username, passwordHash));
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
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonArray userArray = new JsonArray();
            for (User user : users) {
                JsonObject obj = new JsonObject();
                obj.addProperty("username", user.getUsername());
                obj.addProperty("passwordHash", user.getPasswordHash());
                obj.addProperty("accountType", user.getAccountType());
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
}
