package server.command;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import server.protocol.ServerProtocol;

/**
 * Exécuteur de commandes serveur
 * Implémentation du Command Pattern
 */
public class CommandExecutor {
    private Map<String, ServerCommand> commands;

    /**
     * Constructeur
     */
    public CommandExecutor() {
        commands = new HashMap<>();
        // Les commandes sont enregistrées par le ClientHandler
    }

    /**
     * Enregistre une commande
     */
    public void registerCommand(String commandName, ServerCommand command) {
        commands.put(commandName, command);
    }

    /**
     * Exécute une commande
     * @return true si l'exécution a réussi, false sinon
     */
    public boolean executeCommand(String commandName, String args, PrintWriter out) {
        ServerCommand command = commands.get(commandName);

        if (command != null) {
            return command.execute(out);
        } else {
            out.println(ServerProtocol.RESP_ERROR + ": Unknown command '" + commandName + "'");
            return false;
        }
    }

    /**
     * Vérifie si une commande existe
     */
    public boolean hasCommand(String commandName) {
        return commands.containsKey(commandName);
    }

    /**
     * Supprime une commande
     */
    public void removeCommand(String commandName) {
        commands.remove(commandName);
    }

    /**
     * Récupère toutes les commandes disponibles
     */
    public Map<String, ServerCommand> getAllCommands() {
        return new HashMap<>(commands);
    }
}