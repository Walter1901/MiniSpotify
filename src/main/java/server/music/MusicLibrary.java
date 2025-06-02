package server.music;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized music library implementing Singleton pattern.
 * Manages all songs available in the system.
 * Follows Single Responsibility Principle - only handles song storage and retrieval.
 */
public class MusicLibrary {
    // Singleton instance (volatile for thread safety)
    private static volatile MusicLibrary instance = null;

    // Song storage
    private List<Song> songs = new ArrayList<>();

    /**
     * Private constructor for Singleton pattern
     */
    private MusicLibrary() {}

    /**
     * Get Singleton instance using Double-Checked Locking
     * @return Single MusicLibrary instance
     */
    public static MusicLibrary getInstance() {
        if (instance == null) {
            synchronized (MusicLibrary.class) {
                if (instance == null) {
                    instance = new MusicLibrary();
                }
            }
        }
        return instance;
    }

    /**
     * Add a song to the library
     * Prevents duplicate songs
     * @param song Song to add
     */
    public void addSong(Song song) {
        if (song != null && !songs.contains(song)) {
            songs.add(song);
        }
    }

    /**
     * Search songs by title (case insensitive partial match)
     * @param title Title to search for
     * @return List of matching songs
     */
    public List<Song> searchByTitle(String title) {
        if (title == null || title.isEmpty()) {
            return new ArrayList<>();
        }

        return songs.stream()
                .filter(s -> s.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Filter songs by artist (case insensitive partial match)
     * @param artist Artist name to filter by
     * @return List of songs by the artist
     */
    public List<Song> filterByArtist(String artist) {
        if (artist == null || artist.isEmpty()) {
            return new ArrayList<>();
        }

        return songs.stream()
                .filter(s -> s.getArtist().toLowerCase().contains(artist.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Get all songs in the library
     * @return Defensive copy of all songs
     */
    public List<Song> getAllSongs() {
        return new ArrayList<>(songs);
    }

    /**
     * Get total number of songs
     * @return Number of songs in library
     */
    public int size() {
        return songs.size();
    }
}