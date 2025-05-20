package server.music;

import users.User;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * Collaborative playlist that allows multiple users to modify it
 * Modified version to avoid serialization issues
 */
public class CollaborativePlaylist extends Playlist implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ownerUsername; // Store username instead of User object
    private List<String> collaboratorUsernames = new ArrayList<>(); // Store usernames instead of User objects

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
            System.out.println("WARNING: Attempted to add null collaborator to playlist " + getName());
            return;
        }

        String username = user.getUsername();

        if (username.equals(ownerUsername)) {
            System.out.println("WARNING: Owner cannot be added as a collaborator to their own playlist");
            return;
        }

        if (!collaboratorUsernames.contains(username)) {
            collaboratorUsernames.add(username);
            System.out.println("Added collaborator " + username + " to playlist " + getName());
        } else {
            System.out.println("User " + username + " is already a collaborator for playlist " + getName());
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

    /**
     * Get the owner username
     */
    public String getOwnerUsername() {
        return ownerUsername;
    }

    /**
     * Get the list of collaborator usernames (defensive copy)
     */
    public List<String> getCollaboratorUsernames() {
        return new ArrayList<>(collaboratorUsernames);
    }

    /**
     * Returns a string representation of the collaborative playlist
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Collaborative Playlist: ").append(getName());
        sb.append(" (Owner: ").append(ownerUsername != null ? ownerUsername : "unknown").append(")");

        if (!collaboratorUsernames.isEmpty()) {
            sb.append(" - Collaborators: ");
            boolean first = true;
            for (String username : collaboratorUsernames) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(username);
                first = false;
            }
        }

        return sb.toString();
    }
}