package client;

import java.io.*;
import java.net.Socket;
import client.ui.UserInterface;

/**
 * Point d'entrée de l'application client
 */
public class ClientApp {
    // Constantes de configuration
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 12345;

    // Instance unique (Singleton)
    private static volatile ClientApp instance;

    // État du client
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserInterface ui;
    private boolean isConnected;

    /**
     * Constructeur privé (Singleton)
     */
    private ClientApp() {
        this.isConnected = false;
    }

    /**
     * Getter pour l'instance (Double-Checked Locking)
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
     * Connecte le client au serveur
     */
    public boolean connect() {
        try {
            System.out.println("Connecting to server at " + SERVER_ADDRESS + ":" + SERVER_PORT + "...");
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;

            // Welcome message from server
            String welcomeMessage = in.readLine();
            System.out.println(welcomeMessage);

            return true;
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Déconnecte le client du serveur
     */
    public void disconnect() {
        isConnected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Démarre l'interface utilisateur
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
     * Méthode principale
     */
    public static void main(String[] args) {
        ClientApp client = ClientApp.getInstance();
        if (client.connect()) {
            client.startUI();
        } else {
            System.err.println("Failed to connect. Exiting.");
        }
    }

    /**
     * Getters pour les flux et la socket
     */
    public BufferedReader getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return isConnected;
    }
}