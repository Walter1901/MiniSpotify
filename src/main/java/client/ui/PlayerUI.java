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
 * User interface for audio player
 */
public class PlayerUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private CommandInvoker commandInvoker;
    private PlaybackService playbackService;
    private AudioPlayer audioPlayer;

    // Variables to manage playback state
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long pausePosition = 0;
    private String currentSongTitle = null;

    /**
     * Constructor
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
     * Starts the player
     */
    public void startPlayer() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("\n--- Audio Player ---");
        System.out.println("==================================================================================");

        out.println("GET_PLAYLISTS");
        String response;
        List<String> playlists = new ArrayList<>();

        while (!(response = in.readLine()).equals("END")) {
            playlists.add(response);
            System.out.println(response);
        }

        if (playlists.isEmpty()) {
            System.out.println("No playlist exists. Please create a playlist first.");
            return;
        }

        System.out.println("==================================================================================");
        System.out.println("Enter the name of the playlist to play: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine();

        // Get the songs from the playlist
        out.println("GET_PLAYLIST_SONGS " + playlistName);
        response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("Error loading playlist: " + response);
            return;
        }

        // Create local playlist
        DoublyLinkedPlaylist playlist = new DoublyLinkedPlaylist();

        // Read songs
        while (!(response = in.readLine()).equals("END")) {
            String[] songData = response.split("\\|");
            if (songData.length >= 2) {
                String title = songData[0];
                String artist = songData[1];
                String album = songData.length > 2 ? songData[2] : "Unknown";
                String genre = songData.length > 3 ? songData[3] : "Unknown";
                int duration = songData.length > 4 && !songData[4].isEmpty() ? Integer.parseInt(songData[4]) : 0;

                // File path is the LAST part
                String filePath = songData.length > 5 ? songData[5] : null;

                Song song = new Song(title, artist, album, genre, duration);
                if (filePath != null && !filePath.isEmpty()) {
                    song.setFilePath(filePath);
                }

                System.out.println("DEBUG: Added song to playlist: " + title + " with path: " + filePath);
                playlist.addSong(song);
            }
        }

        // Inform the server that the playlist is loaded
        out.println("LOAD_PLAYLIST " + playlistName);
        response = in.readLine();
        System.out.println("Loading playlist response: " + response);

        // IMPORTANT: Initialize service BEFORE choosing playback mode
        playbackService = new PlaybackService(playlist, null);

        // Choose mode after initializing service
        choosePlaybackMode();

        // Player control loop
        controlPlayerLoop(playlist);
    }

    /**
     * Starts the player with a pre-loaded playlist (for shared playlists)
     */
    public void startPlayerWithLoadedPlaylist(String playlistName, String ownerUsername) throws IOException {
        System.out.println("==================================================================================");
        System.out.println("\n--- Audio Player ---");
        System.out.println("Playing shared playlist: " + playlistName + " (shared by " + ownerUsername + ")");
        System.out.println("==================================================================================");

        // Choose playback mode
        choosePlaybackMode();

        // Create local playlist
        DoublyLinkedPlaylist playlist = new DoublyLinkedPlaylist();

        // Get songs from shared playlist
        out.println("GET_SHARED_PLAYLIST_SONGS " + ownerUsername + " " + playlistName);
        String response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("Error loading playlist: " + response);
            return;
        }

        // Read songs
        while (!(response = in.readLine()).equals("END")) {
            String[] songData = response.split("\\|");
            if (songData.length >= 2) {
                String title = songData[0];
                String artist = songData[1];
                String album = songData.length > 2 ? songData[2] : "Unknown";
                String genre = songData.length > 3 ? songData[3] : "Unknown";
                int duration = songData.length > 4 && !songData[4].isEmpty() ? Integer.parseInt(songData[4]) : 0;

                // File path is the LAST part
                String filePath = songData.length > 5 ? songData[5] : null;

                Song song = new Song(title, artist, album, genre, duration);
                if (filePath != null && !filePath.isEmpty()) {
                    song.setFilePath(filePath);
                }

                System.out.println("DEBUG: Added song to playlist: " + title + " with path: " + filePath);
                playlist.addSong(song);
            }
        }

        // Player control loop
        controlPlayerLoop(playlist);
    }

    /**
     * Player control loop (factored common code)
     */
    private void controlPlayerLoop(DoublyLinkedPlaylist playlist) throws IOException {
        // Initialize service with playlist
        playbackService = new PlaybackService(playlist, null);

        // Configure playback mode
        if (playbackService.getCurrentPlayMode() == null) {
            playbackService.setPlaybackMode(new SequentialPlayState()); // Default mode
        }

        // Reset playback state
        isPlaying = false;
        isPaused = false;
        pausePosition = 0;

        // Control loop
        String input;
        boolean exitPlayer = false;

        while (!exitPlayer) {
            System.out.println("==================================================================================");
            System.out.println("Enter a command (play, pause, stop, next, prev, exit): ");
            System.out.println("==================================================================================");
            input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "play":
                    if (isPaused) {
                        // If paused, resume playback
                        out.println("PLAYER_PLAY resume");
                        String response = in.readLine();
                        System.out.println(response);
                        audioPlayer.resume();
                        isPlaying = true;
                        isPaused = false;
                    } else {
                        // New playback
                        out.println("PLAYER_PLAY");
                        String response = in.readLine();
                        System.out.println(response);

                        // Get current song and play
                        Song currentSong = playbackService.getCurrentSong();
                        if (currentSong != null && currentSong.getFilePath() != null) {
                            audioPlayer.play(currentSong.getFilePath());
                            currentSongTitle = currentSong.getTitle();
                            System.out.println("Now playing: " + currentSongTitle);
                        } else {
                            System.out.println("Cannot play: song or file path is missing");
                        }

                        isPlaying = true;
                        isPaused = false;
                        pausePosition = 0;
                    }
                    break;

                case "pause":
                    if (isPlaying && !isPaused) {
                        out.println("PLAYER_PAUSE");
                        String response = in.readLine();
                        System.out.println(response);

                        // Pause player and get position
                        audioPlayer.pause();
                        isPlaying = false;
                        isPaused = true;
                        System.out.println("Playback paused" + (currentSongTitle != null ? ": " + currentSongTitle : ""));
                    } else if (isPaused) {
                        System.out.println("Already paused.");
                    } else {
                        System.out.println("No music is playing.");
                    }
                    break;

                case "stop":
                    out.println("PLAYER_STOP");
                    String stopResponse = in.readLine();
                    System.out.println(stopResponse);

                    // Full stop
                    audioPlayer.stop();
                    isPlaying = false;
                    isPaused = false;
                    pausePosition = 0;
                    System.out.println("Playback stopped");
                    break;

                case "next":
                    out.println("PLAYER_NEXT");
                    String nextResponse = in.readLine();
                    System.out.println(nextResponse);

                    // Stop current playback
                    audioPlayer.stop();

                    // Go to next song
                    playbackService.next();

                    // Automatic playback of the new song
                    Song nextSong = playbackService.getCurrentSong();
                    if (nextSong != null && nextSong.getFilePath() != null) {
                        audioPlayer.play(nextSong.getFilePath());
                        currentSongTitle = nextSong.getTitle();
                        System.out.println("Now playing: " + currentSongTitle);
                    } else {
                        System.out.println("Cannot play next song: song or file path is missing");
                    }

                    isPlaying = true;
                    isPaused = false;
                    pausePosition = 0;
                    break;

                case "prev":
                    out.println("PLAYER_PREV");
                    String prevResponse = in.readLine();
                    System.out.println(prevResponse);

                    // Stop current playback
                    audioPlayer.stop();

                    // Go to previous song
                    playbackService.previous();

                    // Automatic playback of the new song
                    Song prevSong = playbackService.getCurrentSong();
                    if (prevSong != null && prevSong.getFilePath() != null) {
                        audioPlayer.play(prevSong.getFilePath());
                        currentSongTitle = prevSong.getTitle();
                        System.out.println("Now playing: " + currentSongTitle);
                    } else {
                        System.out.println("Cannot play previous song: song or file path is missing");
                    }

                    isPlaying = true;
                    isPaused = false;
                    pausePosition = 0;
                    break;

                case "exit":
                    // Important: Stop playback before exiting
                    if (isPlaying || isPaused) {
                        // First stop the audio player completely
                        audioPlayer.stop();
                        isPlaying = false;
                        isPaused = false;
                        pausePosition = 0;
                        currentSongTitle = null;

                        // Then notify server
                        out.println("PLAYER_STOP");

                        // Use a local variable inside the try block
                        try {
                            // Read response but don't store in a variable that's used outside the try block
                            stopResponse = in.readLine();
                            System.out.println("Stop response: " + stopResponse);
                        } catch (Exception e) {
                            System.err.println("Error reading stop response: " + e.getMessage());
                            // Continue despite error
                        }
                    }

                    // Notify server of player mode exit
                    out.println("PLAYER_EXIT");

                    try {
                        // Same approach here - keep the variable inside the try block
                        String exitResponse = in.readLine();
                        System.out.println("Exit player response: " + exitResponse);
                    } catch (Exception e) {
                        System.err.println("Error reading exit response: " + e.getMessage());
                        // Continue despite error
                    }

                    System.out.println("Return to main menu..");

                    // Reset state without depending on server responses
                    isPlaying = false;
                    isPaused = false;
                    pausePosition = 0;
                    currentSongTitle = null;
                    exitPlayer = true;
                    break;

                default:
                    System.out.println("Unknown command.");
            }
        }
    }

    /**
     * Choose playback mode
     */
    private void choosePlaybackMode() throws IOException {
        System.out.println("Choose reading mode:");
        System.out.println("1. Sequential");
        System.out.println("2. Shuffle");
        System.out.println("3. Repeat");
        System.out.println("==================================================================================");
        String modeChoice = scanner.nextLine().trim();

        out.println("SET_PLAYBACK_MODE " + modeChoice);
        String response = in.readLine();

        // If playbackService is null, inform user
        if (playbackService == null) {
            System.out.println("Error: Playback service not initialized yet.");
            return; // Exit method without attempting to use playbackService
        }

        // Configure playback mode only if playbackService is not null
        switch (modeChoice) {
            case "1":
                playbackService.setPlaybackMode(new SequentialPlayState());
                System.out.println("Sequential mode selected");
                break;
            case "2":
                playbackService.setPlaybackMode(new ShufflePlayState());
                System.out.println("Shuffle mode selected");
                break;
            case "3":
                playbackService.setPlaybackMode(new RepeatPlayState());
                System.out.println("Repeat mode selected");
                break;
            default:
                System.out.println("Unknown mode. Sequential mode selected by default.");
                playbackService.setPlaybackMode(new SequentialPlayState());
        }

        if (!response.startsWith("SUCCESS")) {
            System.out.println("Server response: " + response);
        }
    }
}