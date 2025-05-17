package playback;

import server.music.DoublyLinkedPlaylist;

public class StoppedState implements PlaybackState {
    @Override
    public void play(DoublyLinkedPlaylist playlist) {
        System.out.println("Démarrage de la lecture...");
        // You may need to handle state change elsewhere, as you don't have access to PlaybackService here
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("Impossible de mettre en pause, la musique est arrêtée.");
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("La musique est déjà arrêtée.");
    }

    @Override
    public void next(DoublyLinkedPlaylist playlist) {
        // No action needed
    }

    @Override
    public void previous(DoublyLinkedPlaylist playlist) {
        // No action needed
    }
}