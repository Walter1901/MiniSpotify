package server.music;

import users.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Playlist collaborative permettant à plusieurs utilisateurs de la modifier
 */
public class CollaborativePlaylist extends Playlist {
    private User owner;
    private List<User> collaborators;

    /**
     * Constructeur
     */
    public CollaborativePlaylist(String name, User owner) {
        super(name);
        this.owner = owner;
        this.collaborators = new ArrayList<>();
    }

    /**
     * Ajoute un collaborateur
     */
    public void addCollaborator(User user) {
        if (user != null && !collaborators.contains(user) && !user.equals(owner)) {
            collaborators.add(user);
        }
    }

    /**
     * Supprime un collaborateur
     */
    public void removeCollaborator(User user) {
        collaborators.remove(user);
    }

    /**
     * Vérifie si un utilisateur est collaborateur
     */
    public boolean isCollaborator(User user) {
        return user != null && (user.equals(owner) || collaborators.contains(user));
    }

    /**
     * Retourne le propriétaire
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Retourne la liste des collaborateurs
     */
    public List<User> getCollaborators() {
        return new ArrayList<>(collaborators);
    }
}