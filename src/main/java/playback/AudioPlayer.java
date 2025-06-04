package playback;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

/**
 * Stateless AudioPlayer with compatibility methods
 * Provides methods needed by PlaybackService but state is managed by PlayerUI
 */
public class AudioPlayer {
    private MediaPlayer mediaPlayer;
    private boolean isInitialized = false;

    /**
     * Constructor - Initialize JavaFX only
     */
    public AudioPlayer() {
        initializeJavaFX();
    }

    /**
     * Initialize JavaFX environment
     */
    private void initializeJavaFX() {
        if (!isInitialized) {
            new JFXPanel();
            isInitialized = true;
        }
    }

    /**
     * Play a file - NO state tracking, pure function
     * @param filePath Path to audio file
     */
    public void play(String filePath) {
        Platform.runLater(() -> {
            try {
                // Clean up previous player
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer.stop();
                        mediaPlayer.dispose();
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }

                // Validate file
                if (filePath == null || filePath.isEmpty()) {
                    return;
                }

                File file = null;

                if (filePath.startsWith("mp3/")) {
                    file = new File("./" + filePath);  // ./mp3/filename.mp3
                    if (!file.exists()) {
                        file = new File(filePath);     // mp3/filename.mp3
                    }
                }

                if (file == null || !file.exists()) {
                    file = new File(filePath);
                }

                if (!file.exists()) {
                    System.err.println("❌ Audio file not found: " + filePath);
                    return;
                }

                // Create and start new player
                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);

                // Simple listeners - NO state updates
                mediaPlayer.setOnReady(() -> {
                    mediaPlayer.play();
                });

                mediaPlayer.setOnError(() -> {
                    System.err.println("❌ Media player error for: " + filePath);
                });

                mediaPlayer.setOnEndOfMedia(() -> {
                    // Silent end handling
                });

            } catch (Exception e) {
                System.err.println("❌ Error playing audio: " + e.getMessage());
            }
        });
    }

    /**
     * Pause - NO state tracking
     */
    public void pause() {
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.pause();
                } catch (Exception e) {
                    // Silent error handling
                }
            }
        });
    }

    /**
     * Resume - NO state tracking
     */
    public void resume() {
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.play();
                } catch (Exception e) {
                    // Silent error handling
                }
            }
        });
    }

    /**
     * Stop - NO state tracking
     */
    public void stop() {
        Platform.runLater(() -> {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                }
            } catch (Exception e) {
                // Silent error handling
            }
        });
    }

    // ===== COMPATIBILITY METHODS FOR PlaybackService =====

    /**
     * Compatibility method for PlaybackService
     * Always returns false since state is managed by PlayerUI
     * @return false (state managed externally)
     */
    public boolean isPlaying() {
        return false; // State managed by PlayerUI, not AudioPlayer
    }

    /**
     * Compatibility method for PlaybackService
     * Always returns false since state is managed by PlayerUI
     * @return false (state managed externally)
     */
    public boolean isPaused() {
        return false; // State managed by PlayerUI, not AudioPlayer
    }

    /**
     * Compatibility method for PlaybackService
     * @return null (not tracked)
     */
    public String getCurrentFile() {
        return null; // Not tracked by AudioPlayer
    }

    /**
     * Check if MediaPlayer exists (simple availability check)
     * @return true if player exists, false otherwise
     */
    public boolean hasMediaPlayer() {
        return mediaPlayer != null;
    }

    /**
     * Get status string for display (compatibility)
     * @return Simple status
     */
    public String getStatusString() {
        return hasMediaPlayer() ? "Audio Ready" : "No Audio";
    }
}