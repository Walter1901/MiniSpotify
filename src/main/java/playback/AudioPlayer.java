package playback;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

/**
 * AudioPlayer
 * Robust JavaFX initialization and file path handling
 */
public class AudioPlayer {
    private MediaPlayer mediaPlayer;
    private static boolean javaFxInitialized = false;
    private static boolean javaFxAvailable = true;

    /**
     * Constructor - Initialize JavaFX with proper error handling
     */
    public AudioPlayer() {
        initializeJavaFX();
    }

    /**
     * Initialize JavaFX environment with robust error handling
     */
    private synchronized void initializeJavaFX() {
        if (javaFxInitialized) {
            return;
        }

        try {
            // Set system properties BEFORE initializing JavaFX
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.allowhidpi", "false");
            System.setProperty("javafx.animation.fullspeed", "true");
            System.setProperty("prism.verbose", "false");
            System.setProperty("java.awt.headless", "false");

            // Force software rendering for better compatibility
            System.setProperty("prism.forceGPU", "false");
            System.setProperty("prism.text", "t2k");

            // Initialize JavaFX Platform
            new JFXPanel(); // This initializes the JavaFX Platform

            // Wait a bit for platform to initialize
            Thread.sleep(100);

            javaFxInitialized = true;
            System.out.println("✅ JavaFX initialized successfully");

        } catch (Exception e) {
            System.err.println("❌ JavaFX initialization failed: " + e.getMessage());
            javaFxAvailable = false;
            javaFxInitialized = true; // Mark as attempted
        }
    }

    /**
     * Play a file with enhanced path resolution for JAR
     */
    public void play(String filePath) {
        if (!javaFxAvailable) {
            System.out.println("🎵 [SIMULATION] Playing: " + extractFileName(filePath));
            return;
        }

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

                // Enhanced file resolution for JAR deployment
                File audioFile = resolveAudioFile(filePath);

                if (audioFile == null || !audioFile.exists()) {
                    System.err.println("❌ Audio file not found: " + filePath);
                    return;
                }

                // Create media and player
                String mediaUrl = audioFile.toURI().toString();
                Media media = new Media(mediaUrl);
                mediaPlayer = new MediaPlayer(media);

                // Setup event handlers
                setupMediaPlayerHandlers(audioFile.getName());

            } catch (Exception e) {
                System.err.println("❌ Error playing audio: " + e.getMessage());
            }
        });
    }

    /**
     * Enhanced file resolution for both development and JAR deployment
     */
    private File resolveAudioFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // Try multiple resolution strategies
        File[] candidates = {
                // 1. Direct path (if absolute and exists)
                new File(filePath),

                // 2. Relative to working directory
                new File("./mp3/" + extractFileName(filePath)),
                new File("mp3/" + extractFileName(filePath)),

                // 3. Relative to JAR location
                new File(getJarDirectory(), "mp3/" + extractFileName(filePath)),

                // 4. In resources (if copied to output)
                new File("./src/main/resources/mp3/" + extractFileName(filePath))
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                System.out.println("🎵 Found audio file: " + candidate.getAbsolutePath());
                return candidate;
            }
        }

        return null;
    }

    /**
     * Get the directory where the JAR is located
     */
    private File getJarDirectory() {
        try {
            String jarPath = AudioPlayer.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            return new File(jarPath).getParentFile();
        } catch (Exception e) {
            return new File(".");
        }
    }

    /**
     * Setup media player event handlers
     */
    private void setupMediaPlayerHandlers(String fileName) {
        mediaPlayer.setOnReady(() -> {
            mediaPlayer.play();
            System.out.println("🎵 Now playing: " + fileName);
        });

        mediaPlayer.setOnError(() -> {
            System.err.println("❌ Media player error: " + mediaPlayer.getError());
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            System.out.println("🔚 Finished playing: " + fileName);
        });
    }

    /**
     * Extract filename from path
     */
    private String extractFileName(String filePath) {
        if (filePath == null) return "unknown";

        // Handle both Unix and Windows path separators
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }

    /**
     * Pause playback
     */
    public void pause() {
        if (!javaFxAvailable) {
            System.out.println("⏸️ [SIMULATION] Paused");
            return;
        }

        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.pause();
                } catch (Exception e) {
                    System.err.println("Error pausing: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Resume playback
     */
    public void resume() {
        if (!javaFxAvailable) {
            System.out.println("▶️ [SIMULATION] Resumed");
            return;
        }

        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.play();
                } catch (Exception e) {
                    System.err.println("Error resuming: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Stop playback
     */
    public void stop() {
        if (!javaFxAvailable) {
            System.out.println("⏹️ [SIMULATION] Stopped");
            return;
        }

        Platform.runLater(() -> {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                }
            } catch (Exception e) {
                System.err.println("Error stopping: " + e.getMessage());
            }
        });
    }

    // Compatibility methods
    public boolean isPlaying() {
        return false; // State managed by PlayerUI
    }

    public boolean isPaused() {
        return false; // State managed by PlayerUI
    }

    public String getCurrentFile() {
        return null;
    }

    public boolean hasMediaPlayer() {
        return mediaPlayer != null && javaFxAvailable;
    }

    public String getStatusString() {
        if (!javaFxAvailable) {
            return "JavaFX not available (using simulation)";
        }
        return hasMediaPlayer() ? "Audio Ready" : "No Audio";
    }
}