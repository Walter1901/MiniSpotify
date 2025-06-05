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
 * User interface for social features
 */
public class SocialUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;
    private PlayerUI playerUI;

    /**
     * Constructor
     */
    public SocialUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner, PlayerUI playerUI) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
        this.playerUI = playerUI;
    }

    /**
     * Manage social features such as following users, viewing shared playlists, etc.
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
     * Follow a user by username
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
     * Display the list of users followed by the current user
     */
    private void viewFollowedUsers() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Users you are following:");
        System.out.println("==================================================================================");

        // Send a command to the server to obtain the list of tracked users
        out.println("GET_FOLLOWED_USERS");

        // Read server responses
        String response;
        List<String> followedUsers = new ArrayList<>();
        while (!(response = in.readLine()).equals("END")) {
            followedUsers.add(response);
        }

        // Display the list of followed users
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

        // Propose actions for these users
        if (!followedUsers.isEmpty()) {
            System.out.println("Do you want to unfollow a user? (yes / no)");
            String option = scanner.nextLine().trim().toLowerCase();

            if (option.equals("yes")) {
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
     * Display shared playlists from users the current user follows
     */
    private void viewSharedPlaylists() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("Shared playlists from users you follow:");
        System.out.println("==================================================================================");

        // Send a command to the server to obtain shared playlists
        out.println("GET_SHARED_PLAYLISTS");

        String response;
        Map<Integer, String[]> playlists = new HashMap<>(); // [playlistName, ownerUsername]
        int index = 1;

        // Expected response format: “PlaylistName|OwnerUsername”.
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

        // Action menu for shared playlists
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
     * Defines the user's preferences for sharing playlists
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
     * Display the songs in a shared playlist
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
                // Expected format: title|artist|album|genre|duration
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
     * Copy a shared playlist to the user's library
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
     * Play a shared playlist
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

            // Start playback - reuses PlayerUI logic
            playerUI.startPlayerWithLoadedPlaylist(playlistName, ownerUsername);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }
}