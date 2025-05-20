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
            System.out.println("2. Create collaborative playlist");  // Nouvelle option
            System.out.println("3. Display my playlists");
            System.out.println("4. Add a song to a playlist");
            System.out.println("5. Delete a song from a playlist");
            System.out.println("6. Reorder songs in a playlist");
            System.out.println("7. Back");
            System.out.println("==================================================================================");
            System.out.print("Choose an option: ");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    createPlaylist();
                    break;
                case "2":
                    createCollaborativePlaylist();
                    break;
                case "3":
                    displayPlaylists();
                    break;
                case "4":
                    addSongToPlaylist();
                    break;
                case "5":
                    removeSongFromPlaylist();
                    break;
                case "6":
                    reorderSongsInPlaylist();
                    break;
                case "7":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    /**
     * Crée une nouvelle playlist collaborative
     */
    private void createCollaborativePlaylist() throws IOException {
        // Vérifier d'abord si nous sommes connectés
        if (!mainUI.isLoggedIn()) {
            System.out.println("==================================================================================");
            System.out.println("You are not logged in. Please log in first.");
            System.out.println("==================================================================================");
            return;
        }

        System.out.println("==================================================================================");
        System.out.println("Enter the name of the new collaborative playlist: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine();

        System.out.println("==================================================================================");
        System.out.println("Enter usernames to add as collaborators (comma-separated): ");
        System.out.println("==================================================================================");
        String collaboratorsInput = scanner.nextLine();

        // Envoyer la commande
        out.println("CREATE_COLLAB_PLAYLIST " + playlistName + " " + collaboratorsInput);

        // Lire et traiter la réponse
        String response = in.readLine();

        // Vérifier si nous avons perdu la connexion
        if (response == null) {
            System.out.println("==================================================================================");
            System.out.println("Lost connection to server. Please restart the application.");
            System.out.println("==================================================================================");
            mainUI.setExitRequested(true);
            return;
        }

        // Traiter la réponse normale
        System.out.println("==================================================================================");
        System.out.println(response);

        // Si l'erreur indique que l'utilisateur n'est pas connecté, le reconnecter
        if (response.contains("No user logged in")) {
            System.out.println("Session expired. Please log in again.");
            mainUI.setLoggedIn(false);
        }

        System.out.println("==================================================================================");
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
        List<String> playlists = new ArrayList<>();
        while ((response = readLineFromServer()) != null && !response.equals("END")) {
            playlists.add(response);
        }

        // Si déconnexion, retourner immédiatement
        if (response == null) return;

        if (playlists.isEmpty()) {
            System.out.println("==================================================================================");
            System.out.println("No playlist created.");
            System.out.println("==================================================================================");
            return;
        }

        // Afficher chaque playlist avec ses chansons
        for (String playlistName : playlists) {
            System.out.println("==================================================================================");
            System.out.println("Playlist: " + playlistName);
            System.out.println("==================================================================================");

            // Récupérer les chansons de cette playlist
            out.println("GET_PLAYLIST_SONGS " + playlistName);
            response = in.readLine();

            if (!response.startsWith("SUCCESS")) {
                System.out.println("Error retrieving songs: " + response);
                continue;
            }

            List<String> songs = new ArrayList<>();
            while (!(response = in.readLine()).equals("END")) {
                songs.add(response);
            }

            // Afficher les chansons
            if (songs.isEmpty()) {
                System.out.println("This playlist is empty.");
            } else {
                System.out.println("Songs:");
                int index = 0;
                for (String song : songs) {
                    String[] parts = song.split("\\|");
                    if (parts.length > 0) {
                        String title = parts[0];
                        String artist = parts.length > 1 ? parts[1] : "Unknown";
                        System.out.println(index + ": " + title + " by " + artist);
                        index++;
                    }
                }
            }
        }
        System.out.println("==================================================================================");
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

            // Lire la réponse et vérifier si elle est null (connexion fermée)
            String response = in.readLine();
            if (response == null) {
                System.out.println("==================================================================================");
                System.out.println("Lost connection to server. Please restart the application.");
                System.out.println("==================================================================================");
                mainUI.setExitRequested(true); // Demander à quitter l'application
                return;
            }

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

            // Utiliser une boucle plus robuste qui vérifie si line est null
            while ((line = in.readLine()) != null && !line.equals("END")) {
                System.out.println(line);
                songs.add(line);
            }

            // Vérifier à nouveau si nous avons perdu la connexion
            if (line == null) {
                System.out.println("==================================================================================");
                System.out.println("Lost connection to server. Please restart the application.");
                System.out.println("==================================================================================");
                mainUI.setExitRequested(true);
                return;
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

            // Encore une vérification pour null
            response = in.readLine();
            if (response == null) {
                System.out.println("==================================================================================");
                System.out.println("Lost connection to server. Please restart the application.");
                System.out.println("==================================================================================");
                mainUI.setExitRequested(true);
                return;
            }

            System.out.println(response);
            System.out.println("==================================================================================");
        } catch (IOException e) {
            System.err.println("Error while communicating with server: " + e.getMessage());
            System.out.println("==================================================================================");
            // En cas d'erreur de communication, suggérer de redémarrer l'application
            mainUI.setExitRequested(true);
        }
    }

    /**
     * Version améliorée pour supprimer une chanson d'une playlist
     * Cette méthode affiche d'abord les chansons avec des indices pour faciliter la sélection
     */
    private void removeSongFromPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Playlist name: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine().trim();

        // Vérifier d'abord si la playlist existe
        out.println("CHECK_PLAYLIST " + playlistName);
        String response = in.readLine();

        if ("PLAYLIST_NOT_FOUND".equals(response)) {
            System.out.println("==================================================================================");
            System.out.println("Playlist not found.");
            System.out.println("==================================================================================");
            return;
        }

        // Récupérer les chansons de la playlist
        out.println("GET_PLAYLIST_SONGS " + playlistName);
        response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("==================================================================================");
            System.out.println(response);
            System.out.println("==================================================================================");
            return;
        }

        // Stocker les chansons pour pouvoir les référencer par index
        List<String> songTitles = new ArrayList<>();
        System.out.println("==================================================================================");
        System.out.println("Songs in playlist " + playlistName + ":");
        System.out.println("==================================================================================");

        while (!(response = in.readLine()).equals("END")) {
            // Format attendu: titre|artiste|album|genre|durée|chemin
            String[] parts = response.split("\\|");
            if (parts.length > 0) {
                String title = parts[0];
                songTitles.add(title);
                System.out.println((songTitles.size() - 1) + ": " + response);
            }
        }

        if (songTitles.isEmpty()) {
            System.out.println("==================================================================================");
            System.out.println("No songs in this playlist.");
            System.out.println("==================================================================================");
            return;
        }

        System.out.println("==================================================================================");
        System.out.println("Enter the index of the song to be deleted: ");
        System.out.println("==================================================================================");

        int songIndex;
        try {
            songIndex = Integer.parseInt(scanner.nextLine().trim());
            if (songIndex < 0 || songIndex >= songTitles.size()) {
                System.out.println("==================================================================================");
                System.out.println("Invalid song index.");
                System.out.println("==================================================================================");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("==================================================================================");
            System.out.println("Please enter a valid number.");
            System.out.println("==================================================================================");
            return;
        }

        // Obtenir le titre de la chanson à l'index spécifié
        String songTitle = songTitles.get(songIndex);

        // Envoyer la commande au serveur
        out.println("REMOVE_SONG_FROM_PLAYLIST " + playlistName + " " + songTitle);
        response = in.readLine();

        System.out.println("==================================================================================");
        System.out.println(response);
        System.out.println("==================================================================================");
    }

    private void reorderSongsInPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Enter the name of the playlist: ");
        String playlistName = scanner.nextLine().trim();
        System.out.println("==================================================================================");

        // Vérifier d'abord si la playlist existe
        out.println("CHECK_PLAYLIST " + playlistName);
        String response = in.readLine();

        if ("PLAYLIST_NOT_FOUND".equals(response)) {
            System.out.println("==================================================================================");
            System.out.println("Playlist not found.");
            System.out.println("==================================================================================");
            return;
        }

        // Récupérer les chansons de la playlist
        out.println("GET_PLAYLIST_SONGS " + playlistName);
        response = in.readLine();

        if (!response.startsWith("SUCCESS")) {
            System.out.println("==================================================================================");
            System.out.println(response);
            System.out.println("==================================================================================");
            return;
        }

        // Stocker les chansons pour pouvoir les référencer par index
        List<String> songTitles = new ArrayList<>();
        System.out.println("==================================================================================");
        System.out.println("Songs in playlist " + playlistName + ":");
        System.out.println("==================================================================================");

        while (!(response = in.readLine()).equals("END")) {
            // Format attendu: titre|artiste|album|genre|durée|chemin
            String[] parts = response.split("\\|");
            if (parts.length > 0) {
                String title = parts[0];
                songTitles.add(title);
                String artist = parts.length > 1 ? parts[1] : "Unknown";
                System.out.println((songTitles.size() - 1) + ": " + title + " by " + artist);
            }
        }

        if (songTitles.isEmpty()) {
            System.out.println("==================================================================================");
            System.out.println("No songs in this playlist.");
            System.out.println("==================================================================================");
            return;
        }

        // Demander quelle chanson déplacer
        System.out.println("==================================================================================");
        System.out.println("Enter the index of the song to move: ");
        int fromIndex;
        try {
            fromIndex = Integer.parseInt(scanner.nextLine().trim());
            if (fromIndex < 0 || fromIndex >= songTitles.size()) {
                System.out.println("Invalid index.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return;
        }

        // Demander à quelle position la déplacer
        System.out.println("Enter the new position for the song (0 to " + (songTitles.size() - 1) + "): ");
        int toIndex;
        try {
            toIndex = Integer.parseInt(scanner.nextLine().trim());
            if (toIndex < 0 || toIndex >= songTitles.size()) {
                System.out.println("Invalid position.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return;
        }

        // Envoyer la commande au serveur
        out.println("REORDER_PLAYLIST_SONG " + playlistName + " " + fromIndex + " " + toIndex);
        response = in.readLine();

        System.out.println("==================================================================================");
        if (response.startsWith("SUCCESS")) {
            System.out.println("Song successfully moved.");
        } else {
            System.out.println("Error: " + response);
        }
        System.out.println("==================================================================================");
    }

    /**
     * Lit une ligne du serveur et gère la déconnexion
     * @return la ligne lue ou null en cas de déconnexion
     */
    private String readLineFromServer() throws IOException {
        String line = in.readLine();
        if (line == null) {
            System.out.println("==================================================================================");
            System.out.println("Lost connection to server. Please restart the application.");
            System.out.println("==================================================================================");
            mainUI.setExitRequested(true);
        }
        return line;
    }
}