package playback;

import server.music.DoublyLinkedPlaylist;

public class ShufflePlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        playlist.shuffle();
        System.out.println("🔀 Chanson aléatoire : " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "Aucune chanson"));
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // En mode shuffle, previous fait la même chose que next - choix aléatoire
        next(service, playlist);
    }

    @Override
    public String getName() {
        return "Shuffle";
    }
}