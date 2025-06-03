package playback;

import server.music.DoublyLinkedPlaylist;

public class StoppedState implements PlaybackState {
    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) {
            return; // Silent failure
        }
        service.setState(new PlayingState());
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // Silent - cannot pause when stopped
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist != null && !playlist.isEmpty()) {
            service.getCurrentPlayMode().next(service, playlist);
        }
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist != null && !playlist.isEmpty()) {
            service.getCurrentPlayMode().previous(service, playlist);
        }
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // Silent - already stopped
    }

    @Override
    public String getName() {
        return "Stopped";
    }
}