package server.music;

import java.io.Serializable;

/**
 * Song entity representing a music track.
 * Contains all metadata required for music library management.
 * Implements Serializable for persistence support.
 */
public class Song implements Serializable {
    private static final long serialVersionUID = 1L;

    // Song metadata
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int duration; // Duration in seconds
    private String filePath; // File system path for playback

    /**
     * Constructor for creating a song with metadata
     * @param title Song title
     * @param artist Artist name
     * @param album Album name
     * @param genre Music genre
     * @param duration Duration in seconds
     */
    public Song(String title, String artist, String album, String genre, int duration) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.duration = duration;
    }

    /**
     * Constructor including file path
     * @param title Song title
     * @param artist Artist name
     * @param album Album name
     * @param genre Music genre
     * @param duration Duration in seconds
     * @param filePath File system path
     */
    public Song(String title, String artist, String album, String genre, int duration, String filePath) {
        this(title, artist, album, genre, duration);
        this.filePath = filePath;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public int getDuration() { return duration; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setAlbum(String album) { this.album = album; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setDuration(int duration) { this.duration = duration; }

    /**
     * Get formatted duration as MM:SS
     * @return Formatted duration string
     */
    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * String representation for display
     * @return Human-readable song information
     */
    @Override
    public String toString() {
        return title + " by " + artist + (duration > 0 ? " (" + getFormattedDuration() + ")" : "");
    }

    /**
     * Equality based on title and artist
     * @param obj Object to compare
     * @return true if songs are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Song song = (Song) obj;
        return title.equals(song.title) && artist.equals(song.artist);
    }

    /**
     * Hash code based on title and artist
     * @return Hash code for the song
     */
    @Override
    public int hashCode() {
        return title.hashCode() + artist.hashCode();
    }
}