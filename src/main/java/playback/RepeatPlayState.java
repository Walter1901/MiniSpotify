package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;

public class RepeatPlayState implements PlaybackMode {

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) return;

        Song currentSong = playlist.getCurrentSong();
        System.out.println("🔁 Répétition : " +
                (currentSong != null ? currentSong.getTitle() : "Aucune chanson"));
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        // En mode répétition, previous fait la même chose que next
        next(service, playlist);
    }

    @Override
    public String getName() {
        return "Repeat";
    }
}