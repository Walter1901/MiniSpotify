package client.ui;

import client.commands.*;
import playback.*;
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
    private StreamingAudioPlayer streamingPlayer;

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
        this.streamingPlayer = new StreamingAudioPlayer(); // Utiliser le nouveau player
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
        if (playbackService.getCurrentPlayMode() == null) {
            playbackService.setPlaybackMode(new SequentialPlayState());
        }

        // État initial
        isPlaying = false;
        isPaused = false;
        pausePosition = 0;

        String input;
        boolean exitPlayer = false;

        while (!exitPlayer) {
            System.out.println("==================================================================================");
            System.out.println("Commands: play, pause, resume, stop, next, prev, exit");
            System.out.println("Current status: " +
                    (streamingPlayer.isPlaying() ?
                            (streamingPlayer.isPaused() ? "PAUSED" : "PLAYING") : "STOPPED"));
            System.out.println("==================================================================================");
            input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "play":
                    if (streamingPlayer.isPaused()) {
                        // Si en pause, reprendre
                        streamingPlayer.resume();
                        out.println("PLAYER_RESUME");
                        String response = in.readLine();
                        System.out.println("▶️ " + response);
                    } else if (!streamingPlayer.isPlaying()) {
                        // Nouvelle lecture
                        Song currentSong = playbackService.getCurrentSong();
                        if (currentSong != null && currentSong.getFilePath() != null) {
                            // Utiliser le streaming au lieu de la lecture locale
                            boolean success = streamingPlayer.playStream(
                                    "localhost", 12345, currentSong.getFilePath()
                            );

                            if (success) {
                                currentSongTitle = currentSong.getTitle();
                                System.out.println("🎵 Streaming started: " + currentSongTitle);

                                out.println("PLAYER_PLAY");
                                String response = in.readLine();
                                System.out.println(response);
                            } else {
                                System.out.println("❌ Failed to start streaming");
                            }
                        } else {
                            System.out.println("❌ No song selected or file path missing");
                        }
                    } else {
                        System.out.println("ℹ️ Already playing. Use 'pause' to pause or 'stop' to stop.");
                    }
                    break;

                case "pause":
                    if (streamingPlayer.isPlaying() && !streamingPlayer.isPaused()) {
                        streamingPlayer.pause();
                        out.println("PLAYER_PAUSE");
                        String response = in.readLine();
                        System.out.println("⏸️ " + response);
                    } else if (streamingPlayer.isPaused()) {
                        System.out.println("ℹ️ Already paused. Use 'resume' or 'play' to continue.");
                    } else {
                        System.out.println("ℹ️ Nothing is playing.");
                    }
                    break;

                case "resume":
                    if (streamingPlayer.isPaused()) {
                        streamingPlayer.resume();
                        out.println("PLAYER_RESUME");
                        String response = in.readLine();
                        System.out.println("▶️ " + response);
                    } else {
                        System.out.println("ℹ️ Not paused. Use 'play' to start or 'pause' to pause.");
                    }
                    break;

                case "stop":
                    streamingPlayer.stop();
                    out.println("PLAYER_STOP");
                    String stopResponse = in.readLine();
                    System.out.println("⏹️ " + stopResponse);
                    currentSongTitle = null;
                    break;

                case "next":
                    // Arrêter le streaming actuel
                    streamingPlayer.stop();

                    // Passer à la chanson suivante
                    playbackService.next();

                    // Démarrer le streaming de la nouvelle chanson
                    Song nextSong = playbackService.getCurrentSong();
                    if (nextSong != null && nextSong.getFilePath() != null) {
                        boolean success = streamingPlayer.playStream(
                                "localhost", 12345, nextSong.getFilePath()
                        );

                        if (success) {
                            currentSongTitle = nextSong.getTitle();
                            System.out.println("⏭️ Next song streaming: " + currentSongTitle);
                        }
                    }

                    out.println("PLAYER_NEXT");
                    String nextResponse = in.readLine();
                    System.out.println(nextResponse);
                    break;

                case "prev":
                    // Même logique que "next" mais pour la chanson précédente
                    streamingPlayer.stop();
                    playbackService.previous();

                    Song prevSong = playbackService.getCurrentSong();
                    if (prevSong != null && prevSong.getFilePath() != null) {
                        boolean success = streamingPlayer.playStream(
                                "localhost", 12345, prevSong.getFilePath()
                        );

                        if (success) {
                            currentSongTitle = prevSong.getTitle();
                            System.out.println("⏮️ Previous song streaming: " + currentSongTitle);
                        }
                    }

                    out.println("PLAYER_PREV");
                    String prevResponse = in.readLine();
                    System.out.println(prevResponse);
                    break;

                case "exit":
                    // Arrêter le streaming avant de quitter
                    streamingPlayer.stop();

                    out.println("PLAYER_EXIT");
                    try {
                        String exitResponse = in.readLine();
                        System.out.println("Exit response: " + exitResponse);
                    } catch (Exception e) {
                        System.err.println("Error reading exit response: " + e.getMessage());
                    }

                    System.out.println("🚪 Exiting player...");
                    currentSongTitle = null;
                    exitPlayer = true;
                    break;

                default:
                    System.out.println("❓ Unknown command. Available: play, pause, resume, stop, next, prev, exit");
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