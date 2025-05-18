package server.command;

import java.io.PrintWriter;

/**
 * Interface pour le pattern Command côté serveur
 */
public interface ServerCommand {
    /**
     * Exécute la commande
     * @param out Writer pour envoyer la réponse au client
     * @return true si l'exécution s'est bien passée, false sinon
     */
    boolean execute(PrintWriter out);
}