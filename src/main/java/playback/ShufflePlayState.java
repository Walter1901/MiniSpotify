package playback;

import server.music.DoublyLinkedPlaylist;

public class ShufflePlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;
        playlist.shuffle();
        // No console output
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // In shuffle mode, previous is also random
        next(service, playlist);
    }

    @Override
    public String getName() {
        return "Shuffle";
    }
}