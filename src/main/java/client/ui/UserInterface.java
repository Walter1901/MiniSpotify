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
    private boolean exitRequested = false; // Nouvel attribut pour quitter l'application

    // Sous-composants UI
    private AuthenticationUI authUI;
    private MusicLibraryUI libraryUI;
    private PlaylistManagerUI playlistUI;
    private PlayerUI playerUI;
    private SocialUI socialUI; // Composant pour les fonctionnalités sociales

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
        this.socialUI = new SocialUI(this, in, out, scanner, playerUI);

        displayWelcomeBanner();
    }

    /**
     * Démarre l'interface utilisateur
     */
    public void start() {
        try {
            // Boucle principale de l'application
            while (!exitRequested) {
                try {
                    // Commencer par l'authentification si non connecté
                    if (!isLoggedIn) {
                        authUI.showInitialMenu();

                        // Si l'utilisateur a demandé de quitter, sortir de la boucle
                        if (exitRequested) {
                            break;
                        }
                    }

                    // Si connecté, afficher le menu principal
                    if (isLoggedIn) {
                        showMainMenu();
                        // Après la déconnexion, le programme continue ici
                        // Nous vérifions si la raison de sortie n'est pas une demande de quitter
                        if (exitRequested) {
                            break;  // Si c'est une demande de quitter, sortir de la boucle
                        }
                        // Sinon, continuer la boucle pour revenir au menu d'accueil
                        continue;
                    }
                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                    System.out.println("==================================================================================");
                    System.out.println("Lost connection to server. Please restart the application.");
                    System.out.println("==================================================================================");
                    exitRequested = true;
                }
            }

            // Nettoyer les ressources
            cleanup();
        } catch (IOException e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche le menu principal
     */
    private void showMainMenu() throws IOException {
        boolean back = false;
        while (!back && isLoggedIn) {
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
                    socialUI.manageSocialFeatures();
                    break;
                case "5":
                    authUI.logout();
                    back = true;  // Ceci termine la boucle showMainMenu() pour revenir à start()
                    // IMPORTANT: NE PAS définir exitRequested = true ici !
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
        System.out.println("4. Social features");
        System.out.println("5. Logout");
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
        System.out.println("==================================================================================");
        System.out.println("Thank you for using MiniSpotify! Goodbye.");
        System.out.println("==================================================================================");
    }

    /**
     * Définit si l'application doit se terminer
     */
    public void setExitRequested(boolean exitRequested) {
        this.exitRequested = exitRequested;
    }

    /**
     * Vérifie si l'application doit se terminer
     */
    public boolean isExitRequested() {
        return exitRequested;
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

    /**
     * Récupère le composant PlayerUI
     */
    public PlayerUI getPlayerUI() {
        return playerUI;
    }

    /**
     * Récupère le composant social
     */
    public SocialUI getSocialUI() {
        return socialUI;
    }
}