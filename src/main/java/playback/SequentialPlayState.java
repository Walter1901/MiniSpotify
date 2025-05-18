package playback;

import server.music.DoublyLinkedPlaylist;

public class SequentialPlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        playlist.next();
        System.out.println("⏭️ Chanson suivante (mode séquentiel) : " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "Aucune chanson"));
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        playlist.previous();
        System.out.println("⏮️ Chanson précédente (mode séquentiel) : " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "Aucune chanson"));
    }

    @Override
    public String getName() {
        return "Sequential";
    }
}