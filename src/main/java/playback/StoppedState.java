package playback;

import server.music.DoublyLinkedPlaylist;

public class StoppedState implements PlaybackState {
    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) {
            System.out.println("📂 Impossible de démarrer la lecture : playlist vide ou non définie.");
            return;
        }

        System.out.println("▶️ Démarrage de la lecture : " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "Aucune chanson"));
        service.setState(new PlayingState());
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⚠️ Impossible de mettre en pause, la musique est arrêtée.");
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // Allow moving to next song even when stopped
        if (playlist != null && !playlist.isEmpty()) {
            System.out.println("Moving to next song while stopped.");
            service.getCurrentPlayMode().next(service, playlist);
        } else {
            System.out.println("⚠️ Cannot move to next song: playlist is empty or not defined.");
        }
    }
    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⚠️ Impossible de revenir à la chanson précédente, la musique est arrêtée.");
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⚠️ La musique est déjà arrêtée.");
    }

    @Override
    public String getName() {
        return "Stopped";
    }
}