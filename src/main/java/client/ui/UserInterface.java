package client.ui;

import client.commands.*;
import playback.PlaybackService;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Main user interface class for the MiniSpotify client application.
 * Manages the overall user experience and coordinates between different UI components.
 * Implements a clean, minimal console interface without decorative borders.
 *
 * Features:
 * - Clean ASCII art banner
 * - Minimal menu system with icons
 * - Simple error handling displays
 * - Multi-platform screen clearing
 * - Seamless navigation between components
 */
public class UserInterface {
    // ===============================
    // CONNECTION AND NETWORKING
    // ===============================

    /** Socket connection to the server */
    private Socket socket;

    /** Input stream for reading server responses */
    private BufferedReader in;

    /** Output stream for sending commands to server */
    private PrintWriter out;

    /** Scanner for reading user input from console */
    private Scanner scanner;

    // ===============================
    // APPLICATION STATE
    // ===============================

    /** Flag indicating if user is currently logged in */
    private boolean isLoggedIn = false;

    /** Username of the currently logged in user */
    private String currentUser;

    /** Playback service for audio functionality */
    private PlaybackService playbackService;

    /** Flag to indicate application should exit */
    private boolean exitRequested = false;

    // ===============================
    // UI COMPONENTS
    // ===============================

    /** Authentication UI component for login/registration */
    private AuthenticationUI authUI;

    /** Music library UI component for browsing songs */
    private MusicLibraryUI libraryUI;

    /** Playlist manager UI component for playlist operations */
    private PlaylistManagerUI playlistUI;

    /** Player UI component for audio playback control */
    private PlayerUI playerUI;

    /** Social UI component for social features */
    private SocialUI socialUI;

    /**
     * Constructor - Initialize the user interface with server connection
     *
     * @param socket Socket connection to the server
     * @param in BufferedReader for server communication
     * @param out PrintWriter for server communication
     */
    public UserInterface(Socket socket, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.scanner = new Scanner(System.in);

        // Initialize all UI sub-components with dependency injection
        this.authUI = new AuthenticationUI(this, in, out, scanner);
        this.libraryUI = new MusicLibraryUI(this, in, out, scanner);
        this.playlistUI = new PlaylistManagerUI(this, in, out, scanner);
        this.playerUI = new PlayerUI(this, in, out, scanner);
        this.socialUI = new SocialUI(this, in, out, scanner, playerUI);

        // Display welcome banner on startup
        displayWelcomeBanner();
    }

    /**
     * Main application loop - Start the user interface
     * Handles the main application flow between authentication and main menu
     */
    public void start() {
        try {
            // Main application loop
            while (!exitRequested) {
                try {
                    // Start with authentication if not logged in
                    if (!isLoggedIn) {
                        authUI.showInitialMenu();

                        // Check if user requested to exit during authentication
                        if (exitRequested) {
                            break;
                        }
                    }

                    // Show main menu if logged in
                    if (isLoggedIn) {
                        showMainMenu();
                        // After logout, check if we should exit or continue
                        if (exitRequested) {
                            break;
                        }
                        continue; // Return to authentication screen
                    }
                } catch (IOException e) {
                    showConnectionError();
                    exitRequested = true;
                }
            }

            // Clean up resources before exit
            cleanup();
        } catch (IOException e) {
            showFatalError();
        }
    }

