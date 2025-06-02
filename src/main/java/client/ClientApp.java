package client;

import java.io.*;
import java.net.Socket;
import client.ui.UserInterface;

/**
 * Main entry point for the client application.
 * Implements Singleton pattern to ensure only one client instance.
 * Handles connection to server and UI initialization.
 */
public class ClientApp {
    // Server connection configuration
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 12345;

    // Singleton instance (volatile for thread safety)
    private static volatile ClientApp instance;

    // Client state and network components
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserInterface ui;
    private boolean isConnected;

    /**
     * Private constructor for Singleton pattern
     */
    private ClientApp() {
        this.isConnected = false;
    }

    /**
     * Get Singleton instance using Double-Checked Locking pattern
     * @return Single instance of ClientApp
     */
    public static ClientApp getInstance() {
        if (instance == null) {
            synchronized (ClientApp.class) {
                if (instance == null) {
                    instance = new ClientApp();
                }
            }
        }
        return instance;
    }

    /**
     * Establish connection to the MiniSpotify server
     * @return true if connection successful, false otherwise
     */
    public boolean connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;

            // Read welcome message from server
            String welcomeMessage = in.readLine();
            System.out.println(welcomeMessage);

            return true;
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Disconnect from server and clean up resources
     */
    public void disconnect() {
        isConnected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Initialize and start the user interface
     */
    public void startUI() {
        if (!isConnected) {
            System.err.println("Cannot start UI - not connected to server.");
            return;
        }

        ui = new UserInterface(socket, in, out);
        ui.start();
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        ClientApp client = ClientApp.getInstance();
        if (client.connect()) {
            client.startUI();
        } else {
            System.err.println("Failed to connect. Exiting.");
        }
    }

    // Getters for network components
    public BufferedReader getIn() { return in; }
    public PrintWriter getOut() { return out; }
    public Socket getSocket() { return socket; }
    public boolean isConnected() { return isConnected; }
}