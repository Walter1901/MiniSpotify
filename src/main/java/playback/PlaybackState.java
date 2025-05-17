package playback;

import server.music.DoublyLinkedPlaylist;

public interface PlaybackState {
    void play(DoublyLinkedPlaylist playlist);
    void pause(PlaybackService service, DoublyLinkedPlaylist playlist);
    void next(DoublyLinkedPlaylist playlist);
    void previous(DoublyLinkedPlaylist playlist);
    void stop(PlaybackService service, DoublyLinkedPlaylist playlist);
}
