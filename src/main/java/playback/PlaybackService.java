package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;
import users.User;

import java.io.File;

/**
 * Service for managing audio playback with different modes and states.
 * Implements State and Strategy patterns.
 * Modified to work with stateless AudioPlayer - state is managed by PlayerUI
 */
public class PlaybackService {
    private PlaybackState currentState;
    private PlaybackMode currentPlayMode;
    private DoublyLinkedPlaylist playlist;
    private AudioPlayer audioPlayer;

    /**
     * Constructor
     * @param playlist Playlist to manage
     * @param user User context (can be null)
     */
    public PlaybackService(DoublyLinkedPlaylist playlist, User user) {
        this.playlist = playlist;
        this.currentState = new StoppedState();
        this.audioPlayer = new AudioPlayer();
        this.currentPlayMode = new SequentialPlayState();
    }

    /**
     * Set playback mode strategy
     * @param mode Playback mode to use
     */
    public void setPlaybackMode(PlaybackMode mode) {
        this.currentPlayMode = mode;
    }

    /**
     * Play current song
     * State management is handled by PlayerUI, not here
     */
    public void play() {
        if (playlist == null || playlist.isEmpty()) {
            return;
        }

        Song currentSong = playlist.getCurrentSong();
        if (currentSong != null) {
            String filePath = currentSong.getFilePath();

            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    // Just start playback - no state management here
                    audioPlayer.play(filePath);
                }
            }

            // Update internal state (for state pattern)
            currentState.play(this, playlist);
        }
    }

    /**
     * Pause playback
     * Just delegates to AudioPlayer - no state checking
     */
    public void pause() {
        audioPlayer.pause();
        currentState.pause(this, playlist);
    }

    /**
     * Stop playback
     */
    public void stop() {
        audioPlayer.stop();
        currentState.stop(this, playlist);
    }

    /**
     * Move to next song
     */
    public void next() {
        audioPlayer.stop();
        currentState.next(this, playlist);
    }

    /**
     * Move to previous song
     */
    public void previous() {
        audioPlayer.stop();
        currentState.previous(this, playlist);
    }

    /**
     * Set internal state (for state pattern)
     * @param state New state
     */
    public void setState(PlaybackState state) {
        this.currentState = state;
    }

    /**
     * Get current state
     * @return Current playback state
     */
    public PlaybackState getCurrentState() {
        return currentState;
    }

    /**
     * Get current playback mode
     * @return Current playback mode
     */
    public PlaybackMode getCurrentPlayMode() {
        return currentPlayMode;
    }

    /**
     * Get current song from playlist
     * @return Current song or null if no playlist
     */
    public Song getCurrentSong() {
        return playlist != null ? playlist.getCurrentSong() : null;
    }
}