package playback;

public interface PlayerState {
    void play(PlaybackService service);
    void pause(PlaybackService service);
    void stop(PlaybackService service);
}
