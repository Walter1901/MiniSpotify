package client.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interface utilisateur pour la gestion des playlists
 */
public class PlaylistManagerUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    /**
     * Constructeur
     */
    public PlaylistManagerUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
    }

    /**
     * Gère les playlists
     */
    public void managePlaylists() throws IOException {
        boolean back = false;
        while (!back) {
            System.out.println("==================================================================================");
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

    /**
     * Crée une nouvelle playlist
     */
    private void createPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Enter the name of the new playlist: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine();

        out.println("CREATE_PLAYLIST " + playlistName);

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

    /**
     * Affiche les playlists
     */
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
     * Ajoute une chanson à une playlist
     */
    private void addSongToPlaylist() {
        try {
            System.out.println("==================================================================================");
            System.out.println("Playlist name: ");
            System.out.println("==================================================================================");
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

    /**
     * Supprime une chanson d'une playlist
     */
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
}