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
            showConnectionBanner();
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;

            // Read welcome message from server
            String welcomeMessage = in.readLine();
            showConnectionSuccess();

            return true;
        } catch (IOException e) {
            showConnectionError();
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
            // Silent cleanup
        }
    }

    /**
     * Initialize and start the user interface
     */
    public void startUI() {
        if (!isConnected) {
            showNotConnectedError();
            return;
        }

        ui = new UserInterface(socket, in, out);
        ui.start();
    }

    /**
     * Display connection banner
     */
    private void showConnectionBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                           🔗 CONNECTING TO SERVER                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
        System.out.print("🔄 Connecting to " + SERVER_ADDRESS + ":" + SERVER_PORT + "... ");
    }

    /**
     * Display connection success
     */
    private void showConnectionSuccess() {
        System.out.println("✅ Connected!");
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                          🎉 CONNECTION ESTABLISHED                               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Display connection error
     */
    private void showConnectionError() {
        System.out.println("❌ Failed!");
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                            ❌ CONNECTION FAILED                                  ║");
        System.out.println("║                                                                                  ║");
        System.out.println("║  • Please ensure the server is running                                          ║");
        System.out.println("║  • Check your network connection                                                ║");
        System.out.println("║  • Verify server address and port                                               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Display not connected error
     */
    private void showNotConnectedError() {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                              ⚠️  NOT CONNECTED                                   ║");
        System.out.println("║                     Cannot start UI - not connected to server                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        ClientApp client = ClientApp.getInstance();
        if (client.connect()) {
            client.startUI();
        } else {
            System.out.println("\n🚪 Exiting application...");
        }
    }

    // Getters for network components
    public BufferedReader getIn() { return in; }
    public PrintWriter getOut() { return out; }
    public Socket getSocket() { return socket; }
    public boolean isConnected() { return isConnected; }
}