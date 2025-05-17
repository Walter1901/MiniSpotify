package server.music;

public class Song {
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int duration; // in seconds

    public Song(String title, String artist, String album, String genre, int duration) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.duration = duration;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public int getDuration() { return duration; }

    @Override
    public String toString() {
        return title + " by " + artist;
    }
}
