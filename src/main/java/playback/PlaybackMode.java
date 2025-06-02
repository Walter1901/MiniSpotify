package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Strategy interface for different playback modes.
 * Implements Strategy pattern to define family of algorithms
 * for playlist navigation.
 *
 * Follows Interface Segregation Principle (ISP) by providing
 * only methods relevant to playback mode behavior.
 */
public interface PlaybackMode {
    /**
     * Move to next song according to this mode's strategy
     * @param service PlaybackService context
     * @param playlist Current playlist
     */
    void next(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Move to previous song according to this mode's strategy
     * @param service PlaybackService context
     * @param playlist Current playlist
     */
    void previous(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Get the name of this playback mode for display
     * @return Mode name as string
     */
    String getName();
}