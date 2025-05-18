package client.ui;

import client.commands.*;
import playback.PlaybackService;
import playback.RepeatPlayState;
import playback.SequentialPlayState;
import playback.ShufflePlayState;
import server.music.DoublyLinkedPlaylist;
import server.music.Song;
import users.User;

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

    /**
     * Constructeur
     */
    public PlayerUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
        this.commandInvoker = new CommandInvoker();
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

        if (response.startsWith("ERROR")) {
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

                // Le chemin du fichier est la DERNIÈRE partie - assurez-vous qu'il n'est pas formaté comme une chaîne avec des séparateurs
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

        // Choix du mode de lecture
        System.out.println("Choose reading mode:");
        System.out.println("1. Sequential");
        System.out.println("2. Shuffle");
        System.out.println("3. Repeat");
        System.out.println("==================================================================================");
        String modeChoice = scanner.nextLine().trim();

        out.println("SET_PLAYBACK_MODE " + modeChoice);
        response = in.readLine();
        System.out.println("Setting playback mode response: " + response);

        // Initialiser le service avec la playlist
        playbackService = new PlaybackService(playlist, null);

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

        // Définir les commandes
        CommandInvoker commandInvoker = new CommandInvoker();
        commandInvoker.register("play", new PlayCommand(playbackService));
        commandInvoker.register("pause", new PauseCommand(playbackService));
        commandInvoker.register("stop", new StopCommand(playbackService));
        commandInvoker.register("next", new NextCommand(playbackService));
        commandInvoker.register("prev", new PrevCommand(playbackService));

        // Boucle de contrôle
        String input;
        boolean exitPlayer = false;

        while (!exitPlayer) {
            System.out.println("==================================================================================");
            System.out.println("Enter a command (play, pause, stop, next, prev, exit): ");
            System.out.println("==================================================================================");
            input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("exit")) {
                out.println("PLAYER_EXIT");
                response = in.readLine(); // Lire la réponse mais ne pas l'afficher
                System.out.println("Return to main menu..");
                exitPlayer = true;
            } else if (commandInvoker.hasCommand(input)) {
                // Exécuter la commande localement
                commandInvoker.execute(input);

                // Envoyer la commande au serveur et lire la réponse
                out.println("PLAYER_" + input.toUpperCase());
                response = in.readLine(); // Lire la réponse mais ne pas l'afficher
            } else {
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

        if (!response.startsWith("SUCCESS")) {
            System.out.println("Unknown mode. Sequential mode selected by default.");
        }
    }

    /**
     * Enregistre les commandes dans l'invocateur
     */
    private void registerCommands() {
        commandInvoker.register("play", new PlayCommand(playbackService));
        commandInvoker.register("pause", new PauseCommand(playbackService));
        commandInvoker.register("stop", new StopCommand(playbackService));
        commandInvoker.register("next", new NextCommand(playbackService));
        commandInvoker.register("prev", new PrevCommand(playbackService));
    }

    /**
     * Exécute la boucle de contrôle du lecteur
     */
    private void runPlayerControlLoop() throws IOException {
        String input;
        boolean exitPlayer = false;

        while (!exitPlayer) {
            System.out.println("==================================================================================");
            System.out.println("Enter a command (play, pause, stop, next, prev, exit): ");
            System.out.println("==================================================================================");
            input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("exit")) {
                out.println("PLAYER_EXIT");
                System.out.println("Return to main menu..");
                exitPlayer = true;
            } else if (commandInvoker.hasCommand(input)) {
                // Exécuter la commande localement
                commandInvoker.execute(input);

                // Envoyer la commande au serveur
                out.println("PLAYER_" + input.toUpperCase());
                System.out.println(in.readLine());
            } else {
                System.out.println("Unknown command.");
            }
        }
    }

    /**
     * Récupère l'utilisateur depuis le serveur
     */
    private User getUserFromServer() {
        // Cette méthode est un placeholder
        // Dans une implémentation réelle, vous récupéreriez l'utilisateur depuis le serveur
        return null;
    }
}