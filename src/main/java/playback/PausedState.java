package playback;

import server.music.DoublyLinkedPlaylist;

public class PausedState implements PlaybackState {
    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("▶️ Reprise de la lecture : " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "Aucune chanson"));
        service.setState(new PlayingState());
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("La musique est déjà en pause.");
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Impossible de passer à la chanson suivante en pause. Reprenez la lecture d'abord.");
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Impossible de revenir à la chanson précédente en pause. Reprenez la lecture d'abord.");
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⏹️ Arrêt de la lecture depuis pause...");
        service.setState(new StoppedState());
    }

    @Override
    public String getName() {
        return "Paused";
    }
}