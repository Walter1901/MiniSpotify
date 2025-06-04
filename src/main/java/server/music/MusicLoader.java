package server.music;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import persistence.UserPersistenceManager;
import users.User;

/**
 * Enhanced Music loader with REAL metadata for your music collection
 * including proper Album, Genre, and Duration information!
 */
public class MusicLoader {
    private static volatile MusicLoader instance = null;
    private boolean songsLoaded = false;

    // Your real music metadata
    private static final Map<String, RealMusicMetadata> YOUR_MUSIC_DATABASE = new HashMap<>();

    static {
        YOUR_MUSIC_DATABASE.put("mussulo", new RealMusicMetadata(
                "Mussulo",
                "Dj Aka-m e Dj Malvado Feat Dody",
                "Afro House Collection",
                "Electronic",
                210
        ));

        YOUR_MUSIC_DATABASE.put("ciel", new RealMusicMetadata(
                "Ciel",
                "GIMS",
                "Le Fléau",
                "French Rap",
                195
        ));

        YOUR_MUSIC_DATABASE.put("ninao", new RealMusicMetadata(
                "NINAO",
                "GIMS",
                "Le Fléau",
                "French Rap",
                200
        ));

        YOUR_MUSIC_DATABASE.put("mood", new RealMusicMetadata(
                "Mood",
                "Keblack",
                "Appartement 105",
                "French R&B",
                185
        ));

        YOUR_MUSIC_DATABASE.put("melrose place", new RealMusicMetadata(
                "Melrose Place",
                "Keblack Ft. Guy2Bezbar",
                "Tout va bien",
                "French Rap",
                220
        ));
    }

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
     * Load all songs with enhanced JAR compatibility
     */
    public void loadAllSongs() {
        if (!songsLoaded) {
            File mp3Dir = findMp3Directory();

            if (mp3Dir == null) {
                System.out.println("❌ No MP3 directory found. Searched in:");
                printSearchPaths();
                createEmptyLibrary();
                return;
            }

            System.out.println("✅ Found MP3 directory: " + mp3Dir.getAbsolutePath());

            File[] mp3Files = mp3Dir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".mp3"));

            if (mp3Files == null || mp3Files.length == 0) {
                System.out.println("❌ No MP3 files found in directory: " + mp3Dir.getAbsolutePath());
                createEmptyLibrary();
                return;
            }

