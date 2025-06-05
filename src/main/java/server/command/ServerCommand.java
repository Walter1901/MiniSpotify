package server.command;

import java.io.PrintWriter;

/**
 * Interface for server-side Command pattern
 */
public interface ServerCommand {
    /**
     * Execute the command
     * @param out Writer to send response to client
     * @return true if successful, false otherwise
     * */
    boolean execute(PrintWriter out);
}