package playback;

import server.music.DoublyLinkedPlaylist;

public class SequentialPlayState implements PlaybackState {

    @Override
    public void play(DoublyLinkedPlaylist playlist) {
        System.out.println("▶ Lecture séquentielle : " + playlist.getCurrentSong());
    }

    @Override
    public void next(DoublyLinkedPlaylist playlist) {
        playlist.next();
        play(playlist);
    }

    @Override
    public void previous(DoublyLinkedPlaylist playlist) {
        playlist.previous();
        play(playlist);
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏸️ Pause séquentielle.");
        service.setState(new PausedState());
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏹️ Arrêt de la lecture séquentielle.");
        service.setState(new StoppedState());
    }
}