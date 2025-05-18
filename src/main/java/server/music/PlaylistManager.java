package server.music;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire centralisé de playlists
 * Implémentation du pattern Singleton
 */
public class PlaylistManager {
    // Instance unique
    private static volatile PlaylistManager instance = null;

    // Map des playlists (nom -> playlist)
    private Map<String, Playlist> playlists;

    /**
     * Constructeur privé (Singleton)
     */
    private PlaylistManager() {
        playlists = new HashMap<>();
    }

    /**
     * Getter pour l'instance Singleton
     */
    public static PlaylistManager getInstance() {
        if (instance == null) {
            synchronized (PlaylistManager.class) {
                if (instance == null) {
                    instance = new PlaylistManager();
                }
            }
        }
        return instance;
    }

    /**
     * Crée une nouvelle playlist si le nom n'existe pas déjà
     */
    public boolean createPlaylist(String name) {
        if (name == null || name.trim().isEmpty() || playlists.containsKey(name)) {
            return false;
        }
        playlists.put(name, new Playlist(name));
        return true;
    }

    /**
     * Supprime une playlist existante
     */
    public boolean deletePlaylist(String name) {
        return playlists.remove(name) != null;
    }

    /**
     * Renomme une playlist
     */
    public boolean renamePlaylist(String oldName, String newName) {
        if (oldName == null || newName == null ||
                !playlists.containsKey(oldName) ||
                playlists.containsKey(newName) ||
                newName.trim().isEmpty()) {
            return false;
        }

        Playlist oldPlaylist = playlists.remove(oldName);
        Playlist newPlaylist = new Playlist(newName);

        // Copier les chansons de l'ancienne playlist
        PlaylistNode current = oldPlaylist.getHead();
        while (current != null) {
            newPlaylist.addSong(current.getSong());
            current = current.getNext();
        }

        playlists.put(newName, newPlaylist);
        return true;
    }

    /**
     * Récupère une playlist par nom
     */
    public Playlist getPlaylist(String name) {
        return playlists.get(name);
    }

    /**
     * Retourne la map de toutes les playlists
     */
    public Map<String, Playlist> getAllPlaylists() {
        // Retourne une copie défensive pour éviter les modifications externes
        return new HashMap<>(playlists);
    }

    /**
     * Vérifie si une playlist existe
     */
    public boolean playlistExists(String name) {
        return playlists.containsKey(name);
    }
}