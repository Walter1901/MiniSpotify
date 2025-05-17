package playback;

import server.music.DoublyLinkedPlaylist;

public class RepeatPlayState implements PlaybackState {

    @Override
    public void play(DoublyLinkedPlaylist playlist) {
        System.out.println("🔁 Répétition : " + playlist.getCurrentSong());
    }

    @Override
    public void next(DoublyLinkedPlaylist playlist) {
        play(playlist);
    }

    @Override
    public void previous(DoublyLinkedPlaylist playlist) {
        play(playlist);
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏸️ Pause in repeat mode.");
        service.setState(new PausedState());
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏹️ Stop repeat mode.");
        service.setState(new StoppedState());
    }
}