package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;

/**
 * Abstract class implementing the Template Method Pattern for playlist processing.
 * <p>
 * This class defines the skeleton of the playlist processing algorithm, deferring
 * some steps to subclasses. The template method ensures that the algorithm's structure
 * stays unchanged, while subclasses can redefine certain steps.
 * </p>
 */
public abstract class PlaylistProcessor {

    /**
     * Template method that defines the skeleton of the playlist processing algorithm.
     * This method calls other methods in a specific sequence, some of which are
     * meant to be overridden by subclasses.
     *
     * @param playlist The playlist to process
     */
    public final void processPlaylist(DoublyLinkedPlaylist playlist) {
        if (validatePlaylist(playlist)) {
            prepareSongs(playlist);
            processCurrentSong(playlist);
            sortSongs(playlist);      // Can be overridden by subclasses
            applyFilters(playlist);   // Can be overridden by subclasses
            finalizePlaylist(playlist);
        }
    }

    /**
     * Validates that the playlist is usable.
     *
     * @param playlist The playlist to validate
     * @return true if the playlist is valid, false otherwise
     */
    protected boolean validatePlaylist(DoublyLinkedPlaylist playlist) {
        if (playlist == null) {
            System.out.println("❌ Error: null playlist.");
            return false;
        }

        if (playlist.isEmpty()) {
            System.out.println("📂 Empty playlist.");
            return false;
        }

        return true;
    }

    /**
     * Prepares the songs in the playlist.
     *
     * @param playlist The playlist to prepare
     */
    protected void prepareSongs(DoublyLinkedPlaylist playlist) {
        System.out.println("🔄 Preparing playlist...");
    }

    /**
     * Processes the current song in the playlist.
     *
     * @param playlist The playlist containing the current song
     */
    protected void processCurrentSong(DoublyLinkedPlaylist playlist) {
        Song currentSong = playlist.getCurrentSong();
        if (currentSong != null) {
            System.out.println("🎵 Current song: " + currentSong.getTitle() + " by " + currentSong.getArtist());
        } else {
            System.out.println("⚠️ No current song in playlist.");
            playlist.reset(); // Reset playlist to the beginning
        }
    }

    /**
     * Sorts the songs in the playlist.
     * This method must be implemented by subclasses.
     *
     * @param playlist The playlist to sort
     */
    protected abstract void sortSongs(DoublyLinkedPlaylist playlist);

    /**
     * Applies filters to the playlist.
     * This method must be implemented by subclasses.
     *
     * @param playlist The playlist to filter
     */
    protected abstract void applyFilters(DoublyLinkedPlaylist playlist);

    /**
     * Finalizes the playlist processing.
     *
     * @param playlist The processed playlist
     */
    protected void finalizePlaylist(DoublyLinkedPlaylist playlist) {
        System.out.println("✅ Playlist ready to play.");
    }
}