    /**
     * Display and handle the main menu
     * Central hub for accessing all application features
     *
     * @throws IOException If there's a communication error with the server
     */
    private void showMainMenu() throws IOException {
        boolean back = false;
        while (!back && isLoggedIn) {
            displayMainMenuOptions();
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    // Access music library features
                    libraryUI.manageMusicLibrary();
                    break;
                case "2":
                    // Access playlist management features
                    playlistUI.managePlaylists();
                    break;
                case "3":
                    // Start the audio player
                    playerUI.startPlayer();
                    break;
                case "4":
                    // Access social features
                    socialUI.manageSocialFeatures();
                    break;
                case "5":
                    // Logout and return to authentication
                    authUI.logout();
                    back = true; // Exit main menu loop
                    break;
                default:
                    showInvalidOption();
            }
        }
    }

    // ===============================
    // VISUAL DISPLAY METHODS
    // ===============================

    /**
     * Display the welcome banner with clean ASCII art
     * Creates an impressive first impression with minimal styling
     */
    private void displayWelcomeBanner() {
        clearScreen();
        System.out.println();
        System.out.println();
        System.out.println("    ███╗   ███╗██╗███╗   ██╗██╗    ███████╗██████╗  ██████╗ ████████╗██╗███████╗██╗   ██╗");
        System.out.println("    ████╗ ████║██║████╗  ██║██║    ██╔════╝██╔══██╗██╔═══██╗╚══██╔══╝██║██╔════╝╚██╗ ██╔╝");
        System.out.println("    ██╔████╔██║██║██╔██╗ ██║██║    ███████╗██████╔╝██║   ██║   ██║   ██║█████╗   ╚████╔╝ ");
        System.out.println("    ██║╚██╔╝██║██║██║╚██╗██║██║    ╚════██║██╔═══╝ ██║   ██║   ██║   ██║██╔══╝    ╚██╔╝  ");
        System.out.println("    ██║ ╚═╝ ██║██║██║ ╚████║██║    ███████║██║     ╚██████╔╝   ██║   ██║██║        ██║   ");
        System.out.println("    ╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝╚═╝    ╚══════╝╚═╝      ╚═════╝    ╚═╝   ╚═╝╚═╝        ╚═╝   ");
        System.out.println();
        System.out.println("                                      🎵 Version 1.0 🎵");
        System.out.println("                            Your Personal Music Streaming Service");
        System.out.println();
        System.out.println("    ================================================================================");
        System.out.println();
        showLoadingAnimation();
    }

    /**
     * Display a loading animation to enhance user experience
     * Provides visual feedback during application initialization
     */
    private void showLoadingAnimation() {
        System.out.print("    🎼 Initializing");
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(500);
                System.out.print(".");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(" ✅ Ready!");
        System.out.println();
    }

    /**
     * Display the main menu options with clean styling
     * Uses icons and clear descriptions for each feature
     */
    private void displayMainMenuOptions() {
        clearScreen();
        System.out.println();
        System.out.println("    🎧 Welcome back, " + (currentUser != null ? currentUser : "User") + "!");
        System.out.println();
        System.out.println("    ================================================================================");
        System.out.println();
        System.out.println("        1️⃣  📚 Music Library      → Browse and search your music collection");
        System.out.println();
        System.out.println("        2️⃣  📝 Playlist Manager   → Create, edit and organize your playlists");
        System.out.println();
        System.out.println("        3️⃣  ▶️  Audio Player       → Start listening to your favorite music");
        System.out.println();
        System.out.println("        4️⃣  👥 Social Features    → Follow friends and discover new music");
        System.out.println();
        System.out.println("        5️⃣  🚪 Logout             → Sign out and return to welcome screen");
        System.out.println();
        System.out.println("    ================================================================================");
        System.out.println();
        System.out.print("    🎯 Please select an option (1-5): ");
    }

    // ===============================
    // UTILITY AND DISPLAY HELPERS
    // ===============================

    /**
     * Clear the console screen (cross-platform compatible)
     * Supports both Windows and Unix-like systems
     */
    private void clearScreen() {
        try {
            // Windows system
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Unix/Linux/Mac systems
                System.out.print("\033[2J\033[H");
            }
        } catch (Exception e) {
            // Fallback: add blank lines if screen clearing fails
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * Display an invalid option error message
     * Provides clear feedback when user enters invalid input
     */
    private void showInvalidOption() {
        System.out.println();
        System.out.println("    ❌ INVALID OPTION");
        System.out.println();
        System.out.println("    Please select a number between 1-5");
        System.out.println();
        System.out.print("    ⏸️  Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Display connection error message
     * Shows when connection to server is lost
     */
    private void showConnectionError() {
        System.out.println();
        System.out.println("    🔌 CONNECTION LOST");
        System.out.println();
        System.out.println("    Lost connection to server. Please restart the application.");
        System.out.println();
    }

    /**
     * Display fatal error message
     * Shows when a critical error occurs that requires restart
     */
    private void showFatalError() {
        System.out.println();
        System.out.println("    ⚠️  FATAL ERROR");
        System.out.println();
        System.out.println("    A critical error occurred. Please restart the application.");
        System.out.println();
    }

    /**
     * Clean up resources before application exit
     * Ensures proper closure of connections and resources
     *
     * @throws IOException If there's an error closing resources
     */
    private void cleanup() throws IOException {
        scanner.close();
        if (!socket.isClosed()) {
            socket.close();
        }

        clearScreen();
        System.out.println();
        System.out.println();
        System.out.println("                            🎵 THANK YOU FOR USING MINI SPOTIFY 🎵");
        System.out.println();
        System.out.println("                                  See you next time! 👋");
        System.out.println();
        System.out.println("    ================================================================================");
        System.out.println();
    }

    // ===============================
    // GETTERS AND SETTERS
    // ===============================

    /**
     * Set the exit requested flag
     * Used by other components to request application termination
     *
     * @param exitRequested true if application should exit, false otherwise
     */
    public void setExitRequested(boolean exitRequested) {
        this.exitRequested = exitRequested;
    }

    /**
     * Check if application exit has been requested
     *
     * @return true if exit requested, false otherwise
     */
    public boolean isExitRequested() {
        return exitRequested;
    }

    /**
     * Set the logged in status
     *
     * @param loggedIn true if user is logged in, false otherwise
     */
    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    /**
     * Set the current user
     *
     * @param user Username of the current user
     */
    public void setCurrentUser(String user) {
        currentUser = user;
    }

    /**
     * Check if user is currently logged in
     *
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    /**
     * Get the current user's username
     *
     * @return Current user's username, or null if not logged in
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Get the playback service
     *
     * @return Current playback service instance
     */
    public PlaybackService getPlaybackService() {
        return playbackService;
    }

    /**
     * Set the playback service
     *
     * @param playbackService Playback service to set
     */
    public void setPlaybackService(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    /**
     * Get the player UI component
     *
     * @return Player UI component instance
     */
    public PlayerUI getPlayerUI() {
        return playerUI;
    }

    /**
     * Get the social UI component
     *
     * @return Social UI component instance
     */
    public SocialUI getSocialUI() {
        return socialUI;
    }
}