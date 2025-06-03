package playback;

import server.music.DoublyLinkedPlaylist;

public class RepeatPlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // In repeat mode, stay on same song
        // No console output
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // In repeat mode, stay on same song
        // No console output
    }

    @Override
    public String getName() {
        return "Repeat";
    }
}