package playback;

import server.music.DoublyLinkedPlaylist;

public class SequentialPlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;
        playlist.next();
        // No console output
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;
        playlist.previous();
        // No console output
    }

    @Override
    public String getName() {
        return "Sequential";
    }
}