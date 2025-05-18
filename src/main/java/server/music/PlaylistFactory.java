package server.music;

import users.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory pour créer différents types de playlists
 * Implémentation du pattern Factory
 */
public class PlaylistFactory {

    /**
     * Type de playlist
     */
    public enum PlaylistType {
        STANDARD,
        COLLABORATIVE,
        SMART
    }

    /**
     * Crée une playlist en fonction du type demandé
     */
    public static Playlist createPlaylist(String name, PlaylistType type, User owner) {
        switch (type) {
            case COLLABORATIVE:
                return new CollaborativePlaylist(name, owner);
            case SMART:
                return new SmartPlaylist(name, owner);
            case STANDARD:
            default:
                return new Playlist(name);
        }
    }

    /**
     * Crée une playlist standard
     */
    public static Playlist createStandardPlaylist(String name) {
        return new Playlist(name);
    }

    /**
     * Crée une playlist collaborative
     */
    public static Playlist createCollaborativePlaylist(String name, User owner) {
        return new CollaborativePlaylist(name, owner);
    }

    /**
     * Crée une playlist intelligente
     */
    public static Playlist createSmartPlaylist(String name, User owner) {
        return new SmartPlaylist(name, owner);
    }

    /**
     * Classes internes pour les différents types de playlists
     */

    /**
     * Playlist collaborative
     */
    private static class CollaborativePlaylist extends Playlist {
        private User owner;
        private List<User> collaborators = new ArrayList<>();

        public CollaborativePlaylist(String name, User owner) {
            super(name);
            this.owner = owner;
        }

        public void addCollaborator(User user) {
            if (user != null && !collaborators.contains(user)) {
                collaborators.add(user);
            }
        }

        public void removeCollaborator(User user) {
            collaborators.remove(user);
        }

        public List<User> getCollaborators() {
            return new ArrayList<>(collaborators);
        }

        public User getOwner() {
            return owner;
        }
    }

    /**
     * Playlist intelligente avec filtres automatiques
     */
    private static class SmartPlaylist extends Playlist {
        private User owner;
        private String filterGenre;
        private String filterArtist;

        public SmartPlaylist(String name, User owner) {
            super(name);
            this.owner = owner;
        }

        public void setFilterGenre(String genre) {
            this.filterGenre = genre;
            updateSongs();
        }

        public void setFilterArtist(String artist) {
            this.filterArtist = artist;
            updateSongs();
        }

        private void updateSongs() {
            // Actualise le contenu de la playlist en fonction des filtres
            // Cette méthode serait appelée à chaque changement de filtre
            // ou à chaque mise à jour de la bibliothèque musicale
        }
    }
}