package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;

/**
 * Classe abstraite qui implémente le Template Method Pattern pour le traitement des playlists
 */
public abstract class PlaylistProcessor {

    /**
     * Template method qui définit le squelette du traitement d'une playlist
     */
    public final void processPlaylist(DoublyLinkedPlaylist playlist) {
        if (validatePlaylist(playlist)) {
            prepareSongs(playlist);
            processCurrentSong(playlist);
            sortSongs(playlist);      // Peut être redéfinie par les sous-classes
            applyFilters(playlist);   // Peut être redéfinie par les sous-classes
            finalizePlaylist(playlist);
        }
    }

    /**
     * Valide que la playlist est utilisable
     */
    protected boolean validatePlaylist(DoublyLinkedPlaylist playlist) {
        if (playlist == null) {
            System.out.println("❌ Erreur : playlist null.");
            return false;
        }

        if (playlist.isEmpty()) {
            System.out.println("📂 Playlist vide.");
            return false;
        }

        return true;
    }

    /**
     * Préparation des chansons de la playlist
     */
    protected void prepareSongs(DoublyLinkedPlaylist playlist) {
        System.out.println("🔄 Préparation de la playlist...");
    }

    /**
     * Traitement de la chanson courante
     */
    protected void processCurrentSong(DoublyLinkedPlaylist playlist) {
        Song currentSong = playlist.getCurrentSong();
        if (currentSong != null) {
            System.out.println("🎵 Chanson courante : " + currentSong.getTitle() + " par " + currentSong.getArtist());
        } else {
            System.out.println("⚠️ Aucune chanson actuelle dans la playlist.");
            playlist.reset(); // Réinitialise la playlist au début
        }
    }

    /**
     * Tri des chansons - à implémenter par les sous-classes
     */
    protected abstract void sortSongs(DoublyLinkedPlaylist playlist);

    /**
     * Application de filtres - à implémenter par les sous-classes
     */
    protected abstract void applyFilters(DoublyLinkedPlaylist playlist);

    /**
     * Finalisation du traitement de la playlist
     */
    protected void finalizePlaylist(DoublyLinkedPlaylist playlist) {
        System.out.println("✅ Playlist prête à être jouée.");
    }
}