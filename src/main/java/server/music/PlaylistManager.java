package server.music;

import java.util.HashMap;
import java.util.Map;

public class PlaylistManager {
    private Map<String, Playlist> playlists;

    public PlaylistManager() {
        playlists = new HashMap<>();
    }

    // Créer une nouvelle playlist si le nom n'existe pas déjà
    public boolean createPlaylist(String name) {
        if (playlists.containsKey(name)) {
            return false;
        }
        playlists.put(name, new Playlist(name));
        return true;
    }

    // Supprime une playlist existante
    public boolean deletePlaylist(String name) {
        return playlists.remove(name) != null;
    }

    // Renommer une playlist (création d'une nouvelle et suppression de l'ancienne)
    public boolean renamePlaylist(String oldName, String newName) {
        if (!playlists.containsKey(oldName) || playlists.containsKey(newName)) {
            return false;
        }
        Playlist oldPlaylist = playlists.remove(oldName);
        Playlist newPlaylist = new Playlist(newName);
        PlaylistNode current = oldPlaylist.getHead();
        while (current != null) {
            newPlaylist.addSong(current.getSong());
            current = current.getNext();
        }
        playlists.put(newName, newPlaylist);
        return true;
    }

    // Récupère une playlist par nom
    public Playlist getPlaylist(String name) {
        return playlists.get(name);
    }

    // Retourne la map de toutes les playlists
    public Map<String, Playlist> getAllPlaylists() {
        return playlists;
    }
}