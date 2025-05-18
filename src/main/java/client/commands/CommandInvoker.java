package client.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * Invocateur de commandes pour le pattern Command
 */
public class CommandInvoker {
    private Map<String, Command> commands;

    /**
     * Constructeur
     */
    public CommandInvoker() {
        commands = new HashMap<>();
    }

    /**
     * Enregistre une commande
     */
    public void register(String commandName, Command command) {
        commands.put(commandName.toLowerCase(), command);
    }

    /**
     * Exécute une commande
     */
    public void execute(String commandName) {
        Command command = commands.get(commandName.toLowerCase());
        if (command != null) {
            command.execute();
        } else {
            System.out.println("Unknown command: " + commandName);
        }
    }

    /**
     * Vérifie si une commande existe
     */
    public boolean hasCommand(String commandName) {
        return commands.containsKey(commandName.toLowerCase());
    }

    /**
     * Affiche toutes les commandes disponibles
     */
    public void listCommands() {
        System.out.println("Available commands:");
        commands.keySet().forEach(cmd -> System.out.println("- " + cmd));
    }
}