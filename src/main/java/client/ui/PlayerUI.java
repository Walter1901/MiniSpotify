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
 * Interface utilisateur pour le lecteur audio
 */
public class PlayerUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private CommandInvoker commandInvoker;
    private PlaybackService playbackService;
    private AudioPlayer audioPlayer;

    // Variables pour gérer l'état de lecture
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long pausePosition = 0;
    private String currentSongTitle = null;

    /**
     * Constructeur
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
     * Démarre le lecteur
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

        // Obtenir les chansons de la playlist
        out.println("GET_PLAYLIST_SONGS " + playlistName);
        response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("Error loading playlist: " + response);
            return;
        }

        // Créer la playlist locale
        DoublyLinkedPlaylist playlist = new DoublyLinkedPlaylist();

        // Lire les chansons
        while (!(response = in.readLine()).equals("END")) {
            String[] songData = response.split("\\|");
            if (songData.length >= 2) {
                String title = songData[0];
                String artist = songData[1];
                String album = songData.length > 2 ? songData[2] : "Unknown";
                String genre = songData.length > 3 ? songData[3] : "Unknown";
                int duration = songData.length > 4 && !songData[4].isEmpty() ? Integer.parseInt(songData[4]) : 0;

                // Le chemin du fichier est la DERNIÈRE partie
                String filePath = songData.length > 5 ? songData[5] : null;

                Song song = new Song(title, artist, album, genre, duration);
                if (filePath != null && !filePath.isEmpty()) {
                    song.setFilePath(filePath);
                }

                System.out.println("DEBUG: Added song to playlist: " + title + " with path: " + filePath);
                playlist.addSong(song);
            }
        }

        // Informer le serveur que la playlist est chargée
        out.println("LOAD_PLAYLIST " + playlistName);
        response = in.readLine();
        System.out.println("Loading playlist response: " + response);

        // IMPORTANT: Initialiser le service AVANT de choisir le mode de lecture
        playbackService = new PlaybackService(playlist, null);

        // Choix du mode après avoir initialisé le service
        choosePlaybackMode();

        // Boucle de contrôle du lecteur
        controlPlayerLoop(playlist);
    }

    /**
     * Démarre le lecteur avec une playlist déjà chargée (pour les playlists partagées)
     */
    public void startPlayerWithLoadedPlaylist(String playlistName, String ownerUsername) throws IOException {
        System.out.println("==================================================================================");
        System.out.println("\n--- Audio Player ---");
        System.out.println("Playing shared playlist: " + playlistName + " (shared by " + ownerUsername + ")");
        System.out.println("==================================================================================");

        // Choix du mode de lecture
        choosePlaybackMode();

        // Créer la playlist locale
        DoublyLinkedPlaylist playlist = new DoublyLinkedPlaylist();

        // Obtenir les chansons de la playlist partagée
        out.println("GET_SHARED_PLAYLIST_SONGS " + ownerUsername + " " + playlistName);
        String response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("Error loading playlist: " + response);
            return;
        }

        // Lire les chansons
        while (!(response = in.readLine()).equals("END")) {
            String[] songData = response.split("\\|");
            if (songData.length >= 2) {
                String title = songData[0];
                String artist = songData[1];
                String album = songData.length > 2 ? songData[2] : "Unknown";
                String genre = songData.length > 3 ? songData[3] : "Unknown";
                int duration = songData.length > 4 && !songData[4].isEmpty() ? Integer.parseInt(songData[4]) : 0;

                // Le chemin du fichier est la DERNIÈRE partie
                String filePath = songData.length > 5 ? songData[5] : null;

                Song song = new Song(title, artist, album, genre, duration);
                if (filePath != null && !filePath.isEmpty()) {
                    song.setFilePath(filePath);
                }

                System.out.println("DEBUG: Added song to playlist: " + title + " with path: " + filePath);
                playlist.addSong(song);
            }
        }

        // Boucle de contrôle du lecteur
        controlPlayerLoop(playlist);
    }

    /**
     * Boucle de contrôle du lecteur (factorisation du code commun)
     */
    private void controlPlayerLoop(DoublyLinkedPlaylist playlist) throws IOException {
        // Initialiser le service avec la playlist
        playbackService = new PlaybackService(playlist, null);

        // Configuration du mode de lecture
        if (playbackService.getCurrentPlayMode() == null) {
            playbackService.setPlaybackMode(new SequentialPlayState()); // Mode par défaut
        }

        // Réinitialiser l'état de lecture
        isPlaying = false;
        isPaused = false;
        pausePosition = 0;

        // Boucle de contrôle
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
                        // Si on était en pause, reprendre la lecture
                        out.println("PLAYER_PLAY resume");
                        String response = in.readLine();
                        System.out.println(response);
                        audioPlayer.resume();
                        isPlaying = true;
                        isPaused = false;
                    } else {
                        // Nouvelle lecture
                        out.println("PLAYER_PLAY");
                        String response = in.readLine();
                        System.out.println(response);

                        // Obtenir la chanson actuelle et la jouer
                        Song currentSong = playbackService.getCurrentSong();
                        if (currentSong != null && currentSong.getFilePath() != null) {
                            audioPlayer.play(currentSong.getFilePath());
                            currentSongTitle = currentSong.getTitle();
                        }

                        isPlaying = true;
                        isPaused = false;
                        pausePosition = 0;
                    }
                    break;

                case "pause":
                    if (isPlaying) {
                        out.println("PLAYER_PAUSE");
                        String response = in.readLine();
                        System.out.println(response);

                        // Pause du lecteur audio et récupération de sa position
                        audioPlayer.pause();
                        isPlaying = false;
                        isPaused = true;
                    } else {
                        System.out.println("No music is playing.");
                    }
                    break;

                case "stop":
                    out.println("PLAYER_STOP");
                    String stopResponse = in.readLine();
                    System.out.println(stopResponse);

                    // Arrêt complet du lecteur
                    audioPlayer.stop();
                    isPlaying = false;
                    isPaused = false;
                    pausePosition = 0;
                    break;

                case "next":
                    out.println("PLAYER_NEXT");
                    String nextResponse = in.readLine();
                    System.out.println(nextResponse);

                    // Arrêter la lecture actuelle
                    audioPlayer.stop();

                    // Passer à la chanson suivante
                    playbackService.next();

                    // Lecture automatique de la nouvelle chanson
                    Song nextSong = playbackService.getCurrentSong();
                    if (nextSong != null && nextSong.getFilePath() != null) {
                        audioPlayer.play(nextSong.getFilePath());
                        currentSongTitle = nextSong.getTitle();
                    }

                    isPlaying = true;
                    isPaused = false;
                    pausePosition = 0;
                    break;

                case "prev":
                    out.println("PLAYER_PREV");
                    String prevResponse = in.readLine();
                    System.out.println(prevResponse);

                    // Arrêter la lecture actuelle
                    audioPlayer.stop();

                    // Revenir à la chanson précédente
                    playbackService.previous();

                    // Lecture automatique de la nouvelle chanson
                    Song prevSong = playbackService.getCurrentSong();
                    if (prevSong != null && prevSong.getFilePath() != null) {
                        audioPlayer.play(prevSong.getFilePath());
                        currentSongTitle = prevSong.getTitle();
                    }

                    isPlaying = true;
                    isPaused = false;
                    pausePosition = 0;
                    break;

                case "exit":
                    // Important: Arrêter la lecture avant de quitter
                    if (isPlaying || isPaused) {
                        audioPlayer.stop();
                        out.println("PLAYER_STOP");
                        try {
                            String exitStopResponse = in.readLine();
                            System.out.println("Stop response: " + exitStopResponse);
                        } catch (Exception e) {
                            System.err.println("Error reading stop response: " + e.getMessage());
                            // Continue despite error
                        }
                    }

                    // Notifier le serveur de la sortie du mode player
                    out.println("PLAYER_EXIT");

                    try {
                        String exitResponse = in.readLine();
                        System.out.println("Exit player response: " + exitResponse);
                    } catch (Exception e) {
                        System.err.println("Error reading exit response: " + e.getMessage());
                        // Continue despite error
                    }

                    System.out.println("Return to main menu..");

                    // Réinitialiser l'état sans dépendre des réponses du serveur
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
     * Choix du mode de lecture
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

        // SUPPRIMER CETTE LIGNE:
        // this.selectedMode = modeChoice;

        // Si playbackService est null, informer l'utilisateur
        if (playbackService == null) {
            System.out.println("Error: Playback service not initialized yet.");
            return; // Sortir de la méthode sans tenter d'utiliser playbackService
        }

        // Configuration du mode de lecture seulement si playbackService n'est pas null
        switch (modeChoice) {
            case "1":
                playbackService.setPlaybackMode(new SequentialPlayState());
                break;
            case "2":
                playbackService.setPlaybackMode(new ShufflePlayState());
                break;
            case "3":
                playbackService.setPlaybackMode(new RepeatPlayState());
                break;
            default:
                System.out.println("Unknown mode. Sequential mode selected by default.");
                playbackService.setPlaybackMode(new SequentialPlayState());
        }

        if (!response.startsWith("SUCCESS")) {
            System.out.println("Unknown mode. Sequential mode selected by default.");
        }
    }

    /**
     * Applique le mode de lecture au service
     */
    private void applyPlaybackMode(String modeChoice) {
        // Vérifier que le service est initialisé
        if (playbackService == null) {
            System.out.println("Playback service not initialized yet.");
            return;
        }

        // Configuration du mode de lecture
        switch (modeChoice) {
            case "1":
                playbackService.setPlaybackMode(new SequentialPlayState());
                break;
            case "2":
                playbackService.setPlaybackMode(new ShufflePlayState());
                break;
            case "3":
                playbackService.setPlaybackMode(new RepeatPlayState());
                break;
            default:
                System.out.println("Unknown mode. Sequential mode selected by default.");
                playbackService.setPlaybackMode(new SequentialPlayState());
        }
    }
}