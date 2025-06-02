package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;

public class RepeatPlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        Song currentSong = playlist.getCurrentSong();
        System.out.println("🔁 Repeating: " +
                (currentSong != null ? currentSong.getTitle() : "No song"));
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // In repeat mode, previous does the same as next
        next(service, playlist);
    }

    @Override
    public String getName() {
        return "Repeat";
    }
}