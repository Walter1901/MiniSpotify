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
        System.out.println("==================================================================================");
        System.out.println("1. Create an account");
        System.out.println("2. Log in");
        System.out.println("==================================================================================");
        System.out.print("Choose an option: ");
        String option = scanner.nextLine().trim();

        if (option.equals("1")) {
            createAccount();
        } else if (option.equals("2")) {
            login();
        } else {
            System.out.println("Invalid option. Please try again.");
            showInitialMenu();
        }
    }

    /**
     * Gère la création de compte
     */
    public void createAccount() throws IOException {
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
        } else {
            System.out.println("==================================================================================");
            System.out.println("Error: " + response);
            System.out.println("==================================================================================");
            createAccount(); // retry
        }
    }

    /**
     * Gère la connexion
     */
    public void login() throws IOException {
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
        } else {
            System.out.println("==================================================================================");
            System.out.println("❌ Authentication failed.");
            System.out.println("==================================================================================");
            login();
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
    }
}