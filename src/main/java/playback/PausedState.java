package playback;

import server.music.DoublyLinkedPlaylist;

public class PausedState implements PlaybackState {
    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("▶️ Resuming playback: " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "No song"));
        service.setState(new PlayingState());
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Music is already paused.");
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Cannot skip to next song while paused. Resume playback first.");
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Cannot go to previous song while paused. Resume playback first.");
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏹️ Stopping playback from pause...");
        service.setState(new StoppedState());
    }

    @Override
    public String getName() {
        return "Paused";
    }
}