            createSongsWithRealMetadata(mp3Files, mp3Dir);
            songsLoaded = true;
        }
    }

    /**
     * Create songs with proper relative paths for JAR deployment
     */
    private void createSongsWithRealMetadata(File[] mp3Files, File mp3Dir) {
        System.out.println("🎵 Loading " + mp3Files.length + " songs with real metadata...");

        for (File file : mp3Files) {
            String fileName = file.getName();
            String songKey = extractSongKeyFromFileName(fileName);
            RealMusicMetadata metadata = YOUR_MUSIC_DATABASE.get(songKey);

            Song song;
            if (metadata != null) {
                song = new Song(
                        metadata.title,
                        metadata.artist,
                        metadata.album,
                        metadata.genre,
                        metadata.duration
                );

                System.out.println("✅ Loaded: " + metadata.title + " by " + metadata.artist +
                        " (" + metadata.album + ", " + metadata.genre + ", " +
                        formatDuration(metadata.duration) + ")");
            } else {
                String title = extractTitleFromFileName(fileName);
                String artist = extractArtistFromFileName(fileName);

                song = new Song(title, artist, "Various Artists", "Mixed", 180);
                System.out.println("⚠️ Unknown song, using extracted data: " + title + " by " + artist);
            }

            // Set RELATIVE path that works for both development and JAR
            String relativePath = createRelativePath(file, mp3Dir);
            song.setFilePath(relativePath);
            MusicLibrary.getInstance().addSong(song);
        }

        System.out.println("🎉 Successfully loaded " + mp3Files.length + " songs!");
    }

    /**
     * Create relative path that works in both development and JAR deployment
     */
    private String createRelativePath(File file, File mp3Dir) {
        try {
            // For JAR deployment, use simple relative path
            return "mp3/" + file.getName();
        } catch (Exception e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * Enhanced MP3 directory finder for JAR deployment
     */
    private File findMp3Directory() {
        // Get the directory where the JAR is located
        File jarDir = getJarDirectory();

        String[] paths = {
                // 1. Next to JAR file
                new File(jarDir, "mp3").getAbsolutePath(),

                // 2. Current working directory
                "./mp3",
                "mp3",

                // 3. User home directory
                System.getProperty("user.home") + "/MiniSpotify/mp3",

                // 4. Development paths
                "src/main/resources/mp3",
                "./src/main/resources/mp3",

                // 5. Relative to working directory
                System.getProperty("user.dir") + "/mp3"
        };

        for (String path : paths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] mp3Files = dir.listFiles((d, name) ->
                        name.toLowerCase().endsWith(".mp3"));
                if (mp3Files != null && mp3Files.length > 0) {
                    System.out.println("✅ Found MP3 directory: " + dir.getAbsolutePath());
                    return dir;
                }
            }
        }

        return null;
    }

    /**
     * Get the directory where the JAR is located
     */
    private File getJarDirectory() {
        try {
            String jarPath = MusicLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            return jarFile.isDirectory() ? jarFile : jarFile.getParentFile();
        } catch (Exception e) {
            return new File(".");
        }
    }

    /**
     * Print all search paths for debugging
     */
    private void printSearchPaths() {
        File jarDir = getJarDirectory();

        String[] paths = {
                new File(jarDir, "mp3").getAbsolutePath(),
                "./mp3",
                "mp3",
                System.getProperty("user.home") + "/MiniSpotify/mp3",
                "src/main/resources/mp3",
                System.getProperty("user.dir") + "/mp3"
        };

        for (String path : paths) {
            File dir = new File(path);
            System.out.println("  - " + path + " (exists: " + dir.exists() + ")");
        }
    }

    /**
     * Create empty library when no songs found
     */
    private void createEmptyLibrary() {
        System.out.println("📝 Creating empty music library");
        System.out.println("💡 To add music:");
        System.out.println("   1. Create an 'mp3' folder next to the JAR file");
        System.out.println("   2. Copy your MP3 files into that folder");
        System.out.println("   3. Restart the server");
        songsLoaded = true;
    }

    // Helper methods
    private String extractSongKeyFromFileName(String fileName) {
        String key = fileName.toLowerCase()
                .replace(".mp3", "")
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("feat\\.?", "ft.")
                .replaceAll("\\s+", " ")
                .trim();

        if (key.contains(" - ")) {
            String[] parts = key.split(" - ");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }

        return key;
    }

    private String extractTitleFromFileName(String fileName) {
        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

        String cleaned = nameWithoutExtension
                .replaceAll("\\(Official.*?\\)", "")
                .replaceAll("\\(Clip.*?\\)", "")
                .replaceAll("\\(Lyrics.*?\\)", "")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\{.*?\\}", "")
                .replaceAll("HD|4K|1080p|720p", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.contains(" - ")) {
            String[] parts = cleaned.split(" - ", 2);
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }

        return cleaned;
    }

    private String extractArtistFromFileName(String fileName) {
        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

        String cleaned = nameWithoutExtension
                .replaceAll("\\(Official.*?\\)", "")
                .replaceAll("\\(Clip.*?\\)", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.contains(" - ")) {
            String[] parts = cleaned.split(" - ", 2);
            return parts[0].trim();
        }

        return "Unknown Artist";
    }

    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /**
     * Metadata class
     */
    private static class RealMusicMetadata {
        final String title;
        final String artist;
        final String album;
        final String genre;
        final int duration;

        RealMusicMetadata(String title, String artist, String album, String genre, int duration) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.genre = genre;
            this.duration = duration;
        }
    }
}