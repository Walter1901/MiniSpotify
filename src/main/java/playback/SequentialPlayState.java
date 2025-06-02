package playback;

import server.music.DoublyLinkedPlaylist;

public class SequentialPlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        playlist.next();
        System.out.println("⏭️ Next song (sequential mode): " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "No song"));
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        playlist.previous();
        System.out.println("⏮️ Previous song (sequential mode): " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "No song"));
    }

    @Override
    public String getName() {
        return "Sequential";
    }
}