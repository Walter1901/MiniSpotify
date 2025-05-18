package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;

/**
 * Implémentation pour trier par titre et jouer séquentiellement
 */
public class SequentialProcessor extends PlaylistProcessor {

    @Override
    protected void sortSongs(DoublyLinkedPlaylist playlist) {
        // Pas de tri, garde l'ordre original
        System.out.println("⏭️ Mode séquentiel : lecture dans l'ordre de la playlist");
    }

    @Override
    protected void applyFilters(DoublyLinkedPlaylist playlist) {
        // Pas de filtre en mode séquentiel
    }
}