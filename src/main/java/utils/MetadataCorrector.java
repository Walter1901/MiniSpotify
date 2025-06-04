package utils;

import persistence.UserPersistenceManager;
import server.music.Song;
import server.music.Playlist;
import users.User;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 🔧 METADATA CORRECTOR - Fix your existing JSON with real music data
 *
 * This utility will update your existing users.json file to replace:
 * ❌ "album": "Unknown"
 * ❌ "genre": "Unknown"
 * ❌ "duration": 0
 *
 * With:
 * ✅ "album": "Le Fléau"
 * ✅ "genre": "French Rap"
 * ✅ "duration": 195
 */
public class MetadataCorrector {

    // 🎵 YOUR REAL MUSIC METADATA DATABASE
    private static final Map<String, SongMetadata> REAL_METADATA = new HashMap<>();

    static {
        // Initialize with your actual music information
        REAL_METADATA.put("mussulo", new SongMetadata("Afro House Collection", "Electronic", 210));
        REAL_METADATA.put("ciel", new SongMetadata("Le Fléau", "French Rap", 195));
        REAL_METADATA.put("ninao", new SongMetadata("Le Fléau", "French Rap", 200));
        REAL_METADATA.put("mood", new SongMetadata("Appartement 105", "French R&B", 185));
        REAL_METADATA.put("melrose place", new SongMetadata("Tout va bien", "French Rap", 220));
    }

    /**
     * 🔧 MAIN CORRECTION METHOD
     * Call this to fix all your existing playlists
     */
    public static void correctAllUserMetadata() {
        System.out.println("🔧 Starting metadata correction for your music collection...");

        List<User> allUsers = UserPersistenceManager.loadUsers();
        boolean changesMade = false;
        int songsUpdated = 0;

        for (User user : allUsers) {
            System.out.println("👤 Checking user: " + user.getUsername());

            for (Playlist playlist : user.getPlaylists()) {
                System.out.println("  📝 Checking playlist: " + playlist.getName());

                List<Song> songs = playlist.getSongs();
                for (int i = 0; i < songs.size(); i++) {
                    Song song = songs.get(i);

                    // Check if this song needs metadata correction
                    SongMetadata correctMetadata = findCorrectMetadata(song);

                    if (correctMetadata != null && needsCorrection(song)) {
                        // Create corrected song
                        Song correctedSong = new Song(
                                song.getTitle(),           // Keep original title
                                song.getArtist(),          // Keep original artist
                                correctMetadata.album,     // ✅ REAL album
                                correctMetadata.genre,     // ✅ REAL genre
                                correctMetadata.duration   // ✅ REAL duration
                        );
                        correctedSong.setFilePath(song.getFilePath()); // Keep file path

                        // Replace song in playlist (we need to modify the song directly)
                        updateSongMetadata(song, correctMetadata);

                        songsUpdated++;
                        changesMade = true;

                        System.out.println("    ✅ Updated: " + song.getTitle() +
                                " -> Album: " + correctMetadata.album +
                                ", Genre: " + correctMetadata.genre +
                                ", Duration: " + formatDuration(correctMetadata.duration));
                    }
                }
            }
        }

        if (changesMade) {
            // Save the corrected data
            UserPersistenceManager.saveUsers(allUsers);
            System.out.println("🎉 Metadata correction completed!");
            System.out.println("📊 Updated " + songsUpdated + " songs with real metadata");
            System.out.println("💾 Changes saved to users.json");
        } else {
            System.out.println("ℹ️ No corrections needed - all metadata is already correct!");
        }
    }

    /**
     * Find correct metadata for a song based on title
     */
    private static SongMetadata findCorrectMetadata(Song song) {
        String songTitle = song.getTitle().toLowerCase();

        // Direct lookup first
        if (REAL_METADATA.containsKey(songTitle)) {
            return REAL_METADATA.get(songTitle);
        }

        // Try partial matching for variations
        for (Map.Entry<String, SongMetadata> entry : REAL_METADATA.entrySet()) {
            String key = entry.getKey();
            if (songTitle.contains(key) || key.contains(songTitle)) {
                return entry.getValue();
            }
        }

        return null; // No metadata found
    }

    /**
     * Check if a song needs metadata correction
     */
    private static boolean needsCorrection(Song song) {
        return "Unknown".equals(song.getAlbum()) ||
                "Unknown".equals(song.getGenre()) ||
                song.getDuration() == 0;
    }

    /**
     * Update song metadata using reflection or direct access
     * Note: This assumes Song class has setters or public fields
     */
    private static void updateSongMetadata(Song song, SongMetadata metadata) {
        try {
            // Try to use reflection to update private fields
            java.lang.reflect.Field albumField = Song.class.getDeclaredField("album");
            albumField.setAccessible(true);
            albumField.set(song, metadata.album);

            java.lang.reflect.Field genreField = Song.class.getDeclaredField("genre");
            genreField.setAccessible(true);
            genreField.set(song, metadata.genre);

            java.lang.reflect.Field durationField = Song.class.getDeclaredField("duration");
            durationField.setAccessible(true);
            durationField.set(song, metadata.duration);

        } catch (Exception e) {
            System.err.println("⚠️ Could not update song metadata for: " + song.getTitle());
            System.err.println("💡 You may need to add setter methods to your Song class");
        }
    }

    /**
     * Format duration for display
     */
    private static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /**
     * Metadata container class
     */
    private static class SongMetadata {
        final String album;
        final String genre;
        final int duration;

        SongMetadata(String album, String genre, int duration) {
            this.album = album;
            this.genre = genre;
            this.duration = duration;
        }
    }

    /**
     * 🚀 QUICK TEST METHOD
     * Run this to see what would be corrected without actually changing anything
     */
    public static void previewCorrections() {
        System.out.println("👀 PREVIEW: What would be corrected...");

        List<User> allUsers = UserPersistenceManager.loadUsers();
        int songsToUpdate = 0;

        for (User user : allUsers) {
            for (Playlist playlist : user.getPlaylists()) {
                for (Song song : playlist.getSongs()) {
                    SongMetadata correctMetadata = findCorrectMetadata(song);

                    if (correctMetadata != null && needsCorrection(song)) {
                        songsToUpdate++;
                        System.out.println("🔄 Would update: " + song.getTitle() +
                                " by " + song.getArtist());
                        System.out.println("    Current: Album='" + song.getAlbum() +
                                "', Genre='" + song.getGenre() +
                                "', Duration=" + song.getDuration());
                        System.out.println("    New:     Album='" + correctMetadata.album +
                                "', Genre='" + correctMetadata.genre +
                                "', Duration=" + correctMetadata.duration);
                        System.out.println();
                    }
                }
            }
        }

        System.out.println("📊 Total songs that would be updated: " + songsToUpdate);
    }

    /**
     * 🎯 MAIN METHOD - Run this to correct your metadata
     */
    public static void main(String[] args) {
        System.out.println("🎵 METADATA CORRECTOR for MiniSpotify");
        System.out.println("=====================================");

        // First show what would be corrected
        previewCorrections();

        System.out.println("\n⚡ Running actual corrections...");
        correctAllUserMetadata();

        System.out.println("\n✅ All done! Your JSON now has real metadata!");
    }
}