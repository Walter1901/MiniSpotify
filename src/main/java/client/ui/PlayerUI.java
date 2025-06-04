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
     *
     * @param mainUI  Reference to main user interface
     * @param in      Input stream for server communication
     * @param out     Output stream for server communication
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
     *
     * @param playlistName  Name of the shared playlist
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
     *
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
     * Main control loop with beautiful, clean interface
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
            // Clear screen effect with spacing
            System.out.println("\n\n");

            // Beautiful header
            displayPlayerHeader();

            // Current status in a beautiful box
            displayCurrentStatus();

            // Command prompt
            displayCommandPrompt();

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
                        displayInvalidCommand();
                }

                // Brief pause for better UX
                if (!exitPlayer) {
                    Thread.sleep(800);
                }

            } catch (Exception e) {
                displayError(e.getMessage());
            }
        }
    }

    /**
     * Display beautiful player header
     */
    private void displayPlayerHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                            🎵 MINI SPOTIFY PLAYER 🎵                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Display current status in a beautiful format
     */
    private void displayCurrentStatus() {
        System.out.println("┌──────────────────────────────────────────────────────────────────────────────────┐");

        // Status line
        String statusIcon = getStatusIcon();
        String statusText = getStatusText();
        System.out.printf("│ Status: %s %-65s │%n", statusIcon, statusText);

        // Current song line
        if (currentSongTitle != null) {
            String songDisplay = "♪ " + currentSongTitle;
            if (songDisplay.length() > 68) {
                songDisplay = songDisplay.substring(0, 65) + "...";
            }
            System.out.printf("│ Song:   %-69s │%n", songDisplay);
        } else {
            System.out.printf("│ Song:   %-69s │%n", "No song selected");
        }

        System.out.println("└──────────────────────────────────────────────────────────────────────────────────┘");
    }

    /**
     * Display command prompt beautifully
     */
    private void displayCommandPrompt() {
        System.out.println();
        System.out.println("┌──────────────────────── Available Commands ────────────────────────┐");
        System.out.println("│▶️  play  │⏸️  pause  │⏹️  stop  │⏭️  next  │⏮️  prev  │🚪  exit   │");
        System.out.println("└────────────────────────────────────────────────────────────────────┘");
        System.out.print("🎮 Enter command: ");
    }

    /**
     * Get status icon based on current state
     */
    private String getStatusIcon() {
        if (isPlaying) return "▶️";
        else if (isPaused) return "⏸️";
        else return "⏹️";
    }

    /**
     * Get status text based on current state
     */
    private String getStatusText() {
        if (isPlaying) return "PLAYING";
        else if (isPaused) return "PAUSED";
        else return "STOPPED";
    }

    /**
     * Display invalid command message
     */
    private void displayInvalidCommand() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        ❌ Invalid command entered                                ║");
        System.out.println("║                   Please use: play, pause, stop, next, prev, exit                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Display error message beautifully
     */
    private void displayError(String message) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.printf(" ║                              ⚠️  Error: %-40s ║%n", message);
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Handle play command with clean output
     */
    private void handlePlayCommand() throws IOException {
        if (isPaused && currentSongTitle != null) {
            // Resume from pause
            out.println("PLAYER_PLAY resume");
            String response = in.readLine();

            audioPlayer.resume();
            isPlaying = true;
            isPaused = false;

            displayActionMessage("▶️ Resumed playback", currentSongTitle);
        } else {
            // Start new song
            out.println("PLAYER_PLAY");
            String response = in.readLine();

            Song currentSong = playbackService.getCurrentSong();
            if (currentSong != null && currentSong.getFilePath() != null) {
                playNewSong(currentSong);
            } else {
                displayActionMessage("❌ Error", "No song available to play");
            }
        }
    }

    /**
     * Handle pause command with clean output
     */
    private void handlePauseCommand() throws IOException {
        if (!isPlaying || isPaused) {
            displayActionMessage("⚠️ Cannot pause", "Not playing or already paused");
            return;
        }

        out.println("PLAYER_PAUSE");
        String response = in.readLine();

        audioPlayer.pause();
        isPlaying = false;
        isPaused = true;

        displayActionMessage("⏸️ Paused", currentSongTitle != null ? currentSongTitle : "Unknown");
    }

    /**
     * Handle stop command with clean output
     */
    private void handleStopCommand() throws IOException {
        out.println("PLAYER_STOP");
        String response = in.readLine();

        audioPlayer.stop();
        resetPlayerState();

        displayActionMessage("⏹️ Stopped", "Playback stopped");
    }

    /**
     * Handle next command with clean output
     */
    private void handleNextCommand() throws IOException {
        out.println("PLAYER_NEXT");
        String response = in.readLine();

        audioPlayer.stop();
        resetPlayerState();

        playbackService.next();
        Song nextSong = playbackService.getCurrentSong();

        if (nextSong != null && nextSong.getFilePath() != null) {
            playNewSong(nextSong);
            displayActionMessage("⏭️ Next song", nextSong.getTitle());
        } else {
            displayActionMessage("❌ Error", "No next song available");
        }
    }

    /**
     * Handle previous command with clean output
     */
    private void handlePrevCommand() throws IOException {
        out.println("PLAYER_PREV");
        String response = in.readLine();

        audioPlayer.stop();
        resetPlayerState();

        playbackService.previous();
        Song prevSong = playbackService.getCurrentSong();

        if (prevSong != null && prevSong.getFilePath() != null) {
            playNewSong(prevSong);
            displayActionMessage("⏮️ Previous song", prevSong.getTitle());
        } else {
            displayActionMessage("❌ Error", "No previous song available");
        }
    }

    /**
     * Handle exit command with clean output
     */
    private void handleExitCommand() throws IOException {
        audioPlayer.stop();
        resetPlayerState();

        out.println("PLAYER_STOP");
        try {
            String stopResponse = in.readLine();
        } catch (Exception e) {
            // Silent error handling
        }

        out.println("PLAYER_EXIT");
        try {
            String exitResponse = in.readLine();
        } catch (Exception e) {
            // Silent error handling
        }

        displayActionMessage("🚪 Exiting", "Returning to main menu...");
    }

    /**
     * Play new song with clean state update
     */
    private void playNewSong(Song song) {
        // Update state immediately
        currentSongTitle = song.getTitle();
        isPlaying = true;
        isPaused = false;
        pausePosition = 0;

        // Start audio playback silently
        audioPlayer.play(song.getFilePath());

        displayActionMessage("🎵 Now playing", song.getTitle() + " by " + song.getArtist());
    }

    /**
     * Reset player state silently
     */
    private void resetPlayerState() {
        isPlaying = false;
        isPaused = false;
        pausePosition = 0;
        currentSongTitle = null;
    }

    /**
     * Display action message in a beautiful format
     */
    private void displayActionMessage(String action, String details) {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────────────────────────────────────────┐");
        System.out.printf("│ %s %-67s │%n", action, "");
        if (details != null && !details.isEmpty()) {
            if (details.length() > 74) {
                details = details.substring(0, 71) + "...";
            }
            System.out.printf("│ ➤ %-75s │%n", details);
        }
        System.out.println("└──────────────────────────────────────────────────────────────────────────────────┘");
    }

    /**
     * Choose playback mode with beautiful interface
     */
    private void choosePlaybackMode() throws IOException {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                           🎛️  PLAYBACK MODE SELECTION                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║                                                                                  ║");
        System.out.println("║    [1]  Sequential   → Play songs in order                                       ║");
        System.out.println("║    [2]  Shuffle      → Play songs randomly                                       ║");
        System.out.println("║    [3]  Repeat       → Repeat current song                                       ║");
        System.out.println("║                                                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
        System.out.print("🎯 Enter your choice (1-3): ");

        String modeChoice = scanner.nextLine().trim();

        out.println("SET_PLAYBACK_MODE " + modeChoice);
        String response = in.readLine();

        if (playbackService != null) {
            String modeName = "";
            switch (modeChoice) {
                case "1":
                    playbackService.setPlaybackMode(new SequentialPlayState());
                    modeName = "Sequential";
                    break;
                case "2":
                    playbackService.setPlaybackMode(new ShufflePlayState());
                    modeName = "Shuffle";
                    break;
                case "3":
                    playbackService.setPlaybackMode(new RepeatPlayState());
                    modeName = "Repeat";
                    break;
                default:
                    playbackService.setPlaybackMode(new SequentialPlayState());
                    modeName = "Sequential (Default)";
            }

            displayActionMessage("✅ Mode selected", modeName + " mode activated");
            try {
                Thread.sleep(1500); // Brief pause to show the selection
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}