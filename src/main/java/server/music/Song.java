package server.music;

import java.io.Serializable;

public class Song implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String artist;
    private String album;
    private String genre;
    private int duration; // in seconds
    private String filePath; // Important pour la lecture

    public Song(String title, String artist, String album, String genre, int duration) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.duration = duration;
    }

    // Constructeur avec filePath
    public Song(String title, String artist, String album, String genre, int duration, String filePath) {
        this(title, artist, album, genre, duration);
        this.filePath = filePath;
    }

    // Getters et setters
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public int getDuration() { return duration; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public String toString() {
        return title + " by " + artist + (duration > 0 ? " (" + getFormattedDuration() + ")" : "");
    }

    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}