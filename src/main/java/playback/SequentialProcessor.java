package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Implementation for sequential playback.
 * <p>
 * This processor doesn't modify the playlist order, playing songs
 * in their original sequence.
 * </p>
 */
public class SequentialProcessor extends PlaylistProcessor {

    /**
     * No sorting needed for sequential playback - keeps the original order.
     *
     * @param playlist The playlist to process
     */
    @Override
    protected void sortSongs(DoublyLinkedPlaylist playlist) {
        // No sorting, keep original order
        System.out.println("⏭️ Sequential mode: playing in playlist order");
    }

    /**
     * No filters applied in sequential mode.
     *
     * @param playlist The playlist to process
     */
    @Override
    protected void applyFilters(DoublyLinkedPlaylist playlist) {
        // No filters in sequential mode
    }
}