package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;

public class PlayingState implements PlaybackState {

    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("La musique est déjà en lecture.");
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏸️ Pause en cours...");
        service.setState(new PausedState());
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // Délégue au mode de lecture actuel
        service.getCurrentPlayMode().next(service, playlist);
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // Délégue au mode de lecture actuel
        service.getCurrentPlayMode().previous(service, playlist);
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏹️ Arrêt de la lecture...");
        service.setState(new StoppedState());
    }

    @Override
    public String getName() {
        return "Playing";
    }
}
