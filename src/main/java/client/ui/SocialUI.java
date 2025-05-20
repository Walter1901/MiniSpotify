package client.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interface utilisateur pour les fonctionnalités sociales
 */
public class SocialUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private PlayerUI playerUI;

    /**
     * Constructeur
     */
    public SocialUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner, PlayerUI playerUI) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
        this.playerUI = playerUI;
    }

    /**
     * Gère les fonctionnalités sociales
     */
    public void manageSocialFeatures() throws IOException {
        boolean back = false;
        while (!back) {
            System.out.println("==================================================================================");
            System.out.println("\n--- Social Features ---");
            System.out.println("1. Follow a user");
            System.out.println("2. View followed users");
            System.out.println("3. View shared playlists");
            System.out.println("4. Set playlist sharing preferences");
            System.out.println("5. Back");
            System.out.println("==================================================================================");
            System.out.print("Choose an option: ");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    followUser();
                    break;
                case "2":
                    viewFollowedUsers();
                    break;
                case "3":
                    viewSharedPlaylists();
                    break;
                case "4":
                    setSharingPreferences();
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
     * Suit un nouvel utilisateur
     */
    private void followUser() throws IOException {
        System.out.println("==================================================================================");
        System.out.print("Enter username to follow: ");
        String username = scanner.nextLine().trim();
        System.out.println("==================================================================================");

        out.println("FOLLOW_USER " + username);
        String response = in.readLine();

        System.out.println("==================================================================================");
        System.out.println(response);
        System.out.println("==================================================================================");
    }

    /**
     * Affiche la liste des utilisateurs suivis
     */
    private void viewFollowedUsers() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Users you are following:");
        System.out.println("==================================================================================");

        // Envoyer une commande au serveur pour obtenir la liste des utilisateurs suivis
        out.println("GET_FOLLOWED_USERS");

        // Lire les réponses du serveur
        String response;
        List<String> followedUsers = new ArrayList<>();
        while (!(response = in.readLine()).equals("END")) {
            followedUsers.add(response);
        }

        // Afficher les utilisateurs suivis
        if (followedUsers.isEmpty()) {
            System.out.println("You are not following any users.");
        } else {
            int index = 1;
            for (String username : followedUsers) {
                System.out.println(index + ". " + username);
                index++;
            }
        }
        System.out.println("==================================================================================");

        // Proposer des actions sur ces utilisateurs
        if (!followedUsers.isEmpty()) {
            System.out.println("Do you want to unfollow a user? (y/n)");
            String option = scanner.nextLine().trim().toLowerCase();

            if (option.equals("y")) {
                System.out.print("Enter the number of the user to unfollow: ");
                try {
                    int userIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    if (userIndex >= 0 && userIndex < followedUsers.size()) {
                        String unfollowUsername = followedUsers.get(userIndex);
                        out.println("UNFOLLOW_USER " + unfollowUsername);
                        response = in.readLine();
                        System.out.println("==================================================================================");
                        System.out.println(response);
                        System.out.println("==================================================================================");
                    } else {
                        System.out.println("Invalid user number.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number.");
                }
            }
        }
    }

    /**
     * Affiche les playlists partagées par les utilisateurs suivis
     */
    private void viewSharedPlaylists() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Shared playlists from users you follow:");
        System.out.println("==================================================================================");

        // Envoyer une commande au serveur pour obtenir les playlists partagées
        out.println("GET_SHARED_PLAYLISTS");

        String response;
        Map<Integer, String[]> playlists = new HashMap<>(); // [playlistName, ownerUsername]
        int index = 1;

        // Format de réponse attendu: "PlaylistName|OwnerUsername"
        while (!(response = in.readLine()).equals("END")) {
            String[] parts = response.split("\\|");
            if (parts.length >= 2) {
                String playlistName = parts[0];
                String ownerUsername = parts[1];
                playlists.put(index, new String[]{playlistName, ownerUsername});
                System.out.println(index + ". " + playlistName + " (shared by " + ownerUsername + ")");
                index++;
            }
        }

        if (playlists.isEmpty()) {
            System.out.println("No shared playlists available.");
            System.out.println("==================================================================================");
            return;
        }

        // Menu d'actions pour les playlists partagées
        System.out.println("==================================================================================");
        System.out.println("Options:");
        System.out.println("1. View songs in a shared playlist");
        System.out.println("2. Copy a shared playlist to your library");
        System.out.println("3. Play a shared playlist");
        System.out.println("4. Back");
        System.out.println("==================================================================================");
        System.out.print("Choose an option: ");

        String option = scanner.nextLine().trim();
        switch (option) {
            case "1":
                viewSharedPlaylistSongs(playlists);
                break;
            case "2":
                copySharedPlaylist(playlists);
                break;
            case "3":
                playSharedPlaylist(playlists);
                break;
            case "4":
                return;
            default:
                System.out.println("Invalid option.");
        }
    }

    /**
     * Définit les préférences de partage de playlists
     */
    private void setSharingPreferences() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Playlist sharing preferences:");
        System.out.println("1. Share all playlists publicly");
        System.out.println("2. Keep playlists private");
        System.out.println("==================================================================================");
        System.out.print("Choose an option: ");

        String option = scanner.nextLine().trim();
        boolean sharePublicly = option.equals("1");

        out.println("SET_PLAYLIST_SHARING " + sharePublicly);
        String response = in.readLine();

        System.out.println("==================================================================================");
        System.out.println(response);
        System.out.println("==================================================================================");
    }

    /**
     * Affiche les chansons d'une playlist partagée
     */
    private void viewSharedPlaylistSongs(Map<Integer, String[]> playlists) throws IOException {
        System.out.println("==================================================================================");
        System.out.print("Enter the number of the playlist to view: ");

        try {
            int playlistIndex = Integer.parseInt(scanner.nextLine().trim());
            String[] playlist = playlists.get(playlistIndex);

            if (playlist == null) {
                System.out.println("Invalid playlist number.");
                return;
            }

            String playlistName = playlist[0];
            String ownerUsername = playlist[1];

            System.out.println("==================================================================================");
            System.out.println("Songs in '" + playlistName + "' (shared by " + ownerUsername + "):");
            System.out.println("==================================================================================");

            out.println("GET_SHARED_PLAYLIST_SONGS " + ownerUsername + " " + playlistName);

            String response;
            if (!(response = in.readLine()).startsWith("SUCCESS")) {
                System.out.println("Error: " + response);
                return;
            }

            int songIndex = 1;
            while (!(response = in.readLine()).equals("END")) {
                // Format attendu: titre|artiste|album|genre|durée
                String[] songData = response.split("\\|");
                if (songData.length > 0) {
                    String title = songData[0];
                    String artist = songData.length > 1 ? songData[1] : "Unknown";
                    String album = songData.length > 2 ? songData[2] : "";
                    String genre = songData.length > 3 ? songData[3] : "";

                    System.out.println(songIndex + ". " + title + " by " + artist +
                            (album.isEmpty() ? "" : " (" + album + ")") +
                            (genre.isEmpty() ? "" : " - " + genre));
                    songIndex++;
                }
            }

            if (songIndex == 1) {
                System.out.println("This playlist is empty.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
        System.out.println("==================================================================================");
    }

    /**
     * Copie une playlist partagée dans la bibliothèque de l'utilisateur
     */
    private void copySharedPlaylist(Map<Integer, String[]> playlists) throws IOException {
        System.out.println("==================================================================================");
        System.out.print("Enter the number of the playlist to copy: ");

        try {
            int playlistIndex = Integer.parseInt(scanner.nextLine().trim());
            String[] playlist = playlists.get(playlistIndex);

            if (playlist == null) {
                System.out.println("Invalid playlist number.");
                return;
            }

            String playlistName = playlist[0];
            String ownerUsername = playlist[1];

            System.out.println("==================================================================================");
            System.out.print("Enter a name for your copy: ");
            String newPlaylistName = scanner.nextLine().trim();

            out.println("COPY_SHARED_PLAYLIST " + ownerUsername + " " + playlistName + " " + newPlaylistName);

            String response = in.readLine();
            System.out.println("==================================================================================");
            System.out.println(response);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
        System.out.println("==================================================================================");
    }

    /**
     * Joue une playlist partagée
     */
    private void playSharedPlaylist(Map<Integer, String[]> playlists) throws IOException {
        System.out.println("==================================================================================");
        System.out.print("Enter the number of the playlist to play: ");

        try {
            int playlistIndex = Integer.parseInt(scanner.nextLine().trim());
            String[] playlist = playlists.get(playlistIndex);

            if (playlist == null) {
                System.out.println("Invalid playlist number.");
                return;
            }

            String playlistName = playlist[0];
            String ownerUsername = playlist[1];

            System.out.println("==================================================================================");
            System.out.println("Loading shared playlist: " + playlistName);

            out.println("LOAD_SHARED_PLAYLIST " + ownerUsername + " " + playlistName);

            String response = in.readLine();
            if (!response.startsWith("SUCCESS")) {
                System.out.println("Error loading playlist: " + response);
                return;
            }

            // Démarrer la lecture - réutilise la logique du PlayerUI
            playerUI.startPlayerWithLoadedPlaylist(playlistName, ownerUsername);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }
}