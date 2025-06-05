package client.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * User interface for authentication
 */
public class AuthenticationUI {
    private UserInterface mainUI;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    /**
     * Constructor
     */
    public AuthenticationUI(UserInterface mainUI, BufferedReader in, PrintWriter out, Scanner scanner) {
        this.mainUI = mainUI;
        this.in = in;
        this.out = out;
        this.scanner = scanner;
    }

    /**
     * Display the initial menu
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

            }
        }
    }

    /**
     * Exit Application
     */
    private void exitApplication() {
        System.out.println("==================================================================================");
        System.out.println("Thank you for using MiniSpotify! Goodbye.");
        System.out.println("==================================================================================");
        mainUI.setExitRequested(true);
    }

    /**
     * Manages account creation
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
                accountCreated = true;
            } else {
                System.out.println("==================================================================================");
                System.out.println("Error: " + response);
                System.out.println("==================================================================================");

                // Ask user to try again or return to main menu
                System.out.println("Do you want to try again? (yes / no): ");
                String retry = scanner.nextLine().trim().toLowerCase();
                if (!retry.equals("yes")) {
                    // Back to main menu
                    return;
                }
                // Otherwise, continue the loop
            }
        }
    }

    /**
     * Manages connection with limit of attempts
     */
    public void login() throws IOException {
        // Maximum 2 attempts
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
                return;
            } else {
                System.out.println("==================================================================================");
                System.out.println("❌ Authentication failed.");
                System.out.println("==================================================================================");

                attempts++;

                // If this was the last authorized attempt
                if (attempts >= maxAttempts) {
                    System.out.println("==================================================================================");
                    System.out.println("Too many failed attempts. Returning to main menu.");
                    System.out.println("==================================================================================");
                    // Do nothing else, the method will terminate and return to the main menu.
                }
            }
        }
    }

    /**
     * Manage disconnection
     */
    public void logout() {
        out.println("LOGOUT");
        mainUI.setLoggedIn(false);
        mainUI.setCurrentUser(null);
        System.out.println("==================================================================================");
        System.out.println("You have been logged out.");
        System.out.println("==================================================================================");

        // Add a small delay for the user to read the message
        try {
            Thread.sleep(1500); // Wait 1.5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}