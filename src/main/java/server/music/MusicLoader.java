package server.music;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import persistence.UserPersistenceManager;
import users.User;

/**
 * Music loader implementing Singleton pattern - JAR COMPATIBLE VERSION
 * Dynamically loads music from file system with JAR support.
 */
public class MusicLoader {
    private static volatile MusicLoader instance = null;
    private boolean songsLoaded = false;

    private MusicLoader() {}

    public static MusicLoader getInstance() {
        if (instance == null) {
            synchronized (MusicLoader.class) {
                if (instance == null) {
                    instance = new MusicLoader();
                }
            }
        }
        return instance;
    }

    /**
     * Load all songs with JAR compatibility
     */
    public void loadAllSongs() {
        if (!songsLoaded) {
            File mp3Dir = findMp3Directory();

            if (mp3Dir == null) {
                System.out.println("❌ No MP3 directory found. No songs loaded.");
                songsLoaded = true;
                return;
            }

            File[] mp3Files = mp3Dir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
            if (mp3Files == null || mp3Files.length == 0) {
                System.out.println("❌ No MP3 files found in directory: " + mp3Dir.getAbsolutePath());
                songsLoaded = true;
                return;
            }

            System.out.println("🎵 Loading " + mp3Files.length + " MP3 files from: " + mp3Dir.getAbsolutePath());
            createSongsFromFiles(mp3Files);

            // Debug: Print all loaded songs
            System.out.println("🎵 Songs loaded in library:");
            for (Song song : MusicLibrary.getInstance().getAllSongs()) {
                System.out.println("  - Title: '" + song.getTitle() + "' by '" + song.getArtist() + "'");
            }

            songsLoaded = true;
        }
    }

    /**
     * Find MP3 directory with JAR compatibility
     */
    private File findMp3Directory() {
        // Try multiple possible locations
        String[] possiblePaths = {
                "src/main/resources/mp3",           // Development
                "resources/mp3",                    // Production
                "mp3",                             // Root level
                "./mp3",                           // Current directory
                "../mp3",                          // Parent directory
                "./src/main/resources/mp3"         // Alternative development
        };

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                System.out.println("✅ Found MP3 directory: " + dir.getAbsolutePath());
                return dir;
            }
        }

        // Try to find using class loader (for JAR)
        try {
            URL resourceUrl = MusicLoader.class.getClassLoader().getResource("mp3");
            if (resourceUrl != null) {
                File dir = Paths.get(resourceUrl.toURI()).toFile();
                if (dir.exists() && dir.isDirectory()) {
                    System.out.println("✅ Found MP3 directory via classloader: " + dir.getAbsolutePath());
                    return dir;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not find MP3 directory via classloader: " + e.getMessage());
        }

        // Try current working directory
        String workingDir = System.getProperty("user.dir");
        File workingDirMp3 = new File(workingDir, "mp3");
        if (workingDirMp3.exists() && workingDirMp3.isDirectory()) {
            System.out.println("✅ Found MP3 directory in working directory: " + workingDirMp3.getAbsolutePath());
            return workingDirMp3;
        }

        System.out.println("❌ MP3 directory not found. Tried paths:");
        for (String path : possiblePaths) {
            System.out.println("  - " + new File(path).getAbsolutePath());
        }
        System.out.println("  - Working directory: " + workingDir);

        return null;
    }

    /**
     * Create Song objects from MP3 files with improved parsing
     */
    private void createSongsFromFiles(File[] mp3Files) {
        for (File file : mp3Files) {
            String fileName = file.getName();
            String songTitle = extractTitleFromFileName(fileName);
            String artist = extractArtistFromFileName(fileName);

            Song song = new Song(songTitle, artist, "Unknown", "Unknown", 0);
            song.setFilePath(file.getAbsolutePath());

            System.out.println("🎵 Created song: '" + songTitle + "' by '" + artist + "' from file: " + fileName);
            MusicLibrary.getInstance().addSong(song);
        }
    }

    /**
     * Extract song title from filename with comprehensive cleaning
     */
    private String extractTitleFromFileName(String fileName) {
        // Remove .mp3 extension
        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

        // Clean up common patterns in filenames
        String cleaned = nameWithoutExtension
                .replaceAll("\\(Official.*?\\)", "")     // Remove (Official Video), etc.
                .replaceAll("\\(Clip.*?\\)", "")         // Remove (Clip Officiel), etc.
                .replaceAll("\\(Lyrics.*?\\)", "")       // Remove (Lyrics Video), etc.
                .replaceAll("\\(Audio.*?\\)", "")        // Remove (Audio), etc.
                .replaceAll("\\[.*?\\]", "")             // Remove [anything]
                .replaceAll("\\{.*?\\}", "")             // Remove {anything}
                .replaceAll("HD|4K|1080p|720p", "")      // Remove quality indicators
                .replaceAll("\\s+", " ")                 // Normalize whitespace
                .trim();

        // If filename contains " - ", extract the part after it as title
        if (cleaned.contains(" - ")) {
            String[] parts = cleaned.split(" - ", 2);
            if (parts.length > 1) {
                return parts[1].trim();
            } else {
                return parts[0].trim();
            }
        }

        return cleaned;
    }

    /**
     * Extract artist from filename with comprehensive cleaning
     */
    private String extractArtistFromFileName(String fileName) {
        // Remove .mp3 extension
        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

        // Clean up common patterns
        String cleaned = nameWithoutExtension
                .replaceAll("\\(Official.*?\\)", "")
                .replaceAll("\\(Clip.*?\\)", "")
                .replaceAll("\\(Lyrics.*?\\)", "")
                .replaceAll("\\(Audio.*?\\)", "")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\{.*?\\}", "")
                .replaceAll("HD|4K|1080p|720p", "")
                .replaceAll("\\s+", " ")
                .trim();

        // If filename contains " - ", extract the part before it as artist
        if (cleaned.contains(" - ")) {
            String[] parts = cleaned.split(" - ", 2);
            return parts[0].trim();
        }

        return "Unknown Artist";
    }

    /**
     * Update file paths for existing playlist songs
     */
    public void repairExistingPlaylists() {
        List<User> allUsers = UserPersistenceManager.loadUsers();
        boolean changesFound = false;

        for (User user : allUsers) {
            for (Playlist playlist : user.getPlaylists()) {
                List<Song> songs = playlist.getSongs();
                for (Song playlistSong : songs) {
                    if (playlistSong.getFilePath() == null || playlistSong.getFilePath().isEmpty()) {
                        // Find matching song in library (case insensitive)
                        for (Song librarySong : MusicLibrary.getInstance().getAllSongs()) {
                            if (librarySong.getTitle().equalsIgnoreCase(playlistSong.getTitle())) {
                                playlistSong.setFilePath(librarySong.getFilePath());
                                changesFound = true;
                                System.out.println("🔧 Repaired file path for song: " + playlistSong.getTitle());
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (changesFound) {
            UserPersistenceManager.saveUsers(allUsers);
            System.out.println("✅ Playlist repairs saved to persistence");
        }
    }
}