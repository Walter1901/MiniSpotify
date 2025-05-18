package server.music;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bibliothèque musicale centralisée
 * Implémentation du pattern Singleton
 */
public class MusicLibrary {
    // Instance unique (volatile pour visibilité entre threads)
    private static volatile MusicLibrary instance = null;

    // Liste des chansons disponibles
    private List<Song> songs = new ArrayList<>();

    /**
     * Constructeur privé (Singleton)
     */
    private MusicLibrary() {}

    /**
     * Getter pour l'instance Singleton avec Double-Checked Locking
     * pour assurer la thread-safety
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
     * Initialise la bibliothèque avec des chansons d'exemple
     */
    public void initializeWithSampleSongs() {
        // Ajouter des chansons d'exemple à la bibliothèque
        addSong(new Song("Mussulo", "Dj Aka-m e Dj Malvado Feat Dody", "Afro House", "Electronic", 416));
        addSong(new Song("Ciel", "GIMS", "Rap", "Hip-Hop", 306));
        addSong(new Song("NINAO", "GIMS", "Rap", "Hip-Hop", 247));
        addSong(new Song("Mood", "Keblack", "Rap", "Hip-Hop", 253));
        addSong(new Song("Melrose Place", "Keblack Ft. Guy2Bezbar", "Rap", "Hip-Hop", 234));

        System.out.println("Sample songs added to library: " + getAllSongs().size() + " songs");
    }

    /**
     * Ajoute une chanson à la bibliothèque
     */
    public void addSong(Song song) {
        if (song != null && !songs.contains(song)) {
            songs.add(song);
        }
    }

    /**
     * Recherche des chansons par titre (insensible à la casse)
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
     * Filtre les chansons par genre
     */
    public List<Song> filterByGenre(String genre) {
        if (genre == null || genre.isEmpty()) {
            return new ArrayList<>();
        }

        return songs.stream()
                .filter(s -> s.getGenre().equalsIgnoreCase(genre))
                .collect(Collectors.toList());
    }

    /**
     * Filtre les chansons par artiste
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
     * Récupère toutes les chansons disponibles
     */
    public List<Song> getAllSongs() {
        // Retourne une copie défensive pour éviter les modifications externes
        return new ArrayList<>(songs);
    }

    /**
     * Récupère une chanson par son titre exact
     */
    public Song getSongByExactTitle(String title) {
        if (title == null || title.isEmpty()) {
            return null;
        }

        return songs.stream()
                .filter(s -> s.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
    }

    /**
     * Vide la bibliothèque (utile pour les tests)
     */
    public void clear() {
        songs.clear();
    }
}