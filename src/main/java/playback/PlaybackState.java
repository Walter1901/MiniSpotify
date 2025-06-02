package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * State interface for playback states.
 * Implements State pattern to allow PlaybackService to change behavior
 * when its internal state changes.
 *
 * Follows Interface Segregation Principle (ISP) by providing
 * only methods relevant to playback state management.
 */
public interface PlaybackState {
    /**
     * Start or resume playback
     * @param service PlaybackService context
     * @param playlist Current playlist
     */
    void play(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Pause playback
     * @param service PlaybackService context
     * @param playlist Current playlist
     */
    void pause(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Move to next song according to current playback mode
     * @param service PlaybackService context
     * @param playlist Current playlist
     */
    void next(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Move to previous song according to current playback mode
     * @param service PlaybackService context
     * @param playlist Current playlist
     */
    void previous(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Stop playback completely
     * @param service PlaybackService context
     * @param playlist Current playlist
     */
    void stop(PlaybackService service, DoublyLinkedPlaylist playlist);

    /**
     * Get the name of this state for display purposes
     * @return State name as string
     */
    String getName();
}