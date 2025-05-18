package playback;

import server.music.Song;

/**
 * Interface Observer pour les notifications de changement d'état de lecture
 */
public interface PlaybackObserver {
    /**
     * Appelé lorsque l'état de lecture change
     */
    void onPlayStateChanged(String stateName);

    /**
     * Appelé lorsque la chanson en cours change
     */
    void onSongChanged(Song song);
}