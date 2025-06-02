package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Implementation for repeat playback mode.
 * <p>
 * This processor sets up the playlist for repeating the current song.
 * </p>
 */
public class RepeatProcessor extends PlaylistProcessor {

    /**
     * No sorting needed in repeat mode.
     *
     * @param playlist The playlist to process
     */
    @Override
    protected void sortSongs(DoublyLinkedPlaylist playlist) {
        // No sorting in repeat mode
        System.out.println("🔁 Repeat mode: looping current song");
    }

    /**
     * No filters applied in repeat mode.
     *
     * @param playlist The playlist to process
     */
    @Override
    protected void applyFilters(DoublyLinkedPlaylist playlist) {
        // No filters in repeat mode
    }
}