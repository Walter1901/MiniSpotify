package client;

import java.io.*;
import java.net.Socket;

public class ClientApp {

    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try {
            // Établissement de la connexion au serveur
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Welcome message from server
            System.out.println(in.readLine());

            // Démarrage de l'interface utilisateur avec la connexion socket
            UserInterface ui = new UserInterface(socket, in, out);
            ui.start();

        } catch (IOException e) {
            System.out.println("Erreur de connexion au serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}