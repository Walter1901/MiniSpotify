package playback;

import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;

/**
 * Audio playback manager using JavaZoom JLayer
 */
public class AudioPlayer {
    private Player player;
    private Thread playerThread;
    private String currentFile;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long pausePosition;
    private long songTotalLength;
    private FileInputStream fileInputStream;
    private long startTime; // To track elapsed time for better pause position calculation

    /**
     * Plays an audio file
     * @param filePath path to the file
     */
    public void play(String filePath) {
        try {
            // Stop current playback if necessary
            stop();

            // Check if the path is not a path formatted by error
            if (filePath != null && filePath.contains("|")) {
                System.out.println("⚠️ Incorrect path format detected: " + filePath);
                System.out.println("⚠️ Cannot play file with this format");
                isPlaying = false;
                return;
            }

            // Check if the file exists
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("⚠️ File not found: " + filePath);
                System.out.println("⚠️ Absolute path: " + file.getAbsolutePath());
                isPlaying = false;
                return;
            }

            currentFile = filePath;
            fileInputStream = new FileInputStream(filePath);
            songTotalLength = fileInputStream.available();

            // Create a new player
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            player = new Player(bufferedInputStream);

            // Record start time for better pause position tracking
            startTime = System.currentTimeMillis();

            // Start playback in a separate thread
            playerThread = new Thread(() -> {
                try {
                    System.out.println("🎵 Starting audio playback: " + filePath);
                    isPlaying = true;
                    isPaused = false;
                    player.play();
                    System.out.println("🎵 Playback ended");
                    isPlaying = false;
                } catch (Exception e) {
                    System.out.println("⚠️ Playback error: " + e.getMessage());
                    isPlaying = false;
                }
            });

            playerThread.start();
        } catch (Exception e) {
            System.out.println("⚠️ Player initialization error: " + e.getMessage());
            isPlaying = false;
        }
    }

    /**
     * Pauses playback and returns position
     * @return position in bytes in the file
     */
    public long pause() {
        if (player != null && isPlaying && !isPaused) {
            try {
                // Store current position information before stopping the player
                pausePosition = player.getPosition();

                // Close resources
                player.close();
                if (fileInputStream != null) {
                    fileInputStream.close();
                }

                isPaused = true;
                isPlaying = false;

                if (playerThread != null) {
                    playerThread.interrupt();
                }

                System.out.println("⏸️ Playback paused at " + pausePosition + " ms");
                return pausePosition;
            } catch (Exception e) {
                System.out.println("⚠️ Error during pause: " + e.getMessage());
                return 0;
            }
        }
        return 0;
    }
    /**
     * Resumes playback from pause point
     */
    public void resume() {
        if (isPaused && currentFile != null) {
            try {
                // Create a new player and position it
                fileInputStream = new FileInputStream(currentFile);

                // Skip to pause position if possible
                if (pausePosition > 0) {
                    fileInputStream.skip(pausePosition);
                }

                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                player = new Player(bufferedInputStream);

                // Reset start time for accurate future pause
                startTime = System.currentTimeMillis();

                // Start playback in a separate thread
                playerThread = new Thread(() -> {
                    try {
                        System.out.println("▶️ Resuming playback from " + pausePosition + "/" + songTotalLength);
                        isPlaying = true;
                        isPaused = false;
                        player.play();
                        System.out.println("🎵 Playback ended");
                        isPlaying = false;
                    } catch (Exception e) {
                        System.out.println("⚠️ Error during resume: " + e.getMessage());
                        isPlaying = false;
                    }
                });

                playerThread.start();
            } catch (Exception e) {
                System.out.println("⚠️ Error during resume: " + e.getMessage());
            }
        }
    }

    /**
     * Stops playback
     */
    public void stop() {
        if (player != null) {
            player.close();
            isPlaying = false;
            isPaused = false;

            if (playerThread != null) {
                playerThread.interrupt();
            }

            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error during stop: " + e.getMessage());
            }

            System.out.println("⏹️ Playback stopped");
        }
    }

    /**
     * Checks if playback is in progress
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Checks if playback is paused
     */
    public boolean isPaused() {
        return isPaused;
    }
}