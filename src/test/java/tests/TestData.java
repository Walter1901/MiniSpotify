package tests;

import server.music.Song;
import server.music.Playlist;
import users.*;
import utils.SecurePasswordHasher;
import java.util.List;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🎵 SIMPLE TEST DATA HELPER - Based on YOUR Real Music Collection
 *
 * Quick and easy helper class for generating test data without hardcoding.
 * Uses your actual music as inspiration for realistic test data.
 *
 * Your Music Collection:
 * 🎧 Dj Aka-m e Dj Malvado Feat Dody - Mussulo (Afro House/Electronic)
 * 🎤 GIMS - Ciel & NINAO (French Rap/Hip-Hop)
 * 🎶 Keblack - Mood & Melrose Place (French R&B)
 *
 * @author MiniSpotify Team
 * @version 1.0 - Simple & Clean
 */
public class TestData {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    // Your actual music genres for realistic test data
    private static final String[] YOUR_GENRES = {"Electronic", "Hip-Hop", "R&B", "Afro House", "French Rap"};

    // Artist styles inspired by your collection
    private static final String[] ARTIST_STYLES = {"Dj", "Solo", "Featuring", "Collective", "Duo"};

    /**
     * 🎵 Creates a single song with realistic data
     * Generates unique songs every time, no hardcoding
     */
    public static Song song() {
        int id = COUNTER.getAndIncrement();
        return new Song(
                generateSongTitle(id),      // "Beat 1", "Vibe 2", etc.
                generateArtistName(id),     // "Artist 1", "Dj Artist 2", etc.
                "Album " + id,              // "Album 1", "Album 2", etc.
                getGenreByIndex(id),        // Cycles through your genres
                generateDuration(id)        // Realistic duration: 180-240 seconds
        );
    }

    /**
     * 🎵 Creates multiple songs for testing collections
     * Perfect for playlist tests, performance tests, etc.
     */
    public static List<Song> songs(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> song())
                .toList();
    }

    /**
     * 🎵 Creates a song with specific genre (based on your music)
     */
    public static Song songWithGenre(String preferredGenre) {
        int id = COUNTER.getAndIncrement();

        // Use your actual genres if available, otherwise use preferred
        String genre = java.util.Arrays.asList(YOUR_GENRES).contains(preferredGenre)
                ? preferredGenre
                : YOUR_GENRES[0]; // Default to Electronic

        return new Song(
                generateSongTitle(id),
                generateArtistName(id),
                "Album " + id,
                genre,
                generateDuration(id)
        );
    }

    /**
     * 👤 Creates a user with realistic music fan username
     */
    public static User user() {
        return user("free");
    }

    /**
     * 👤 Creates a user with specified account type
     */
    public static User user(String accountType) {
        String username = generateMusicFanUsername();
        String hash = SecurePasswordHasher.hashPassword("password123");

        return "premium".equalsIgnoreCase(accountType)
                ? new PremiumUser(username, hash)
                : new FreeUser(username, hash);
    }

    /**
     * 📝 Creates a playlist with realistic name
     */
    public static Playlist playlist() {
        return new Playlist(generatePlaylistName());
    }

    /**
     * 🎵 Creates a diverse playlist with songs from different genres in your collection
     * CORRECTED VERSION - No collection stream issues
     */
    public static List<Song> diverseSongs(int count) {
        List<Song> songs = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Cycle through your music genres for diversity
            String genre = YOUR_GENRES[i % YOUR_GENRES.length];
            songs.add(songWithGenre(genre));
        }
        return songs;
    }

    /**
     * 📝 Creates a playlist with diverse songs - SIMPLE VERSION
     */
    public static Playlist diversePlaylist(int songCount) {
        Playlist playlist = playlist();
        List<Song> songs = diverseSongs(songCount);
        songs.forEach(playlist::addSong);
        return playlist;
    }

    // ===============================
    // 🎲 PRIVATE HELPER METHODS
    // ===============================

    /**
     * Generates song titles inspired by your music style
     */
    private static String generateSongTitle(int id) {
        String[] titleWords = {"Beat", "Vibe", "Flow", "Rhythm", "Pulse", "Wave", "Sound", "Track"};
        return titleWords[(id - 1) % titleWords.length] + " " + id;
    }

    /**
     * Generates artist names in the style of your collection
     */
    private static String generateArtistName(int id) {
        String style = ARTIST_STYLES[(id - 1) % ARTIST_STYLES.length];

        return switch (style) {
            case "Dj" -> "Dj Artist " + id;
            case "Featuring" -> "Artist " + id + " Ft. Guest" + (id + 1);
            case "Collective" -> "The Artist " + id + " Collective";
            case "Duo" -> "Artist " + id + " & Partner";
            default -> "Artist " + id;
        };
    }

    /**
     * Cycles through your actual music genres
     */
    private static String getGenreByIndex(int index) {
        return YOUR_GENRES[(index - 1) % YOUR_GENRES.length];
    }

    /**
     * Generates realistic song duration (3-4 minutes like your songs)
     */
    private static int generateDuration(int id) {
        // Your songs are typically 180-240 seconds
        return 180 + ((id * 7) % 60); // Varies between 180-240 seconds
    }

    /**
     * Generates music fan usernames
     */
    private static String generateMusicFanUsername() {
        String[] prefixes = {"Music", "Beat", "Sound", "Vibe", "Rhythm"};
        String[] suffixes = {"Fan", "Lover", "Head", "Addict", "Soul"};
        int id = COUNTER.get();

        return prefixes[(id - 1) % prefixes.length] +
                suffixes[(id - 1) % suffixes.length] +
                id;
    }

    /**
     * Generates realistic playlist names
     */
    private static String generatePlaylistName() {
        String[] adjectives = {"Best", "Top", "Ultimate", "Essential", "Perfect", "Amazing"};
        String[] musicTypes = {"Hits", "Vibes", "Beats", "Mix", "Collection", "Playlist"};
        int id = COUNTER.getAndIncrement();

        return adjectives[(id - 1) % adjectives.length] + " " +
                musicTypes[(id - 1) % musicTypes.length] + " " +
                id;
    }
}