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
     * Add a collaborator safely with enhanced validation
     */
    public void addCollaborator(User user) {
        if (user == null) {
            System.out.println("DEBUG: Cannot add null user as collaborator");
            return;
        }

        String username = user.getUsername();
        if (username == null || username.trim().isEmpty()) {
            System.out.println("DEBUG: Cannot add user with null/empty username");
            return;
        }

        // Owner cannot be added as collaborator
        if (username.equals(ownerUsername)) {
            System.out.println("DEBUG: Owner cannot be added as collaborator: " + username);
            return;
        }

        // Check if already a collaborator (case-insensitive)
        boolean alreadyExists = collaboratorUsernames.stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(username));

        if (alreadyExists) {
            System.out.println("DEBUG: User already a collaborator: " + username);
            return;
        }

        // Add the collaborator
        collaboratorUsernames.add(username);
        System.out.println("DEBUG: Successfully added collaborator: " + username + " to playlist: " + getName());
    }

    /**
     * Add collaborator by username directly (useful for deserialization)
     */
    public void addCollaboratorByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        // Owner check
        if (username.equals(ownerUsername)) {
            return;
        }

        // Duplicate check (case-insensitive)
        boolean alreadyExists = collaboratorUsernames.stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(username));

        if (!alreadyExists) {
            collaboratorUsernames.add(username);
            System.out.println("DEBUG: Added collaborator by username: " + username);
        }
    }

    /**
     * Remove a collaborator with enhanced logging
     */
    public void removeCollaborator(User user) {
        if (user == null) return;

        String username = user.getUsername();
        boolean removed = collaboratorUsernames.removeIf(existing ->
                existing.equalsIgnoreCase(username));

        if (removed) {
            System.out.println("DEBUG: Removed collaborator: " + username + " from playlist: " + getName());
        } else {
            System.out.println("DEBUG: Collaborator not found for removal: " + username);
        }
    }

    /**
     * Check if a user is a collaborator with enhanced validation
     */
    public boolean isCollaborator(User user) {
        if (user == null) return false;

        String username = user.getUsername();
        if (username == null) return false;

        // Owner is always considered a collaborator
        if (username.equals(ownerUsername)) {
            return true;
        }

        // Check if in collaborators list (case-insensitive)
        return collaboratorUsernames.stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(username));
    }

    /**
     * Check if user can modify this playlist
     */
    public boolean canUserModify(String username) {
        if (username == null) return false;

        // Owner can always modify
        if (username.equals(ownerUsername)) {
            return true;
        }

        // Check if user is a collaborator
        return collaboratorUsernames.stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(username));
    }

    /**
     * Get count of collaborators
     */
    public int getCollaboratorCount() {
        return collaboratorUsernames.size();
    }

    /**
     * Debug method to print all collaborators
     */
    public void debugCollaborators() {
        System.out.println("=== COLLABORATIVE PLAYLIST DEBUG ===");
        System.out.println("Playlist: " + getName());
        System.out.println("Owner: " + ownerUsername);
        System.out.println("Collaborators (" + collaboratorUsernames.size() + "):");

        if (collaboratorUsernames.isEmpty()) {
            System.out.println("  (No collaborators)");
        } else {
            for (int i = 0; i < collaboratorUsernames.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + collaboratorUsernames.get(i));
            }
        }
        System.out.println("=====================================");
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