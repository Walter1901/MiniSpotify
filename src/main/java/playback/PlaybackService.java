package playback;

import server.music.DoublyLinkedPlaylist;
import users.User;

public class PlaybackService {
    private PlaybackState currentState;
    private DoublyLinkedPlaylist playlist;
    private User user;

    public PlaybackService(DoublyLinkedPlaylist playlist, User user) {
        this.playlist = playlist;
        this.user = user;
        this.currentState = new SequentialPlayState(); // état par défaut
    }

    public void setPlaybackState(PlaybackState state) {
        if (state instanceof ShufflePlayState && !user.canUseShuffle()) {
            System.out.println("❌ Shuffle interdit pour les utilisateurs Free.");
            return;
        }
        this.currentState = state;
    }

    public void play() {
        if (playlist.isEmpty()) {
            System.out.println("📂 Playlist vide.");
            return;
        }
        currentState.play(playlist);
    }
    public void pause() {
        currentState.pause(this, playlist);
    }

    public void next() {
        currentState.next(playlist);
    }

    public void previous() {
        currentState.previous(playlist);
    }

    public void stop() {
        currentState.stop(this, playlist);
    }

    public void setState(PlaybackState state) {
        this.currentState = state;
    }

    public void setPlaylist(DoublyLinkedPlaylist playlist) {
        this.playlist = playlist;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PlaybackState getCurrentState() {
        return currentState;
    }
}
