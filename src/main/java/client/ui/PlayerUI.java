package client.ui;

import client.commands.*;
import playback.AudioPlayer;
import playback.PlaybackService;
import playback.RepeatPlayState;
import playback.SequentialPlayState;
import playback.ShufflePlayState;
import server.music.DoublyLinkedPlaylist;
import server.music.Song;
import client.ui.UserInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * User interface for audio player functionality
 * Handles all player commands and playlist management
 * Uses centralized state management to prevent synchronization issues
 */
public class PlayerUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private CommandInvoker commandInvoker;
    private PlaybackService playbackService;
    private AudioPlayer audioPlayer;

    // Centralized player state variables - SINGLE SOURCE OF TRUTH
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long pausePosition = 0;
    private String currentSongTitle = null;

    /**
     * Constructor - Initialize PlayerUI with required dependencies
     * @param mainUI Reference to main user interface
     * @param in Input stream for server communication
     * @param out Output stream for server communication
     * @param scanner Scanner for user input
     */
    public PlayerUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
        this.commandInvoker = new CommandInvoker();
        this.audioPlayer = new AudioPlayer();
    }

    /**
     * Start the player with playlist selection
     * Main entry point for player functionality
     */
    public void startPlayer() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("\n--- Audio Player ---");
        System.out.println("==================================================================================");

        // Get available playlists from server
        out.println("GET_PLAYLISTS");
        String response;
        List<String> playlists = new ArrayList<>();

        while (!(response = in.readLine()).equals("END")) {
            playlists.add(response);
            System.out.println("📂 " + response);
        }

        // Check if any playlists exist
        if (playlists.isEmpty()) {
            System.out.println("❌ No playlists found. Please create a playlist first.");
            return;
        }

        // Get playlist name from user
        System.out.println("==================================================================================");
        System.out.print("Enter the name of the playlist to play: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine().trim();

        // Request songs from the selected playlist
        out.println("GET_PLAYLIST_SONGS " + playlistName);
        response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("❌ Error loading playlist: " + response);
            return;
        }

        // Create local playlist and load songs
        DoublyLinkedPlaylist playlist = new DoublyLinkedPlaylist();
        loadSongsIntoPlaylist(playlist);

        // Inform server that playlist is loaded
        out.println("LOAD_PLAYLIST " + playlistName);
        response = in.readLine();
        System.out.println("📀 " + response);

        // Initialize playback service
        playbackService = new PlaybackService(playlist, null);

        // Choose playback mode
        choosePlaybackMode();

        // Start the player control loop
        controlPlayerLoop(playlist);
    }

    /**
     * Start player with a pre-loaded shared playlist
     * Used when playing playlists shared by other users
     * @param playlistName Name of the shared playlist
     * @param ownerUsername Username of the playlist owner
     */
    public void startPlayerWithLoadedPlaylist(String playlistName, String ownerUsername) throws IOException {
        System.out.println("==================================================================================");
        System.out.println("\n--- Audio Player ---");
        System.out.println("🎵 Playing shared playlist: " + playlistName + " (by " + ownerUsername + ")");
        System.out.println("==================================================================================");

        // Create local playlist
        DoublyLinkedPlaylist playlist = new DoublyLinkedPlaylist();

        // Get songs from shared playlist
        out.println("GET_SHARED_PLAYLIST_SONGS " + ownerUsername + " " + playlistName);
        String response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("❌ Error loading shared playlist: " + response);
            return;
        }

        // Load songs into playlist
        loadSongsIntoPlaylist(playlist);

        // Initialize playback service
        playbackService = new PlaybackService(playlist, null);

        // Choose playback mode
        choosePlaybackMode();

        // Start the player control loop
        controlPlayerLoop(playlist);
    }

    /**
     * Load songs from server response into local playlist
     * Parses song data and creates Song objects
     * @param playlist The playlist to load songs into
     */
    private void loadSongsIntoPlaylist(DoublyLinkedPlaylist playlist) throws IOException {
        String response;
        int songCount = 0;

        // Read song data until END marker
        while (!(response = in.readLine()).equals("END")) {
            String[] songData = response.split("\\|");

            if (songData.length >= 2) {
                // Parse song information
                String title = songData[0];
                String artist = songData[1];
                String album = songData.length > 2 ? songData[2] : "Unknown";
                String genre = songData.length > 3 ? songData[3] : "Unknown";
                int duration = 0;

                // Parse duration safely
                if (songData.length > 4 && !songData[4].isEmpty()) {
                    try {
                        duration = Integer.parseInt(songData[4]);
                    } catch (NumberFormatException e) {
                        duration = 0; // Default if parsing fails
                    }
                }

                // Get file path (last element)
                String filePath = songData.length > 5 ? songData[5] : null;

                // Create song object
                Song song = new Song(title, artist, album, genre, duration);
                if (filePath != null && !filePath.isEmpty()) {
                    song.setFilePath(filePath);
                }

                // Add to playlist
                playlist.addSong(song);
                songCount++;

                // Simple confirmation message
                System.out.println("♪ " + title + " by " + artist);
            }
        }

        System.out.println("==================================================================================");
        System.out.println("✅ Loaded " + songCount + " songs into playlist");
        System.out.println("==================================================================================");
    }

    /**
     * Choose playback mode (sequential, shuffle, repeat)
     * This method is called before starting the player
     */
    private void choosePlaybackMode() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Choose playback mode:");
        System.out.println("1. Sequential (play songs in order)");
        System.out.println("2. Shuffle (play songs randomly)");
        System.out.println("3. Repeat (repeat current song)");
        System.out.println("==================================================================================");
        System.out.print("Enter your choice (1-3): ");
        String modeChoice = scanner.nextLine().trim();

        // Send mode selection to server
        out.println("SET_PLAYBACK_MODE " + modeChoice);
        String response = in.readLine();

        // Configure local playback service
        if (playbackService != null) {
            switch (modeChoice) {
                case "1":
                    playbackService.setPlaybackMode(new SequentialPlayState());
                    System.out.println("✅ Sequential mode selected");
                    break;
                case "2":
                    playbackService.setPlaybackMode(new ShufflePlayState());
                    System.out.println("✅ Shuffle mode selected");
                    break;
                case "3":
                    playbackService.setPlaybackMode(new RepeatPlayState());
                    System.out.println("✅ Repeat mode selected");
                    break;
                default:
                    System.out.println("⚠️ Invalid choice. Using Sequential mode by default.");
                    playbackService.setPlaybackMode(new SequentialPlayState());
            }
        }

        // Check server response
        if (!response.startsWith("SUCCESS")) {
            System.out.println("Server response: " + response);
        }
    }

    /**
     * Main control loop with detailed status display
     */
    private void controlPlayerLoop(DoublyLinkedPlaylist playlist) throws IOException {
        // Initialize service
        playbackService = new PlaybackService(playlist, null);
        if (playbackService.getCurrentPlayMode() == null) {
            playbackService.setPlaybackMode(new SequentialPlayState());
        }

        // Reset state
        resetPlayerState();

        String input;
        boolean exitPlayer = false;

        while (!exitPlayer) {
            // Display detailed status
            System.out.println("==================================================================================");
            System.out.println("🎮 MUSIC PLAYER CONTROLS");
            System.out.println("Commands: play, pause, stop, next, prev, exit");
            System.out.println("Status: " + getPlayerStatus());
            System.out.println("Debug - isPlaying: " + isPlaying + ", isPaused: " + isPaused);
            if (currentSongTitle != null) {
                System.out.println("Current song: " + currentSongTitle);
            }
            System.out.println("==================================================================================");
            System.out.print("Enter command: ");

            input = scanner.nextLine().trim().toLowerCase();

            try {
                switch (input) {
                    case "play":
                        handlePlayCommand();
                        break;
                    case "pause":
                        handlePauseCommand();
                        break;
                    case "stop":
                        handleStopCommand();
                        break;
                    case "next":
                        handleNextCommand();
                        break;
                    case "prev":
                        handlePrevCommand();
                        break;
                    case "exit":
                        handleExitCommand();
                        exitPlayer = true;
                        break;
                    default:
                        System.out.println("❌ Unknown command. Use: play, pause, stop, next, prev, or exit");
                }

                // Show state after each command
                System.out.println("🔄 After command - Status: " + getPlayerStatus());

            } catch (Exception e) {
                System.out.println("⚠️ Error processing command: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle play command with complete state management
     */
    private void handlePlayCommand() throws IOException {
        if (isPaused && currentSongTitle != null) {
            // Resume from pause
            out.println("PLAYER_PLAY resume");
            String response = in.readLine();
            System.out.println("Server: " + response);

            // Resume audio and update state immediately
            audioPlayer.resume();
            isPlaying = true;
            isPaused = false;

            System.out.println("▶️ Resumed: " + currentSongTitle);
        } else {
            // Start new song
            out.println("PLAYER_PLAY");
            String response = in.readLine();
            System.out.println("Server: " + response);

            Song currentSong = playbackService.getCurrentSong();
            if (currentSong != null && currentSong.getFilePath() != null) {
                playNewSong(currentSong);
            } else {
                System.out.println("❌ Cannot play: no song available");
            }
        }
    }

    /**
     * Handle pause command with immediate state update
     */
    private void handlePauseCommand() throws IOException {
        System.out.println("🔄 Pause requested - Current state: " + getPlayerStatus());

        // Check if we can pause
        if (!isPlaying || isPaused) {
            System.out.println("⚠️ Cannot pause - not playing or already paused");
            return;
        }

        // Send to server
        out.println("PLAYER_PAUSE");
        String response = in.readLine();
        System.out.println("Server: " + response);

        // Pause audio and update state immediately
        audioPlayer.pause();
        isPlaying = false;
        isPaused = true;

        System.out.println("⏸️ Paused: " + (currentSongTitle != null ? currentSongTitle : "Unknown"));
    }

    /**
     * Handle stop command with immediate state reset
     */
    private void handleStopCommand() throws IOException {
        // Send to server
        out.println("PLAYER_STOP");
        String response = in.readLine();
        System.out.println("Server: " + response);

        // Stop audio and reset state immediately
        audioPlayer.stop();
        resetPlayerState();

        System.out.println("⏹️ Playback stopped");
    }

    /**
     * Handle next command with immediate state management
     */
    private void handleNextCommand() throws IOException {
        System.out.println("🔄 Moving to next song...");

        // Send to server
        out.println("PLAYER_NEXT");
        String response = in.readLine();
        System.out.println("Server: " + response);

        // Stop current audio immediately
        audioPlayer.stop();

        // Reset state immediately
        resetPlayerState();

        // Move to next song
        playbackService.next();
        Song nextSong = playbackService.getCurrentSong();

        // Play next song and update state
        if (nextSong != null && nextSong.getFilePath() != null) {
            System.out.println("⏭️ Next: " + nextSong.getTitle());
            playNewSong(nextSong);
        } else {
            System.out.println("❌ No next song available");
        }
    }

    /**
     * Handle previous command with immediate state management
     */
    private void handlePrevCommand() throws IOException {
        System.out.println("🔄 Moving to previous song...");

        // Send to server
        out.println("PLAYER_PREV");
        String response = in.readLine();
        System.out.println("Server: " + response);

        // Stop current audio immediately
        audioPlayer.stop();

        // Reset state immediately
        resetPlayerState();

        // Move to previous song
        playbackService.previous();
        Song prevSong = playbackService.getCurrentSong();

        // Play previous song and update state
        if (prevSong != null && prevSong.getFilePath() != null) {
            System.out.println("⏮️ Previous: " + prevSong.getTitle());
            playNewSong(prevSong);
        } else {
            System.out.println("❌ No previous song available");
        }
    }

    /**
     * Handle exit command with complete cleanup
     */
    private void handleExitCommand() throws IOException {
        // Stop audio immediately
        audioPlayer.stop();
        resetPlayerState();

        // Notify server
        out.println("PLAYER_STOP");
        try {
            String stopResponse = in.readLine();
            System.out.println("Stop response: " + stopResponse);
        } catch (Exception e) {
            System.err.println("Stop response error: " + e.getMessage());
        }

        out.println("PLAYER_EXIT");
        try {
            String exitResponse = in.readLine();
            System.out.println("Exit response: " + exitResponse);
        } catch (Exception e) {
            System.err.println("Exit response error: " + e.getMessage());
        }

        System.out.println("↩️ Returning to main menu...");
    }

    /**
     * Play new song with immediate state update
     * @param song Song to play
     */
    private void playNewSong(Song song) {
        System.out.println("🎵 Loading: " + song.getTitle() + " by " + song.getArtist());

        // Update ALL state immediately BEFORE starting audio
        currentSongTitle = song.getTitle();
        isPlaying = true;
        isPaused = false;
        pausePosition = 0;

        // Start audio playback
        audioPlayer.play(song.getFilePath());

        System.out.println("🎵 Now playing: " + currentSongTitle);
        System.out.println("🔄 State updated: " + getPlayerStatus());
    }

    /**
     * Reset all player state variables
     */
    private void resetPlayerState() {
        isPlaying = false;
        isPaused = false;
        pausePosition = 0;
        currentSongTitle = null;
        System.out.println("🔄 State reset: " + getPlayerStatus());
    }

    /**
     * Get current player status as string
     * @return Status string for debugging
     */
    private String getPlayerStatus() {
        String status = "";
        if (isPlaying) status += "PLAYING";
        else if (isPaused) status += "PAUSED";
        else status += "STOPPED";

        if (currentSongTitle != null) {
            status += " - " + currentSongTitle;
        }
        return status;
    }
}