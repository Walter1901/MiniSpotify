package playback;

import server.music.DoublyLinkedPlaylist;

public class PausedState implements PlaybackState {
    @Override
    public void play(DoublyLinkedPlaylist playlist) {
        System.out.println("Resuming playback...");
        // You may need access to PlaybackService to change state, adjust as needed
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Music is already paused.");
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Stopping playback from pause...");
        service.setState(new StoppedState());
    }

    @Override
    public void next(DoublyLinkedPlaylist playlist) {
        // Implement as needed
    }

    @Override
    public void previous(DoublyLinkedPlaylist playlist) {
        // Implement as needed
    }
}