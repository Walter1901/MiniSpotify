package playback;

import server.music.DoublyLinkedPlaylist;

/**
 * Implementation for shuffle (random) playback mode.
 * <p>
 * This processor randomizes the order of songs in the playlist.
 * </p>
 */
public class ShuffleProcessor extends PlaylistProcessor {

    /**
     * Shuffles the songs in the playlist to create a random order.
     *
     * @param playlist The playlist to shuffle
     */
    @Override
    protected void sortSongs(DoublyLinkedPlaylist playlist) {
        System.out.println("🔀 Shuffle mode: randomizing playlist");
        playlist.shuffle();
    }

    /**
     * No filters applied in shuffle mode.
     *
     * @param playlist The playlist to process
     */
    @Override
    protected void applyFilters(DoublyLinkedPlaylist playlist) {
        // No filters in shuffle mode
    }
}