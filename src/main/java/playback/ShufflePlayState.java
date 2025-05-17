package playback;

import server.music.DoublyLinkedPlaylist;

public class ShufflePlayState implements PlaybackState {

    @Override
    public void play(DoublyLinkedPlaylist playlist) {
        System.out.println("🔀 Lecture en mode aléatoire : " + playlist.getCurrentSong());
    }

    @Override
    public void next(DoublyLinkedPlaylist playlist) {
        playlist.shuffle();
        play(playlist);
    }

    @Override
    public void previous(DoublyLinkedPlaylist playlist) {
        playlist.shuffle();
        play(playlist);
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏸️ Pause en mode aléatoire.");
        service.setState(new PausedState());
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏹️ Arrêt du mode aléatoire.");
        service.setState(new StoppedState());
    }
}