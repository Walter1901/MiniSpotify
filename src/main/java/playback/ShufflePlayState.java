package playback;

import server.music.DoublyLinkedPlaylist;

public class ShufflePlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        playlist.shuffle();
        System.out.println("🔀 Random song: " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "No song"));
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // In shuffle mode, previous does the same as next - random choice
        next(service, playlist);
    }

    @Override
    public String getName() {
        return "Shuffle";
    }
}