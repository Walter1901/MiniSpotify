package client.ui;

import client.commands.*;
import playback.PlaybackService;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Interface utilisateur principale
 */
public class UserInterface {
    // Connexion au serveur
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    // État de l'application
    private boolean isLoggedIn = false;
    private String currentUser;
    private PlaybackService playbackService;

    // Sous-composants UI
    private AuthenticationUI authUI;
    private MusicLibraryUI libraryUI;
    private PlaylistManagerUI playlistUI;
    private PlayerUI playerUI;

    /**
     * Constructeur
     */
    public UserInterface(Socket socket, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.scanner = new Scanner(System.in);

        // Initialiser les sous-composants
        this.authUI = new AuthenticationUI(this, in, out, scanner);
        this.libraryUI = new MusicLibraryUI(this, in, out, scanner);
        this.playlistUI = new PlaylistManagerUI(this, in, out, scanner);
        this.playerUI = new PlayerUI(this, in, out, scanner);

        displayWelcomeBanner();
    }

    /**
     * Démarre l'interface utilisateur
     */
    public void start() {
        try {
            // Commencer par l'authentification
            if (!isLoggedIn) {
                authUI.showInitialMenu();
            }

            // Si connecté, afficher le menu principal
            if (isLoggedIn) {
                showMainMenu();
            }

            // Nettoyer les ressources
            cleanup();
        } catch (IOException e) {
            System.err.println("Error in user interface: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche le menu principal
     */
    private void showMainMenu() throws IOException {
        boolean exit = false;
        while (!exit && isLoggedIn) {
            displayMainMenuOptions();
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    libraryUI.manageMusicLibrary();
                    break;
                case "2":
                    playlistUI.managePlaylists();
                    break;
                case "3":
                    playerUI.startPlayer();
                    break;
                case "4":
                    authUI.logout();
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    /**
     * Affiche le banner de bienvenue
     */
    private void displayWelcomeBanner() {
        System.out.println(
                "       █████████████████████       \n" +
                        "    ████==================████     \n" +
                        "   ███=======================███   \n" +
                        " ██========== ███████ ==========██  \n" +
                        "██========= ██  W F  ██  ========██ \n" +
                        " ██========== ███████ ==========██  \n" +
                        "   ███=======================███   \n" +
                        "    ████==================████     \n" +
                        "       █████████████████████       \n" +
                        "\n" +
                        "       Welcome to MiniSpotify!\n");
    }

    /**
     * Affiche les options du menu principal
     */
    private void displayMainMenuOptions() {
        System.out.println("==================================================================================");
        System.out.println("\nMain Menu:");
        System.out.println("1. View Music Library");
        System.out.println("2. Manage my playlists");
        System.out.println("3. Start the player (command: play, pause, stop, next, prev, exit)");
        System.out.println("4. Logout");
        System.out.println("==================================================================================");
        System.out.print("Choose an option: ");
    }

    /**
     * Nettoie les ressources
     */
    private void cleanup() throws IOException {
        scanner.close();
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    // Getters et Setters

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public void setCurrentUser(String user) {
        currentUser = user;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public PlaybackService getPlaybackService() {
        return playbackService;
    }

    public void setPlaybackService(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }
}