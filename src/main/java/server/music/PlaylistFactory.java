package server.music;

import users.User;

/**
 * Factory for creating different types of playlists.
 * Implementation of the Factory Pattern.
 * <p>
 * This class provides static methods to create various types of playlists,
 * encapsulating the instantiation logic and providing a consistent interface.
 * </p>
 */
public class PlaylistFactory {

    /**
     * Playlist type enumeration.
     */
    public enum PlaylistType {
        /** Standard playlist with basic functionality */
        STANDARD,
        /** Collaborative playlist that can be edited by multiple users */
        COLLABORATIVE,
    }

    /**
     * Creates a playlist based on the requested type.
     *
     * @param name The name of the playlist
     * @param type The type of playlist to create
     * @param owner The user who owns the playlist (required for collaborative and smart playlists)
     * @return A new playlist of the specified type
     */
    public static Playlist createPlaylist(String name, PlaylistType type, User owner) {
        switch (type) {
            case COLLABORATIVE:
                return new CollaborativePlaylist(name, owner);
            case STANDARD:
            default:
                return new Playlist(name);
        }
    }

    /**
     * Creates a standard playlist.
     *
     * @param name The name of the playlist
     * @return A new standard playlist
     */
    public static Playlist createStandardPlaylist(String name) {
        return new Playlist(name);
    }

    /**
     * Creates a collaborative playlist.
     *
     * @param name The name of the playlist
     * @param owner The user who owns the playlist
     * @return A new collaborative playlist
     */
    public static Playlist createCollaborativePlaylist(String name, User owner) {
        return new CollaborativePlaylist(name, owner);
    }

}