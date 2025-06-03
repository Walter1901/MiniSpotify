package playback;

import server.music.DoublyLinkedPlaylist;

public class PausedState implements PlaybackState {
    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.setState(new PlayingState());
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // Silent - already paused
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.getCurrentPlayMode().next(service, playlist);
        service.setState(new PlayingState());
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.getCurrentPlayMode().previous(service, playlist);
        service.setState(new PlayingState());
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.setState(new StoppedState());
    }

    @Override
    public String getName() {
        return "Paused";
    }
}