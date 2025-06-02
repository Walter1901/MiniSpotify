package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;
import users.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing audio playback with different modes and states.
 * <p>
 * This service implements the State pattern for playback control (play, pause, stop)
 * and the Strategy pattern for playback modes (sequential, shuffle, repeat).
 * It also uses the Observer pattern to notify components about playback changes.
 * </p>
 *
 * <h2>Design patterns used:</h2>
 * <ul>
 *   <li><b>State Pattern</b> - Allows the service to change its behavior when its internal state changes</li>
 *   <li><b>Strategy Pattern</b> - Defines a family of algorithms for playlist navigation</li>
 *   <li><b>Observer Pattern</b> - Notifies external components of state changes</li>
 * </ul>
 */
public class PlaybackService {
    /** Current playback state */
    private PlaybackState currentState;

    /** Current playback mode */
    private PlaybackMode currentPlayMode;

    /** Currently loaded playlist */
    private DoublyLinkedPlaylist playlist;

    /** Current user */
    private User user;

    /** Audio player for file playback */
    private AudioPlayer audioPlayer;

    /** List of registered observers */
    private List<PlaybackObserver> observers = new ArrayList<>();

    /**
     * Creates a new playback service with the specified playlist and user.
     *
     * @param playlist Playlist to play
     * @param user Current user
     */
    public PlaybackService(DoublyLinkedPlaylist playlist, User user) {
        this.playlist = playlist;
        this.user = user;
        this.currentState = new StoppedState();
        this.audioPlayer = new AudioPlayer();
        this.currentPlayMode = new SequentialPlayState(); // Default mode
    }

    /**
     * Sets the playback mode to use.
     * Implements the Strategy pattern by allowing dynamic switching
     * of the playlist navigation algorithm.
     *
     * @param mode Playback mode to use (Sequential, Shuffle, Repeat)
     */
    public void setPlaybackMode(PlaybackMode mode) {
        this.currentPlayMode = mode;
        notifyObservers();
    }

    /**
     * Starts or resumes playback.
     * Delegates behavior to the current state (State pattern).
     */
    public void play() {
        if (playlist == null || playlist.isEmpty()) {
            System.out.println("📂 Empty playlist.");
            return;
        }

        Song currentSong = playlist.getCurrentSong();
        if (currentSong != null) {
            // Explicitly check the path
            String filePath = currentSong.getFilePath();
            System.out.println("DEBUG: Trying to play file: " + filePath);

            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    audioPlayer.play(filePath);
                    System.out.println("🎵 Playing: " + currentSong.getTitle() + " by " + currentSong.getArtist());
                } else {
                    System.out.println("⚠️ File not found: " + filePath);
                    System.out.println("⚠️ Absolute path: " + file.getAbsolutePath());
                }
            } else {
                System.out.println("⚠️ Missing audio file for song: " + currentSong.getTitle());
            }

            currentState.play(this, playlist);
        }
    }

    /**
     * Pauses playback.
     * Delegates behavior to the current state (State pattern).
     */
    public void pause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
        }
        currentState.pause(this, playlist);
    }

    /**
     * Stops playback.
     * Delegates behavior to the current state (State pattern).
     */
    public void stop() {
        audioPlayer.stop();
        currentState.stop(this, playlist);
    }

    /**
     * Skips to the next song according to the current playback mode.
     * Delegates to the current state first, then resumes playback.
     */
    public void next() {
        audioPlayer.stop();
        currentState.next(this, playlist);
        play(); // Auto-play after changing song
    }

    /**
     * Returns to the previous song according to the current playback mode.
     * Delegates to the current state first, then resumes playback.
     */
    public void previous() {
        audioPlayer.stop();
        currentState.previous(this, playlist);
        play(); // Auto-play after changing song
    }

    /**
     * Changes the current player state.
     * Key method of the State pattern.
     *
     * @param state New state
     */
    public void setState(PlaybackState state) {
        this.currentState = state;
        notifyObservers();
    }

    /**
     * Sets the playlist to play.
     *
     * @param playlist Playlist to play
     */
    public void setPlaylist(DoublyLinkedPlaylist playlist) {
        this.playlist = playlist;
        notifyObservers();
    }

    /**
     * Sets the current user.
     *
     * @param user Current user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the current state.
     *
     * @return Current state
     */
    public PlaybackState getCurrentState() {
        return currentState;
    }

    /**
     * Gets the current playback mode.
     *
     * @return Current playback mode
     */
    public PlaybackMode getCurrentPlayMode() {
        return currentPlayMode;
    }

    /**
     * Gets the currently playing song.
     *
     * @return Current song or null if no playlist is loaded
     */
    public Song getCurrentSong() {
        return playlist != null ? playlist.getCurrentSong() : null;
    }

    // Observer pattern methods

    /**
     * Registers an observer for notifications.
     *
     * @param observer Observer to register
     */
    public void registerObserver(PlaybackObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Removes an observer.
     *
     * @param observer Observer to remove
     */
    public void removeObserver(PlaybackObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all observers of state changes.
     * Key method of the Observer pattern.
     */
    private void notifyObservers() {
        for (PlaybackObserver observer : observers) {
            observer.onPlayStateChanged(currentState.getName());

            Song song = getCurrentSong();
            if (song != null) {
                observer.onSongChanged(song);
            }
        }
    }
}