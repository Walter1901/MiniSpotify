package server.music;

import users.User;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * Collaborative playlist that allows multiple users to modify it
 */
public class CollaborativePlaylist extends Playlist implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ownerUsername; // Store username instead of User object
    private List<String> collaboratorUsernames = new ArrayList<>();

    /**
     * Constructor
     */
    public CollaborativePlaylist(String name, User owner) {
        super(name);
        this.ownerUsername = owner != null ? owner.getUsername() : null;
    }

    /**
     * Constructor for deserialization
     */
    public CollaborativePlaylist(String name, String ownerUsername) {
        super(name);
        this.ownerUsername = ownerUsername;
    }

    /**
     * Add a collaborator safely
     */
    public void addCollaborator(User user) {
        if (user == null) {
            return;
        }

        String username = user.getUsername();

        if (username.equals(ownerUsername)) {
            return; // Owner cannot be added as collaborator
        }

        if (!collaboratorUsernames.contains(username)) {
            collaboratorUsernames.add(username);
        }
    }

    /**
     * Remove a collaborator
     */
    public void removeCollaborator(User user) {
        if (user == null) return;
        collaboratorUsernames.remove(user.getUsername());
    }

    /**
     * Check if a user is a collaborator
     */
    public boolean isCollaborator(User user) {
        if (user == null) return false;
        String username = user.getUsername();
        return username.equals(ownerUsername) || collaboratorUsernames.contains(username);
    }

    // Getters
    public String getOwnerUsername() { return ownerUsername; }
    public List<String> getCollaboratorUsernames() { return new ArrayList<>(collaboratorUsernames); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Collaborative Playlist: ").append(getName());
        sb.append(" (Owner: ").append(ownerUsername != null ? ownerUsername : "unknown").append(")");

        if (!collaboratorUsernames.isEmpty()) {
            sb.append(" - Collaborators: ");
            sb.append(String.join(", ", collaboratorUsernames));
        }

        return sb.toString();
    }
}