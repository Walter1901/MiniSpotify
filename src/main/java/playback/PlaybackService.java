package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;
import users.User;

import java.io.File;

/**
 * Service for managing audio playback with different modes and states.
 * Implements State and Strategy patterns.
 */
public class PlaybackService {
    private PlaybackState currentState;
    private PlaybackMode currentPlayMode;
    private DoublyLinkedPlaylist playlist;
    private AudioPlayer audioPlayer;

    public PlaybackService(DoublyLinkedPlaylist playlist, User user) {
        this.playlist = playlist;
        this.currentState = new StoppedState();
        this.audioPlayer = new AudioPlayer();
        this.currentPlayMode = new SequentialPlayState();
    }

    public void setPlaybackMode(PlaybackMode mode) {
        this.currentPlayMode = mode;
    }

    public void play() {
        if (playlist == null || playlist.isEmpty()) {
            System.out.println("Cannot play: playlist is empty or not loaded");
            return;
        }

        Song currentSong = playlist.getCurrentSong();
        if (currentSong != null) {
            String filePath = currentSong.getFilePath();

            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    audioPlayer.play(filePath);
                    System.out.println("🎵 Now playing: " + currentSong.getTitle() + " by " + currentSong.getArtist());
                } else {
                    System.out.println("⚠️ File not found: " + filePath);
                }
            } else {
                System.out.println("⚠️ Missing audio file for song: " + currentSong.getTitle());
            }

            currentState.play(this, playlist);
        }
    }

    public void pause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
        }
        currentState.pause(this, playlist);
    }

    public void stop() {
        audioPlayer.stop();
        currentState.stop(this, playlist);
    }

    public void next() {
        audioPlayer.stop();
        currentState.next(this, playlist);
        play();
    }

    public void previous() {
        audioPlayer.stop();
        currentState.previous(this, playlist);
        play();
    }

    public void setState(PlaybackState state) {
        this.currentState = state;
    }


    public PlaybackState getCurrentState() {
        return currentState;
    }

    public PlaybackMode getCurrentPlayMode() {
        return currentPlayMode;
    }

    public Song getCurrentSong() {
        return playlist != null ? playlist.getCurrentSong() : null;
    }
}