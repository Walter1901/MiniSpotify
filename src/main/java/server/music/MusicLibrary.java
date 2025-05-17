package server.music;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MusicLibrary {
    private static MusicLibrary instance = null;
    private List<Song> songs = new ArrayList<>();

    private MusicLibrary() {}

    /**
     * Initialize the music library with some sample songs
     */
    public void initializeWithSampleSongs() {
        // Add some sample songs to the library
        addSong(new Song("Mussulo", "Dj Aka-m e Dj Malvado Feat Dody", "Afro House", "Electronic", 416));
        addSong(new Song("Ciel", "GIMS", "Rap", "Hip-Hop", 306));
        addSong(new Song("NINAO", "GIMS", "Rap", "Hip-Hop", 247));
        addSong(new Song("Mood", "Keblack", "Rap", "Hip-Hop", 253));
        addSong(new Song("Melrose Place", "Keblack Ft. Guy2Bezbar", "Rap", "Hip-Hop", 234));


        System.out.println("Sample songs added to library: " + getAllSongs().size() + " songs");
    }


    public static MusicLibrary getInstance() {
        if (instance == null) {
            instance = new MusicLibrary();
        }
        return instance;
    }

    public void addSong(Song song) {
        songs.add(song);
    }

    public List<Song> searchByTitle(String title) {
        return songs.stream()
                .filter(s -> s.getTitle().equalsIgnoreCase(title))
                .collect(Collectors.toList());
    }

    public List<Song> filterByGenre(String genre) {
        return songs.stream()
                .filter(s -> s.getGenre().equalsIgnoreCase(genre))
                .collect(Collectors.toList());
    }

    public List<Song> filterByArtist(String artist) {
        return songs.stream()
                .filter(s -> s.getArtist().equalsIgnoreCase(artist))
                .collect(Collectors.toList());
    }

    public List<Song> getAllSongs() {
        return songs;
    }
}