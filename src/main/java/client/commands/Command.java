package client.commands;

/**
 * Command interface for implementing Command pattern on client side.
 * Each command encapsulates a request as an object, allowing for
 * parameterization and queuing of requests.
 */
public interface Command {
    /**
     * Execute the command
     */
    void execute();
}