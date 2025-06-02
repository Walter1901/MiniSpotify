package client.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * Command Invoker for Command pattern implementation.
 * Manages registration and execution of commands.
 * Follows Single Responsibility Principle (SRP).
 */
public class CommandInvoker {
    private Map<String, Command> commands;

    /**
     * Initialize command registry
     */
    public CommandInvoker() {
        commands = new HashMap<>();
    }

    /**
     * Register a command with a name
     * @param commandName Name to associate with command
     * @param command Command implementation to register
     */
    public void register(String commandName, Command command) {
        commands.put(commandName.toLowerCase(), command);
    }

    /**
     * Execute a registered command by name
     * @param commandName Name of command to execute
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
     * Check if a command is registered
     * @param commandName Name to check
     * @return true if command exists, false otherwise
     */
    public boolean hasCommand(String commandName) {
        return commands.containsKey(commandName.toLowerCase());
    }

    /**
     * Display all available commands
     */
    public void listCommands() {
        System.out.println("Available commands:");
        commands.keySet().forEach(cmd -> System.out.println("- " + cmd));
    }
}