package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Implémentation pour mode shuffle (aléatoire)
 */
public class ShuffleProcessor extends PlaylistProcessor {

    @Override
    protected void sortSongs(DoublyLinkedPlaylist playlist) {
        System.out.println("🔀 Mode aléatoire : mélange de la playlist");
        playlist.shuffle();
    }

    @Override
    protected void applyFilters(DoublyLinkedPlaylist playlist) {
        // Pas de filtre en mode shuffle
    }
}