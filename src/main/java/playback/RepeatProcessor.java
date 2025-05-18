package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Implémentation pour mode répétition
 */
public class RepeatProcessor extends PlaylistProcessor {

    @Override
    protected void sortSongs(DoublyLinkedPlaylist playlist) {
        // Pas de tri en mode répétition
        System.out.println("🔁 Mode répétition : lecture en boucle de la chanson courante");
    }

    @Override
    protected void applyFilters(DoublyLinkedPlaylist playlist) {
        // Pas de filtre en mode répétition
    }
}