package utils;

import com.google.gson.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Script to automatically fix file paths in users.json
 * Converts absolute paths to relative paths for JAR deployment
 */
public class PathFixer {

    private static final String USERS_JSON = "users.json";
    private static final String BACKUP_SUFFIX = ".backup";

    public static void main(String[] args) {
        System.out.println("🔧 PATH FIXER - Converting paths for JAR deployment");
        System.out.println("==================================================");

        try {
            fixFilePaths();
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fix all file paths in users.json
     */
    public static void fixFilePaths() throws IOException {
        File jsonFile = new File(USERS_JSON);

        if (!jsonFile.exists()) {
            System.err.println("❌ users.json file not found!");
            return;
        }

        // Create backup
        File backupFile = new File(USERS_JSON + BACKUP_SUFFIX);
        Files.copy(jsonFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("💾 Backup created: " + backupFile.getName());

        // Read JSON
        JsonArray usersArray;
        try (FileReader reader = new FileReader(jsonFile)) {
            usersArray = JsonParser.parseReader(reader).getAsJsonArray();
        }

        int totalSongsFixed = 0;
        int usersProcessed = 0;

        // Process each user
        for (JsonElement userElement : usersArray) {
            JsonObject user = userElement.getAsJsonObject();
            String username = user.get("username").getAsString();

            System.out.println("\n👤 Processing user: " + username);

            if (user.has("playlists")) {
                JsonArray playlists = user.getAsJsonArray("playlists");
                int userSongsFixed = 0;

                for (JsonElement playlistElement : playlists) {
                    JsonObject playlist = playlistElement.getAsJsonObject();
                    String playlistName = playlist.get("name").getAsString();

                    if (playlist.has("songs")) {
                        JsonArray songs = playlist.getAsJsonArray("songs");

                        for (JsonElement songElement : songs) {
                            JsonObject song = songElement.getAsJsonObject();

                            if (song.has("filePath")) {
                                String oldPath = song.get("filePath").getAsString();
                                String newPath = fixSinglePath(oldPath);

                                if (!oldPath.equals(newPath)) {
                                    song.addProperty("filePath", newPath);
                                    userSongsFixed++;
                                    totalSongsFixed++;

                                    String songTitle = song.has("title") ?
                                            song.get("title").getAsString() : "Unknown";

                                    System.out.println("  🎵 " + songTitle);
                                    System.out.println("    Old: " + oldPath);
                                    System.out.println("    New: " + newPath);
                                }
                            }
                        }
                    }
                }

                if (userSongsFixed > 0) {
                    System.out.println("  ✅ " + userSongsFixed + " paths fixed for " + username);
                } else {
                    System.out.println("  ℹ️ No fixes needed for " + username);
                }
            }

            usersProcessed++;
        }

        // Save the corrected file
        if (totalSongsFixed > 0) {
            try (FileWriter writer = new FileWriter(jsonFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(usersArray, writer);
            }

            System.out.println("\n🎉 FIXING COMPLETE!");
            System.out.println("📊 Statistics:");
            System.out.println("  - Users processed: " + usersProcessed);
            System.out.println("  - Paths fixed: " + totalSongsFixed);
            System.out.println("  - File saved: " + USERS_JSON);
            System.out.println("  - Backup: " + backupFile.getName());
            System.out.println("\n✅ Your users.json is now ready for JAR deployment!");

        } else {
            System.out.println("\nℹ️ No fixes needed - all paths are already correct!");

            // Remove backup if no changes
            if (backupFile.exists()) {
                backupFile.delete();
            }
        }
    }

    /**
     * Fix a single file path
     */
    private static String fixSinglePath(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) {
            return originalPath;
        }

        // If it's already a correct relative path, don't change
        if (originalPath.startsWith("mp3/") && !originalPath.contains("\\") && !originalPath.contains("C:")) {
            return originalPath;
        }

        // Extract filename
        String fileName = extractFileName(originalPath);

        // Return new relative path
        return "mp3/" + fileName;
    }

    /**
     * Extract filename from full path
     */
    private static String extractFileName(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return fullPath;
        }

        // Handle Windows and Unix separators
        String fileName = fullPath;

        // Split by \ (Windows)
        int lastBackslash = fileName.lastIndexOf('\\');
        if (lastBackslash >= 0) {
            fileName = fileName.substring(lastBackslash + 1);
        }

        // Split by / (Unix)
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }

        return fileName;
    }

    /**
     * Method to preview changes without applying them
     */
    public static void previewChanges() throws IOException {
        File jsonFile = new File(USERS_JSON);

        if (!jsonFile.exists()) {
            System.err.println("❌ users.json file not found!");
            return;
        }

        System.out.println("👀 PREVIEW OF CHANGES (no modifications)");
        System.out.println("========================================");

        JsonArray usersArray;
        try (FileReader reader = new FileReader(jsonFile)) {
            usersArray = JsonParser.parseReader(reader).getAsJsonArray();
        }

        int totalChanges = 0;

        for (JsonElement userElement : usersArray) {
            JsonObject user = userElement.getAsJsonObject();
            String username = user.get("username").getAsString();

            if (user.has("playlists")) {
                JsonArray playlists = user.getAsJsonArray("playlists");

                for (JsonElement playlistElement : playlists) {
                    JsonObject playlist = playlistElement.getAsJsonObject();

                    if (playlist.has("songs")) {
                        JsonArray songs = playlist.getAsJsonArray("songs");

                        for (JsonElement songElement : songs) {
                            JsonObject song = songElement.getAsJsonObject();

                            if (song.has("filePath")) {
                                String oldPath = song.get("filePath").getAsString();
                                String newPath = fixSinglePath(oldPath);

                                if (!oldPath.equals(newPath)) {
                                    totalChanges++;
                                    String songTitle = song.has("title") ?
                                            song.get("title").getAsString() : "Unknown";

                                    System.out.println("📝 " + username + " - " + songTitle);
                                    System.out.println("   Current: " + oldPath);
                                    System.out.println("   New: " + newPath);
                                    System.out.println();
                                }
                            }
                        }
                    }
                }
            }
        }

        if (totalChanges == 0) {
            System.out.println("✅ No changes needed - all paths are correct!");
        } else {
            System.out.println("📊 Total changes to apply: " + totalChanges);
            System.out.println("💡 Run fixFilePaths() to apply these changes");
        }
    }
}