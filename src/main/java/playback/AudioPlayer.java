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
            System.out.println("🎵 Audio Player initialized");
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
                    System.out.println("⚠️ Invalid file path");
                    return;
                }

                File file = new File(filePath);
                if (!file.exists()) {
                    System.out.println("⚠️ File not found: " + file.getName());
                    return;
                }

                // Create and start new player
                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);

                // Simple listeners - NO state updates
                mediaPlayer.setOnReady(() -> {
                    mediaPlayer.play();
                    System.out.println("🎵 Started: " + file.getName());
                });

                mediaPlayer.setOnError(() -> {
                    System.out.println("⚠️ Media error: " + file.getName());
                });

                mediaPlayer.setOnEndOfMedia(() -> {
                    System.out.println("🎵 Ended: " + file.getName());
                });

            } catch (Exception e) {
                System.out.println("⚠️ Play error: " + e.getMessage());
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
                    System.out.println("⏸️ Audio paused");
                } catch (Exception e) {
                    System.out.println("⚠️ Pause error: " + e.getMessage());
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
                    System.out.println("▶️ Audio resumed");
                } catch (Exception e) {
                    System.out.println("⚠️ Resume error: " + e.getMessage());
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
                System.out.println("⏹️ Audio stopped");
            } catch (Exception e) {
                System.out.println("⚠️ Stop error: " + e.getMessage());
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