package client.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * User interface for playlist management
 */
public class PlaylistManagerUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    /**
     * Constructor
     */
    public PlaylistManagerUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
    }

    /**
     * Manages playlists
     */
    public void managePlaylists() throws IOException {
        boolean back = false;
        while (!back) {
            displayMenuOptions();
            String option = scanner.nextLine().trim();

            try {
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
                        deletePlaylist();
                        break;
                    case "8":
                        manageCollaborators();
                        break;
                    case "9":
                        back = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("==================================================================================");
                System.out.println("Error processing command: " + e.getMessage());
                System.out.println("==================================================================================");

                // If connection is completely lost, exit
                if (e instanceof IOException && e.getMessage() != null &&
                        (e.getMessage().contains("Connection reset") ||
                                e.getMessage().contains("Broken pipe") ||
                                e.getMessage().contains("Socket closed"))) {
                    handleConnectionLoss("Connection error: " + e.getMessage());
                    break;
                }
            }
        }
    }

    /**
     * Display menu options
     */
    private void displayMenuOptions() {
        System.out.println("==================================================================================");
        System.out.println("\n--- Playlist management ---");
        System.out.println("1. Create a new playlist");
        System.out.println("2. Create collaborative playlist");
        System.out.println("3. Display my playlists");
        System.out.println("4. Add a song to a playlist");
        System.out.println("5. Delete a song from a playlist");
        System.out.println("6. Reorder songs in a playlist");
        System.out.println("7. Delete a playlist");
        System.out.println("8. Manage collaborators");
        System.out.println("9. Back");
        System.out.println("==================================================================================");
        System.out.print("Choose an option: ");
    }

    /**
     * Creates a new collaborative playlist
     */
    private void createCollaborativePlaylist() throws IOException {
        // First check if we're logged in
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

        try {
            System.out.println("==================================================================================");
            System.out.println("Creating collaborative playlist '" + playlistName + "' with collaborators: " + collaboratorsInput);
            System.out.println("==================================================================================");

            // Send command
            out.println("CREATE_COLLAB_PLAYLIST " + playlistName + " " + collaboratorsInput);
            out.flush(); // Ensure the command is sent immediately

            System.out.println("Command sent to server, waiting for response...");

            // Read and process response with timeout handling
            String response = safeReadLine();
            if (response == null) {
                handleConnectionLoss("No response from server");
                return;
            }

            System.out.println("==================================================================================");
            if (response.startsWith("COLLAB_PLAYLIST_CREATED")) {
                System.out.println("✅ Collaborative playlist created successfully!");

                // Check if there were warnings (users not found)
                if (response.contains("WARNINGS") || response.contains("not found")) {
                    System.out.println("⚠️ Note: " + response.substring(response.indexOf(":") + 1).trim());
                }
            } else if (response.contains("FAIL")) {
                System.out.println("❌ Failed to create playlist: " + response);
            } else {
                System.out.println("Server response: " + response);
            }

            // If error indicates user is not logged in, reconnect
            if (response.contains("No user logged in")) {
                System.out.println("Session expired. Please log in again.");
                mainUI.setLoggedIn(false);
            }
            System.out.println("==================================================================================");

        } catch (Exception e) {
            System.out.println("==================================================================================");
            System.out.println("Error during collaborative playlist creation: " + e.getMessage());
            System.out.println("==================================================================================");
            throw e;
        }
    }

    /**
     * Creates a new playlist
     */
    private void createPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Enter the name of the new playlist: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine();

        try {
            out.println("CREATE_PLAYLIST " + playlistName);
            String response = safeReadLine();

            if (response == null) {
                handleConnectionLoss("No response when creating playlist");
                return;
            }

            System.out.println("==================================================================================");
            if (response.startsWith("PLAYLIST_CREATED")) {
                System.out.println("✅ Playlist created!");
            } else {
                System.out.println("Error: " + response);
            }
            System.out.println("==================================================================================");
        } catch (Exception e) {
            System.out.println("==================================================================================");
            System.out.println("Error creating playlist: " + e.getMessage());
            System.out.println("==================================================================================");
            throw e; // Rethrow to handle at higher level if needed
        }
    }

    /**
     * Displays playlists
     */
    private void displayPlaylists() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Your playlists:");
        System.out.println("==================================================================================");

        try {
            out.println("GET_PLAYLISTS");

            String response;
            List<String> playlists = new ArrayList<>();

            while ((response = safeReadLine()) != null && !response.equals("END")) {
                playlists.add(response);
            }

            // If response is null, connection was lost
            if (response == null) {
                handleConnectionLoss("Lost connection while retrieving playlists");
                return;
            }

            if (playlists.isEmpty()) {
                System.out.println("==================================================================================");
                System.out.println("No playlist created.");
                System.out.println("==================================================================================");
                return;
            }

            // Display each playlist with its songs
            for (String playlistName : playlists) {
                System.out.println("==================================================================================");
                System.out.println("Playlist: " + playlistName);
                System.out.println("==================================================================================");

                // Get songs for this playlist
                out.println("GET_PLAYLIST_SONGS " + playlistName);
                response = safeReadLine();

                if (response == null) {
                    handleConnectionLoss("Lost connection while retrieving playlist songs");
                    return;
                }

                if (!response.startsWith("SUCCESS")) {
                    System.out.println("Error retrieving songs: " + response);
                    continue;
                }

                List<String> songs = new ArrayList<>();
                while (!(response = safeReadLine()).equals("END")) {
                    if (response == null) {
                        handleConnectionLoss("Lost connection while retrieving playlist songs");
                        return;
                    }
                    songs.add(response);
                }

                // Display songs
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
        } catch (Exception e) {
            System.out.println("==================================================================================");
            System.out.println("Error displaying playlists: " + e.getMessage());
            System.out.println("==================================================================================");
            throw e;
        }
    }

    /**
     * Adds a song to a playlist
     */
    private void addSongToPlaylist() throws IOException {
        try {
            System.out.println("==================================================================================");
            System.out.println("Playlist name: ");
            System.out.println("==================================================================================");
            String playlistName = scanner.nextLine().trim();
            System.out.println("==================================================================================");

            // First, ensure we have the playlist from the server
            out.println("CHECK_PLAYLIST " + playlistName);

            // Read response and check if it's null (closed connection)
            String response = safeReadLine();
            if (response == null) {
                handleConnectionLoss("No response when checking playlist");
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

            // Use a more robust loop that checks if line is null
            while ((line = safeReadLine()) != null && !line.equals("END")) {
                System.out.println(line);
                songs.add(line);
            }

            // Check again if we lost connection
            if (line == null) {
                handleConnectionLoss("No end marker received when getting songs");
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

            // One more null check
            response = safeReadLine();
            if (response == null) {
                handleConnectionLoss("No response when adding song to playlist");
                return;
            }

            System.out.println(response);
            System.out.println("==================================================================================");
        } catch (Exception e) {
            System.err.println("Error while communicating with server: " + e.getMessage());
            System.out.println("==================================================================================");
            throw e;
        }
    }

    /**
     * Improved version to remove a song from a playlist
     * This method first displays songs with indices for easier selection
     */
    private void removeSongFromPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Playlist name: ");
        System.out.println("==================================================================================");
        String playlistName = scanner.nextLine().trim();

        try {
            // First check if playlist exists
            out.println("CHECK_PLAYLIST " + playlistName);
            String response = safeReadLine();

            if (response == null) {
                handleConnectionLoss("No response when checking playlist");
                return;
            }

            if ("PLAYLIST_NOT_FOUND".equals(response)) {
                System.out.println("==================================================================================");
                System.out.println("Playlist not found.");
                System.out.println("==================================================================================");
                return;
            }

            // Get songs from playlist
            out.println("GET_PLAYLIST_SONGS " + playlistName);
            response = safeReadLine();

            if (response == null) {
                handleConnectionLoss("No response when getting playlist songs");
                return;
            }

            if (!response.startsWith("SUCCESS")) {
                System.out.println("==================================================================================");
                System.out.println(response);
                System.out.println("==================================================================================");
                return;
            }

            // Store songs for reference by index
            List<String> songTitles = new ArrayList<>();
            System.out.println("==================================================================================");
            System.out.println("Songs in playlist " + playlistName + ":");
            System.out.println("==================================================================================");

            while (!(response = safeReadLine()).equals("END")) {
                if (response == null) {
                    handleConnectionLoss("No end marker received when getting playlist songs");
                    return;
                }

                // Expected format: title|artist|album|genre|duration|path
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

            // Get the title of the song at specified index
            String songTitle = songTitles.get(songIndex);

            // Send command to server
            out.println("REMOVE_SONG_FROM_PLAYLIST " + playlistName + " " + songTitle);
            response = safeReadLine();

            if (response == null) {
                handleConnectionLoss("No response when removing song from playlist");
                return;
            }

            System.out.println("==================================================================================");
            System.out.println(response);
            System.out.println("==================================================================================");
        } catch (Exception e) {
            System.out.println("==================================================================================");
            System.out.println("Error removing song: " + e.getMessage());
            System.out.println("==================================================================================");
            throw e;
        }
    }

    /**
     * Reorders songs in a playlist
     */
    private void reorderSongsInPlaylist() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Enter the name of the playlist: ");
        String playlistName = scanner.nextLine().trim();
        System.out.println("==================================================================================");

        try {
            // First check if playlist exists
            out.println("CHECK_PLAYLIST " + playlistName);
            String response = safeReadLine();

            if (response == null) {
                handleConnectionLoss("No response when checking playlist");
                return;
            }

            if ("PLAYLIST_NOT_FOUND".equals(response)) {
                System.out.println("==================================================================================");
                System.out.println("Playlist not found.");
                System.out.println("==================================================================================");
                return;
            }

            // Get songs from playlist
            out.println("GET_PLAYLIST_SONGS " + playlistName);
            response = safeReadLine();

            if (response == null) {
                handleConnectionLoss("No response when getting playlist songs");
                return;
            }

            if (!response.startsWith("SUCCESS")) {
                System.out.println("==================================================================================");
                System.out.println(response);
                System.out.println("==================================================================================");
                return;
            }

            // Store songs for reference by index
            List<String> songTitles = new ArrayList<>();
            System.out.println("==================================================================================");
            System.out.println("Songs in playlist " + playlistName + ":");
            System.out.println("==================================================================================");

            while (!(response = safeReadLine()).equals("END")) {
                if (response == null) {
                    handleConnectionLoss("No end marker received when getting playlist songs");
                    return;
                }

                // Expected format: title|artist|album|genre|duration|path
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

            // Ask which song to move
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

            // Ask to which position to move it
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

            // Send command to server
            out.println("REORDER_PLAYLIST_SONG " + playlistName + " " + fromIndex + " " + toIndex);
            response = safeReadLine();

            if (response == null) {
                handleConnectionLoss("No response when reordering playlist");
                return;
            }

            System.out.println("==================================================================================");
            if (response.startsWith("SUCCESS")) {
                System.out.println("Song successfully moved.");
            } else {
                System.out.println("Error: " + response);
            }
            System.out.println("==================================================================================");
        } catch (Exception e) {
            System.out.println("==================================================================================");
            System.out.println("Error reordering songs: " + e.getMessage());
            System.out.println("==================================================================================");
            throw e;
        }
    }

    /**
     * Safely reads a line from the server with better error handling
     */
    private String safeReadLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            System.err.println("Error reading from server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads a line from server and handles disconnection
     */
    private String readLineFromServer() throws IOException {
        String line = safeReadLine();
        if (line == null) {
            handleConnectionLoss("Connection lost while reading from server");
        }
        return line;
    }

    /**
     * Handles a connection loss with detailed message
     */
    private void handleConnectionLoss(String reason) {
        System.out.println("==================================================================================");
        System.out.println("Lost connection to server: " + reason);
        System.out.println("Please restart the application.");
        System.out.println("==================================================================================");
        mainUI.setExitRequested(true);
    }

    private void deletePlaylist() throws IOException {
        try {
            System.out.println("==================================================================================");
            System.out.println("Your playlists:");

            // Récupérer la liste des playlists
            out.println("GET_PLAYLISTS");

            String response;
            List<String> playlists = new ArrayList<>();

            while ((response = safeReadLine()) != null && !response.equals("END")) {
                playlists.add(response);
                System.out.println("- " + response);
            }

            if (response == null) {
                handleConnectionLoss("Lost connection while retrieving playlists");
                return;
            }

            if (playlists.isEmpty()) {
                System.out.println("==================================================================================");
                System.out.println("No playlists to delete.");
                System.out.println("==================================================================================");
                return;
            }

            // Demander quelle playlist supprimer
            System.out.println("==================================================================================");
            System.out.println("Enter the name of the playlist to delete: ");
            String playlistName = scanner.nextLine().trim();

            // Confirmation pour éviter les suppressions accidentelles
            System.out.println("==================================================================================");
            System.out.println("Are you sure you want to delete the playlist '" + playlistName + "'? (y/n): ");
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if (confirmation.equals("y")) {
                // Envoyer la commande de suppression
                out.println("DELETE_PLAYLIST " + playlistName);
                response = safeReadLine();

                if (response == null) {
                    handleConnectionLoss("No response when deleting playlist");
                    return;
                }

                System.out.println("==================================================================================");
                System.out.println(response);
                System.out.println("==================================================================================");
            } else {
                System.out.println("==================================================================================");
                System.out.println("Playlist deletion cancelled.");
                System.out.println("==================================================================================");
            }
        } catch (Exception e) {
            System.out.println("==================================================================================");
            System.out.println("Error deleting playlist: " + e.getMessage());
            System.out.println("==================================================================================");
            throw e;
        }
    }
    /**
     * Manage collaborators for collaborative playlists
     */
    private void manageCollaborators() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("\n--- Collaborator Management ---");
        System.out.println("==================================================================================");

        // Get user's collaborative playlists
        out.println("GET_PLAYLISTS");
        String response;
        List<String> allPlaylists = new ArrayList<>();

        while ((response = safeReadLine()) != null && !response.equals("END")) {
            allPlaylists.add(response);
        }

        if (response == null) {
            handleConnectionLoss("Lost connection while retrieving playlists");
            return;
        }

        // Filter collaborative playlists (we need to check each one)
        List<String> collaborativePlaylists = new ArrayList<>();
        for (String playlistName : allPlaylists) {
            // For now, let user select and we'll check if it's collaborative
            collaborativePlaylists.add(playlistName);
        }

        if (collaborativePlaylists.isEmpty()) {
            System.out.println("==================================================================================");
            System.out.println("No playlists found.");
            System.out.println("==================================================================================");
            return;
        }

        System.out.println("Available playlists:");
        for (int i = 0; i < collaborativePlaylists.size(); i++) {
            System.out.println((i + 1) + ". " + collaborativePlaylists.get(i));
        }

        System.out.println("==================================================================================");
        System.out.print("Select playlist number (or 0 to cancel): ");
        String input = scanner.nextLine().trim();

        try {
            int choice = Integer.parseInt(input);
            if (choice == 0) return;

            if (choice < 1 || choice > collaborativePlaylists.size()) {
                System.out.println("Invalid choice.");
                return;
            }

            String selectedPlaylist = collaborativePlaylists.get(choice - 1);
            managePlaylistCollaborators(selectedPlaylist);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    /**
     * Manage collaborators for a specific playlist
     */
    private void managePlaylistCollaborators(String playlistName) throws IOException {
        while (true) {
            System.out.println("==================================================================================");
            System.out.println("Managing collaborators for: " + playlistName);
            System.out.println("==================================================================================");
            System.out.println("1. List current collaborators");
            System.out.println("2. Add collaborator");
            System.out.println("3. Remove collaborator");
            System.out.println("4. Back");
            System.out.println("==================================================================================");
            System.out.print("Choose an option: ");

            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    listCollaborators(playlistName);
                    break;
                case "2":
                    addCollaborator(playlistName);
                    break;
                case "3":
                    removeCollaborator(playlistName);
                    break;
                case "4":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    /**
     * List collaborators for a playlist
     */
    private void listCollaborators(String playlistName) throws IOException {
        System.out.println("==================================================================================");
        out.println("LIST_COLLABORATORS " + playlistName);

        String response;
        while ((response = safeReadLine()) != null && !response.equals("END")) {
            System.out.println(response);
        }

        if (response == null) {
            handleConnectionLoss("Lost connection while listing collaborators");
            return;
        }
        System.out.println("==================================================================================");
    }

    /**
     * Add a collaborator to a playlist
     */
    private void addCollaborator(String playlistName) throws IOException {
        System.out.println("==================================================================================");
        System.out.print("Enter username to add as collaborator: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }

        out.println("ADD_COLLABORATOR " + playlistName + " " + username);
        String response = safeReadLine();

        if (response == null) {
            handleConnectionLoss("No response when adding collaborator");
            return;
        }

        System.out.println("==================================================================================");
        System.out.println(response);
        System.out.println("==================================================================================");
    }

    /**
     * Remove a collaborator from a playlist
     */
    private void removeCollaborator(String playlistName) throws IOException {
        System.out.println("==================================================================================");

        // First list current collaborators
        System.out.println("Current collaborators:");
        out.println("LIST_COLLABORATORS " + playlistName);

        String response;
        List<String> collaborators = new ArrayList<>();

        while ((response = safeReadLine()) != null && !response.equals("END")) {
            if (response.startsWith("Collaborator: ")) {
                String collaborator = response.substring("Collaborator: ".length());
                collaborators.add(collaborator);
                System.out.println((collaborators.size()) + ". " + collaborator);
            } else {
                System.out.println(response);
            }
        }

        if (response == null) {
            handleConnectionLoss("Lost connection while listing collaborators");
            return;
        }

        if (collaborators.isEmpty()) {
            System.out.println("No collaborators to remove.");
            System.out.println("==================================================================================");
            return;
        }

        System.out.println("==================================================================================");
        System.out.print("Enter username to remove: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }

        out.println("REMOVE_COLLABORATOR " + playlistName + " " + username);
        response = safeReadLine();

        if (response == null) {
            handleConnectionLoss("No response when removing collaborator");
            return;
        }

        System.out.println("==================================================================================");
        System.out.println(response);
        System.out.println("==================================================================================");
    }

}