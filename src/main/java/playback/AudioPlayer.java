package playback;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;

/**
 * Audio playback manager using JavaFX MediaPlayer
 * Supports REAL pause/resume functionality
 */
public class AudioPlayer {
    private MediaPlayer mediaPlayer;
    private String currentFile;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isInitialized = false;

    /**
     * Initialize JavaFX toolkit (call once at startup)
     */
    public AudioPlayer() {
        initializeJavaFX();
    }

    /**
     * Initialize JavaFX environment
     */
    private void initializeJavaFX() {
        if (!isInitialized) {
            // Initialize JavaFX toolkit
            new JFXPanel(); // This initializes JavaFX toolkit
            isInitialized = true;
            System.out.println("🎵 JavaFX Audio Player initialized");
        }
    }

    /**
     * Plays an audio file
     * @param filePath path to the file
     */
    public void play(String filePath) {
        Platform.runLater(() -> {
            try {
                // Stop current playback if necessary
                stop();

                // Validate file path
                if (filePath == null || filePath.contains("|")) {
                    System.out.println("⚠️ Invalid file path: " + filePath);
                    return;
                }

                // Check if file exists
                File file = new File(filePath);
                if (!file.exists()) {
                    System.out.println("⚠️ File not found: " + filePath);
                    System.out.println("⚠️ Absolute path: " + file.getAbsolutePath());
                    return;
                }

                // Create Media and MediaPlayer
                String mediaUrl = file.toURI().toString();
                Media media = new Media(mediaUrl);
                mediaPlayer = new MediaPlayer(media);

                // Set up event handlers
                setupMediaPlayerListeners();

                // Store current file and play
                currentFile = filePath;
                mediaPlayer.play();
                isPlaying = true;
                isPaused = false;

                System.out.println("🎵 Playing: " + file.getName());

            } catch (Exception e) {
                System.out.println("⚠️ Error playing file: " + e.getMessage());
                e.printStackTrace();
                isPlaying = false;
                isPaused = false;
            }
        });
    }

    /**
     * Set up MediaPlayer event listeners
     */
    private void setupMediaPlayerListeners() {
        // On ready
        mediaPlayer.setOnReady(() -> {
            System.out.println("🎵 Media ready - Duration: " +
                    formatDuration(mediaPlayer.getTotalDuration()));
        });

        // On end of media
        mediaPlayer.setOnEndOfMedia(() -> {
            System.out.println("🎵 Playback completed");
            isPlaying = false;
            isPaused = false;
        });

        // On error
        mediaPlayer.setOnError(() -> {
            System.out.println("⚠️ Media error: " + mediaPlayer.getError().getMessage());
            isPlaying = false;
            isPaused = false;
        });

        // On status change
        mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            System.out.println("🎵 Status changed: " + oldStatus + " -> " + newStatus);
        });
    }

    /**
     * Pauses playback (REAL pause - maintains position)
     */
    public void pause() {
        if (mediaPlayer != null && isPlaying && !isPaused) {
            Platform.runLater(() -> {
                try {
                    Duration currentTime = mediaPlayer.getCurrentTime();
                    mediaPlayer.pause();
                    isPaused = true;
                    isPlaying = false;

                    System.out.println("⏸️ Paused at: " + formatDuration(currentTime));
                } catch (Exception e) {
                    System.out.println("⚠️ Error during pause: " + e.getMessage());
                }
            });
        } else if (isPaused) {
            System.out.println("⏸️ Already paused");
        } else {
            System.out.println("⚠️ No music playing to pause");
        }
    }

    /**
     * Resumes playback from pause point (REAL resume)
     */
    public void resume() {
        if (mediaPlayer != null && isPaused) {
            Platform.runLater(() -> {
                try {
                    Duration currentTime = mediaPlayer.getCurrentTime();
                    mediaPlayer.play();
                    isPaused = false;
                    isPlaying = true;

                    System.out.println("▶️ Resumed from: " + formatDuration(currentTime));
                } catch (Exception e) {
                    System.out.println("⚠️ Error during resume: " + e.getMessage());
                }
            });
        } else if (!isPaused) {
            System.out.println("⚠️ Music is not paused");
        } else {
            System.out.println("⚠️ No media to resume");
        }
    }

    /**
     * Stops playback completely
     */
    public void stop() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                    isPlaying = false;
                    isPaused = false;

                    System.out.println("⏹️ Playback stopped");
                } catch (Exception e) {
                    System.out.println("⚠️ Error during stop: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Set volume (0.0 to 1.0)
     */
    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            Platform.runLater(() -> {
                double clampedVolume = Math.max(0.0, Math.min(1.0, volume));
                mediaPlayer.setVolume(clampedVolume);
                System.out.println("🔊 Volume set to: " + (int)(clampedVolume * 100) + "%");
            });
        }
    }

    /**
     * Get current volume
     */
    public double getVolume() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVolume();
        }
        return 1.0;
    }

    /**
     * Get current playback time
     */
    public Duration getCurrentTime() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentTime();
        }
        return Duration.ZERO;
    }

    /**
     * Get total duration
     */
    public Duration getTotalDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getTotalDuration();
        }
        return Duration.ZERO;
    }

    /**
     * Seek to specific time
     */
    public void seek(Duration time) {
        if (mediaPlayer != null) {
            Platform.runLater(() -> {
                mediaPlayer.seek(time);
                System.out.println("⏭️ Seeked to: " + formatDuration(time));
            });
        }
    }

    /**
     * Get progress as percentage (0.0 to 1.0)
     */
    public double getProgress() {
        if (mediaPlayer != null) {
            Duration current = getCurrentTime();
            Duration total = getTotalDuration();
            if (total.toMillis() > 0) {
                return current.toMillis() / total.toMillis();
            }
        }
        return 0.0;
    }

    /**
     * Format duration for display
     */
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }

        int totalSeconds = (int) duration.toSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Get formatted current time for display
     */
    public String getFormattedCurrentTime() {
        return formatDuration(getCurrentTime());
    }

    /**
     * Get formatted total duration for display
     */
    public String getFormattedTotalDuration() {
        return formatDuration(getTotalDuration());
    }

    /**
     * Get playback status string
     */
    public String getStatusString() {
        if (isPaused) return "⏸️ Paused";
        if (isPlaying) return "▶️ Playing";
        return "⏹️ Stopped";
    }

    // Getters
    public boolean isPlaying() { return isPlaying; }
    public boolean isPaused() { return isPaused; }
    public String getCurrentFile() { return currentFile; }
}