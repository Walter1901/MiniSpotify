package playback;

import server.music.DoublyLinkedPlaylist;

public class PlayingState implements PlaybackState {

    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // Silent - already playing
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.setState(new PausedState());
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.getCurrentPlayMode().next(service, playlist);
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.getCurrentPlayMode().previous(service, playlist);
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        service.setState(new StoppedState());
    }

    @Override
    public String getName() {
        return "Playing";
    }
}