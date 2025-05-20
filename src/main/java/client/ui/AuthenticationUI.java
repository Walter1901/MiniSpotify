package client.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Interface utilisateur pour l'authentification
 */
public class AuthenticationUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    /**
     * Constructeur
     */
    public AuthenticationUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
    }

    /**
     * Affiche le menu initial
     */
    public void showInitialMenu() throws IOException {
        boolean validOption = false;

        while (!validOption) {
            System.out.println("==================================================================================");
            System.out.println("1. Create an account");
            System.out.println("2. Log in");
            System.out.println("3. Exit application");
            System.out.println("==================================================================================");
            System.out.print("Choose an option: ");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    createAccount();
                    validOption = true;
                    break;
                case "2":
                    login();
                    validOption = true;
                    break;
                case "3":
                    exitApplication();
                    validOption = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    // Continue la boucle
            }
        }
    }

    /**
     * Quitte l'application
     */
    private void exitApplication() {
        System.out.println("==================================================================================");
        System.out.println("Thank you for using MiniSpotify! Goodbye.");
        System.out.println("==================================================================================");
        mainUI.setExitRequested(true);
    }

    /**
     * Gère la création de compte
     */
    public void createAccount() throws IOException {
        boolean accountCreated = false;

        while (!accountCreated) {
            System.out.println("==================================================================================");
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            System.out.print("Account type (free/premium): ");
            String accountType = scanner.nextLine();
            System.out.println("==================================================================================");

            out.println("CREATE " + username + " " + password + " " + accountType);

            String response = in.readLine();
            if (response != null && response.startsWith("CREATE_SUCCESS")) {
                System.out.println("==================================================================================");
                System.out.println("Account created successfully!");
                System.out.println("==================================================================================");
                mainUI.setCurrentUser(username);
                mainUI.setLoggedIn(true);
                accountCreated = true; // Sortir de la boucle
            } else {
                System.out.println("==================================================================================");
                System.out.println("Error: " + response);
                System.out.println("==================================================================================");

                // Demander à l'utilisateur s'il veut réessayer ou revenir au menu principal
                System.out.println("Do you want to try again? (y/n): ");
                String retry = scanner.nextLine().trim().toLowerCase();
                if (!retry.equals("y")) {
                    // Retour au menu principal
                    return;
                }
                // Sinon, continue la boucle
            }
        }
    }

    /**
     * Gère la connexion avec limite de tentatives
     */
    public void login() throws IOException {
        // Maximum de 2 tentatives
        int maxAttempts = 2;
        int attempts = 0;

        while (attempts < maxAttempts) {
            System.out.println("==================================================================================");
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            System.out.println("==================================================================================");

            out.println("LOGIN " + username + " " + password);

            String response = in.readLine();
            if ("LOGIN_SUCCESS".equalsIgnoreCase(response)) {
                System.out.println("==================================================================================");
                System.out.println("✅ Successfully authenticated.");
                System.out.println("==================================================================================");
                mainUI.setCurrentUser(username);
                mainUI.setLoggedIn(true);
                return; // Sortir de la méthode après un succès
            } else {
                System.out.println("==================================================================================");
                System.out.println("❌ Authentication failed.");
                System.out.println("==================================================================================");

                attempts++;

                // Si c'était la dernière tentative autorisée
                if (attempts >= maxAttempts) {
                    System.out.println("==================================================================================");
                    System.out.println("Too many failed attempts. Returning to main menu.");
                    System.out.println("==================================================================================");
                    // Ne rien faire d'autre, la méthode va se terminer et retourner au menu principal
                }
            }
        }
    }

    /**
     * Gère la déconnexion
     */
    public void logout() {
        out.println("LOGOUT");
        mainUI.setLoggedIn(false);
        mainUI.setCurrentUser(null);
        System.out.println("==================================================================================");
        System.out.println("You have been logged out.");
        System.out.println("==================================================================================");

        // Ajouter un petit délai pour que l'utilisateur puisse lire le message
        try {
            Thread.sleep(1500); // Attendre 1.5 secondes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // IMPORTANT: NE PAS appeler mainUI.setExitRequested(true) ici !
    }
}