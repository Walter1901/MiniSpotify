package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Interface pour les différents modes de lecture
 */
public interface PlaybackMode {
    /**
     * Passe à la chanson suivante selon le mode
     */
    void next(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Passe à la chanson précédente selon le mode
     */
    void previous(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Récupère le nom du mode pour affichage
     */
    String getName();
}