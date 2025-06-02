package server.music;

import java.io.File;
import java.util.List;
import persistence.UserPersistenceManager;
import users.User;

/**
 * Music loader implementing Singleton pattern.
 * Dynamically loads music from file system.
 * Follows Single Responsibility Principle - only handles music loading.
 */
public class MusicLoader {
    private static volatile MusicLoader instance = null;
    private boolean songsLoaded = false;

    /**
     * Private constructor for Singleton pattern
     */
    private MusicLoader() {}

    /**
     * Get Singleton instance
     * @return Single MusicLoader instance
     */
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
     * Load all songs from the MP3 directory
     * Supports both development and production paths
     */
    public void loadAllSongs() {
        if (!songsLoaded) {
            // Try development path first, then production path
            String devPath = "src/main/resources/mp3";
            String prodPath = "resources/mp3";

            File devDir = new File(devPath);
            File prodDir = new File(prodPath);

            File mp3Dir;
            if (devDir.exists() && devDir.isDirectory()) {
                mp3Dir = devDir;
            } else if (prodDir.exists() && prodDir.isDirectory()) {
                mp3Dir = prodDir;
            } else {
                System.out.println("No MP3 directory found. No songs loaded.");
                songsLoaded = true;
                return;
            }

            // Load MP3 files
            File[] mp3Files = mp3Dir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
            if (mp3Files == null || mp3Files.length == 0) {
                System.out.println("No MP3 files found in directory.");
                songsLoaded = true;
                return;
            }

            System.out.println("Loading " + mp3Files.length + " MP3 files...");
            createSongsFromFiles(mp3Files);
            songsLoaded = true;
        }
    }

    /**
     * Create Song objects from MP3 files
     * Extracts metadata from filename
     * @param mp3Files Array of MP3 files to process
     */
    private void createSongsFromFiles(File[] mp3Files) {
        for (File file : mp3Files) {
            String fileName = file.getName();
            String songTitle = extractTitleFromFileName(fileName);
            String artist = extractArtistFromFileName(fileName);

            Song song = new Song(songTitle, artist, "Unknown", "Unknown", 0);
            song.setFilePath(file.getAbsolutePath());

            MusicLibrary.getInstance().addSong(song);
        }
    }

    /**
     * Extract song title from filename
     * Supports format: "Artist - Title.mp3"
     * @param fileName Original filename
     * @return Extracted title
     */
    private String extractTitleFromFileName(String fileName) {
        // Remove .mp3 extension
        String title = fileName.substring(0, fileName.lastIndexOf('.'));

        // If filename contains " - ", extract the part after it as title
        if (title.contains(" - ")) {
            String[] parts = title.split(" - ", 2);
            return parts.length > 1 ? parts[1].trim() : parts[0].trim();
        }

        return title;
    }

    /**
     * Extract artist from filename
     * Supports format: "Artist - Title.mp3"
     * @param fileName Original filename
     * @return Extracted artist name
     */
    private String extractArtistFromFileName(String fileName) {
        // Remove .mp3 extension
        String title = fileName.substring(0, fileName.lastIndexOf('.'));

        // If filename contains " - ", extract the part before it as artist
        if (title.contains(" - ")) {
            String[] parts = title.split(" - ", 2);
            return parts[0].trim();
        }

        return "Unknown Artist";
    }

    /**
     * Update file paths for existing playlist songs
     * Repairs playlists after songs are loaded
     */
    public void repairExistingPlaylists() {
        List<User> allUsers = UserPersistenceManager.loadUsers();
        boolean changesFound = false;

        for (User user : allUsers) {
            for (Playlist playlist : user.getPlaylists()) {
                List<Song> songs = playlist.getSongs();
                for (Song playlistSong : songs) {
                    if (playlistSong.getFilePath() == null || playlistSong.getFilePath().isEmpty()) {
                        // Find matching song in library
                        for (Song librarySong : MusicLibrary.getInstance().getAllSongs()) {
                            if (librarySong.getTitle().equals(playlistSong.getTitle())) {
                                playlistSong.setFilePath(librarySong.getFilePath());
                                changesFound = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Save changes if any were made
        if (changesFound) {
            UserPersistenceManager.saveUsers(allUsers);
        }
    }
}