package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Interface unique pour tous les états de lecture
 */
public interface PlaybackState {
    /**
     * Démarre ou reprend la lecture
     */
    void play(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Met en pause la lecture
     */
    void pause(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Passe à la chanson suivante selon le mode de lecture
     */
    void next(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Passe à la chanson précédente selon le mode de lecture
     */
    void previous(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Arrête la lecture
     */
    void stop(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Retourne le nom de l'état pour affichage
     */
    String getName();
}
