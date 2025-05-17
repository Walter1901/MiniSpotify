package client;

import client.commands.NextCommand;
import client.commands.PauseCommand;
import client.commands.PlayCommand;
import client.commands.PrevCommand;
import client.commands.StopCommand;
import playback.PlaybackService;
import server.music.Playlist;
import server.music.Song;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UserInterface {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private boolean isLoggedIn = false;
    private String currentUser;
    private PlaybackService playbackService;

    public UserInterface(Socket socket, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.scanner = new Scanner(System.in);

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

    public void start() {
        try {
            initialMenu();

            // Fermeture des ressources
            scanner.close();
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error in user interface: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initialMenu() throws IOException {
        System.out.println("1. Create an account");
        System.out.println("2. Log in");
        System.out.print("Choose an option: ");
        String option = scanner.nextLine().trim();
        if (option.equals("1")) {
            createAccount();
        } else if (option.equals("2")) {
            login();
        } else {
            System.out.println("Invalid option. Please try again.");
            initialMenu();
        }

        if (isLoggedIn) {
            mainMenu();
        }
    }

    private void createAccount() throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Account type (free/premium): ");
        String accountType = scanner.nextLine();

        out.println("CREATE " + username + " " + password + " " + accountType);

        String response = in.readLine();
        if (response != null && response.startsWith("CREATE_SUCCESS")) {
            System.out.println("Account created successfully!");
            currentUser = username;
            isLoggedIn = true;
        } else {
            System.out.println("Error: " + response);
            createAccount(); // retry
        }
    }

    private void login() throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        out.println("LOGIN " + username + " " + password);

        String response = in.readLine();
        if ("LOGIN_SUCCESS".equalsIgnoreCase(response)) {
            System.out.println("✅ Successfully authenticated.");
            currentUser = username;
            isLoggedIn = true;
        } else {
            System.out.println("❌ Authentication failed.");
            login();
        }
    }

    private void mainMenu() throws IOException {
        boolean exit = false;
        while (!exit) {
            System.out.print("==================================================================================");
            System.out.println("\nMain Menu :");
            System.out.println("1. View Music Library");
            System.out.println("2. Manage my playlists");
            System.out.println("3. Start the player (command: play, pause, stop, next, prev, exit)");
            System.out.println("4. Logout");
            System.out.println("==================================================================================");
            System.out.print("Choose an option: ");
            String option = scanner.nextLine().trim();
            switch (option) {
                case "1":
                    manageMusicLibrary();
                    break;
                case "2":
                    managePlaylists();
                    break;
                case "3":
                    startPlayer();
                    break;
                case "4":
                    logout();
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void manageMusicLibrary() throws IOException {
        boolean back = false;
        while (!back) {
            System.out.print("==================================================================================");
            System.out.println("\n--- Music Library ---");
            System.out.println("1. Show all songs");
            System.out.println("2. Search by title");
            System.out.println("3. Search by artist");
            System.out.println("4. Back");
            System.out.println("==================================================================================");
            System.out.print("Choose an option: ");
            String option = scanner.nextLine().trim();
            switch (option) {
                case "1":
                    displayAllSongs();
                    break;
                case "2":
                    searchByTitle();
                    break;
                case "3":
                    searchByArtist();
                    break;
                case "4":
                    back = true;
                    break;
                default:
                    System.out.print("==================================================================================");
                    System.out.println("Invalid option..");
                    System.out.println("==================================================================================");
            }
        }
    }

    private void displayAllSongs() throws IOException {
        System.out.print("==================================================================================");
        System.out.println("\n--- List of songs in the library ---");
        System.out.println("==================================================================================");

        out.println("GET_ALL_SONGS");

        String response;
        List<Song> songs = new ArrayList<>();
        while (!(response = in.readLine()).equals("END")) {
            System.out.println(response);
        }

        if (songs.isEmpty()) {
            System.out.print("==================================================================================");
            System.out.println("No songs have been added.");
            System.out.println("==================================================================================");
        }
    }

    private void searchByTitle() throws IOException {
        System.out.print("==================================================================================");
        System.out.print("Enter the title you wish to search for: ");
        String title = scanner.nextLine();
        System.out.println("==================================================================================");

        out.println("SEARCH_TITLE " + title);

        String response;
        List<String> results = new ArrayList<>();
        while (!(response = in.readLine()).equals("END")) {
            results.add(response);
            System.out.println(response);
        }

        if (results.isEmpty()) {
            System.out.print("==================================================================================");
            System.out.println("No songs found for this title.");
            System.out.println("==================================================================================");
        }
    }

    private void searchByArtist() throws IOException {
        System.out.print("==================================================================================");
        System.out.print("Enter the artist you wish to search for: ");
        String artist = scanner.nextLine();
        System.out.println("==================================================================================");

        out.println("SEARCH_ARTIST " + artist);

        String response;
        List<String> results = new ArrayList<>();
        while (!(response = in.readLine()).equals("END")) {
            results.add(response);
            System.out.println(response);
        }

        if (results.isEmpty()) {
            System.out.print("==================================================================================");
            System.out.println("No songs found for this artist.");
            System.out.println("==================================================================================");
        }
    }

    private void managePlaylists() throws IOException {
        boolean back = false;
        while (!back) {
            System.out.print("==================================================================================");
            System.out.println("\n--- Playlist management ---");
            System.out.println("1. Create a new playlist");
            System.out.println("2. Display my playlists");
            System.out.println("3. Add a song to a playlist");
            System.out.println("4. Delete a song from a playlist");
            System.out.println("5. Back");
            System.out.println("==================================================================================");
            System.out.print("Choose an option: ");
            String option = scanner.nextLine().trim();
            switch (option) {
                case "1":
                    createPlaylist();
                    break;
                case "2":
                    displayPlaylists();
                    break;
                case "3":
                    addSongToPlaylist();
                    break;
                case "4":
                    removeSongFromPlaylist();
                    break;
                case "5":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void createPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Enter the name of the new playlist: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine();

        out.println("CREATE_PLAYLIST " + playlistName); // <-- This line sends the command to the server

        String response = in.readLine();
        if (response.startsWith("PLAYLIST_CREATED")) {
            System.out.println("==================================================================================");
            System.out.println("Playlist created!");
            System.out.println("==================================================================================");
        } else {
            System.out.println("==================================================================================");
            System.out.println("Error: " + response);
            System.out.println("==================================================================================");
        }
    }
    private void displayPlaylists() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Your playlists:");
        System.out.println("==================================================================================");

        out.println("GET_PLAYLISTS");

        String response;
        boolean hasPlaylists = false;
        while (!(response = in.readLine()).equals("END")) {
            hasPlaylists = true;
            System.out.println(response);
        }

        if (!hasPlaylists) {
            System.out.println("==================================================================================");
            System.out.println("No playlist created.");
            System.out.println("==================================================================================");
        }
    }

    /**
     * Method to add a song to a playlist
     */
    private void addSongToPlaylist() {
        try {
            System.out.println("==================================================================================");
            System.out.println("Playlist name: ");
            System.out.println("==================================================================================");
            Scanner scanner = new Scanner(System.in);
            String playlistName = scanner.nextLine().trim();
            System.out.println("==================================================================================");

            // First, ensure we have the playlist from the server
            out.println("CHECK_PLAYLIST " + playlistName);
            // Read the response directly from the input stream
            String response = in.readLine();

            if ("PLAYLIST_NOT_FOUND".equals(response)) {
                System.out.println("Playlist not found.");
                System.out.println("==================================================================================");
                return;
            }

            // Get all available songs
            out.println("GET_ALL_SONGS");
            System.out.println("Available songs:");
            System.out.println("==================================================================================");
            String line;
            List<String> songs = new ArrayList<>();

            while (!(line = in.readLine()).equals("END")) {
                System.out.println(line);
                songs.add(line);
            }

            if (songs.isEmpty()) {
                System.out.println("No songs available.");
                System.out.println("==================================================================================");
                return;
            }

            System.out.println("==================================================================================");
            System.out.println("Enter song title: ");
            System.out.println("==================================================================================");
            String songTitle = scanner.nextLine().trim();
            System.out.println("==================================================================================");

            // Send the command with proper formatting
            out.println("ADD_SONG_TO_PLAYLIST " + playlistName + " " + songTitle);
            response = in.readLine();

            System.out.println(response);
            System.out.println("==================================================================================");
        } catch (IOException e) {
            System.err.println("Error while communicating with server: " + e.getMessage());
            System.out.println("==================================================================================");
        }
    }

    private void removeSongFromPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Playlist name: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine();

        out.println("GET_PLAYLIST " + playlistName);
        String response = in.readLine();

        if (response.startsWith("ERROR")) {
            System.out.println("==================================================================================");
            System.out.println("Playlist not found.");
            System.out.println("==================================================================================");
            return;
        }

        while (!(response = in.readLine()).equals("END")) {
            System.out.println(response);
        }

        System.out.println("==================================================================================");
        System.out.println("Enter the title of the song to be deleted: ");
        System.out.println("==================================================================================");
        String songTitle = scanner.nextLine();

        out.println("REMOVE_SONG_FROM_PLAYLIST " + playlistName + " " + songTitle);
        response = in.readLine();

        if (response.startsWith("SUCCESS")) {
            System.out.println("==================================================================================");
            System.out.println("Song deleted.");
            System.out.println("==================================================================================");
        } else {
            System.out.println("==================================================================================");
            System.out.println("Song not found in playlist.");
            System.out.println("==================================================================================");
        }
    }

    private void startPlayer() throws IOException {
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

        out.println("LOAD_PLAYLIST " + playlistName);
        response = in.readLine();

        if (response.startsWith("ERROR")) {
            System.out.println("Error loading playlist: " + response);
            return;
        }

        // Choix du mode de lecture
        choosePlaybackMode();

        String input;
        boolean exitPlayer = false;

        while (!exitPlayer) {
            System.out.println("==================================================================================");
            System.out.println("Enter a command (play, pause, stop, next, prev, exit): ");
            System.out.println("==================================================================================");
            input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "play":
                    out.println("PLAYER_PLAY");
                    System.out.println(in.readLine());
                    break;
                case "pause":
                    out.println("PLAYER_PAUSE");
                    System.out.println(in.readLine());
                    break;
                case "stop":
                    out.println("PLAYER_STOP");
                    System.out.println(in.readLine());
                    break;
                case "next":
                    out.println("PLAYER_NEXT");
                    System.out.println(in.readLine());
                    break;
                case "prev":
                    out.println("PLAYER_PREV");
                    System.out.println(in.readLine());
                    break;
                case "exit":
                    out.println("PLAYER_EXIT");
                    System.out.println("Return to main menu..");
                    exitPlayer = true;
                    break;
                default:
                    System.out.println("Unknown command.");
            }
        }
    }

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

    private void logout() {
        out.println("LOGOUT");
        isLoggedIn = false;
        currentUser = null;
        System.out.println("You have been logged out.");
    }
}