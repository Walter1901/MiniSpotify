package client.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interface utilisateur pour la bibliothèque musicale
 */
public class MusicLibraryUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    /**
     * Constructeur
     */
    public MusicLibraryUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
    }

    /**
     * Gère la bibliothèque musicale
     */
    public void manageMusicLibrary() throws IOException {
        boolean back = false;
        while (!back) {
            System.out.println("==================================================================================");
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
                    System.out.println("==================================================================================");
                    System.out.println("Invalid option..");
                    System.out.println("==================================================================================");
            }
        }
    }

    /**
     * Affiche toutes les chansons
     */
    private void displayAllSongs() throws IOException {
        System.out.println("==================================================================================");
        System.out.println("\n--- List of songs in the library ---");
        System.out.println("==================================================================================");

        out.println("GET_ALL_SONGS");

        String response;
        List<String> songs = new ArrayList<>();
        while (!(response = in.readLine()).equals("END")) {
            songs.add(response);
            System.out.println(response);
        }

        if (songs.isEmpty()) {
            System.out.println("==================================================================================");
            System.out.println("No songs have been added.");
            System.out.println("==================================================================================");
        }
    }

    /**
     * Recherche par titre
     */
    private void searchByTitle() throws IOException {
        System.out.println("==================================================================================");
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
            System.out.println("==================================================================================");
            System.out.println("No songs found for this title.");
            System.out.println("==================================================================================");
        }
    }

    /**
     * Recherche par artiste
     */
    private void searchByArtist() throws IOException {
        System.out.println("==================================================================================");
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
            System.out.println("==================================================================================");
            System.out.println("No songs found for this artist.");
            System.out.println("==================================================================================");
        }
    }
}