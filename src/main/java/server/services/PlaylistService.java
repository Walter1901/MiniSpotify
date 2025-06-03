package server.services;

import persistence.UserPersistenceManager;
import server.music.CollaborativePlaylist;
import server.music.MusicLibrary;
import server.music.Playlist;
import server.music.Song;
import users.User;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service for handling playlist operations
 * Extracted from ClientHandler for better separation of concerns
 */
public class PlaylistService {

    private final Consumer<String> logger;

    public PlaylistService(Consumer<String> logger) {
        this.logger = logger;
    }

    /**
     * Create a standard playlist
     */
    public PlaylistResult createPlaylist(User user, String playlistName) {
        try {
            if (user == null) {
                return new PlaylistResult(false, "User not logged in");
            }

            if (playlistName == null || playlistName.trim().isEmpty()) {
                return new PlaylistResult(false, "Playlist name cannot be empty");
            }

            if (!user.canAddPlaylist()) {
                return new PlaylistResult(false, "Playlist limit reached for your account type");
            }

            // Check if playlist already exists
            boolean exists = user.getPlaylists().stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

            if (exists) {
                return new PlaylistResult(false, "Playlist already exists");
            }

            // Create and add playlist
            user.addPlaylist(new Playlist(playlistName));
            UserPersistenceManager.updateUser(user);

            logger.accept("📝 Playlist created: " + playlistName + " by " + user.getUsername());
            return new PlaylistResult(true, "Playlist created successfully");

        } catch (Exception e) {
            logger.accept("💥 Error creating playlist: " + e.getMessage());
            return new PlaylistResult(false, "Server error creating playlist");
        }
    }

    /**
     * Create a collaborative playlist
     */
    public PlaylistResult createCollaborativePlaylist(User user, String playlistName, String collaboratorsString) {
        try {
            if (user == null) {
                return new PlaylistResult(false, "User not logged in");
            }

            if (!user.canAddPlaylist()) {
                return new PlaylistResult(false, "Playlist limit reached");
            }

            // Check if playlist exists
            boolean exists = user.getPlaylists().stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

            if (exists) {
                return new PlaylistResult(false, "Playlist already exists");
            }

            // Create collaborative playlist
            CollaborativePlaylist playlist = new CollaborativePlaylist(playlistName, user);

            // Add collaborators if specified
            if (collaboratorsString != null && !collaboratorsString.trim().isEmpty()) {
                String[] collaboratorNames = collaboratorsString.split(",");
                for (String collaboratorName : collaboratorNames) {
                    collaboratorName = collaboratorName.trim();
                    if (!collaboratorName.isEmpty()) {
                        User collaborator = UserPersistenceManager.getUserByUsername(collaboratorName);
                        if (collaborator != null) {
                            playlist.addCollaborator(collaborator);
                        }
                    }
                }
            }

            user.addPlaylist(playlist);
            UserPersistenceManager.updateUser(user);

            logger.accept("🤝 Collaborative playlist created: " + playlistName + " by " + user.getUsername());
            return new PlaylistResult(true, "Collaborative playlist created successfully");

        } catch (Exception e) {
            logger.accept("💥 Error creating collaborative playlist: " + e.getMessage());
            return new PlaylistResult(false, "Server error creating collaborative playlist");
        }
    }

    /**
     * Get user's playlists
     */
    public List<String> getUserPlaylists(User user) {
        List<String> playlistNames = new ArrayList<>();
        if (user != null) {
            for (Playlist playlist : user.getPlaylists()) {
                playlistNames.add(playlist.getName());
            }
        }
        return playlistNames;
    }

    /**
     * Check if playlist exists
     */
    public boolean playlistExists(User user, String playlistName) {
        if (user == null || playlistName == null) return false;

        return user.getPlaylists().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));
    }

    /**
     * Get songs from a playlist
     */
    public PlaylistSongsResult getPlaylistSongs(User user, String playlistName) {
        try {
            if (user == null) {
                return new PlaylistSongsResult(false, "User not logged in", new ArrayList<>());
            }

            Playlist playlist = user.getPlaylists().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(playlistName))
                    .findFirst()
                    .orElse(null);

            if (playlist == null) {
                return new PlaylistSongsResult(false, "Playlist not found", new ArrayList<>());
            }

            List<Song> songs = playlist.getSongs();
            return new PlaylistSongsResult(true, "Found " + songs.size() + " songs", songs);

        } catch (Exception e) {
            logger.accept("💥 Error getting playlist songs: " + e.getMessage());
            return new PlaylistSongsResult(false, "Server error", new ArrayList<>());
        }
    }

    /**
     * Add song to playlist
     */
    public PlaylistResult addSongToPlaylist(User user, String playlistName, String songTitle) {
        try {
            if (user == null) {
                return new PlaylistResult(false, "User not logged in");
            }

            // Find playlist
            Playlist playlist = user.getPlaylists().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(playlistName))
                    .findFirst()
                    .orElse(null);

            if (playlist == null) {
                return new PlaylistResult(false, "Playlist not found");
            }

            // Find song in library
            Song originalSong = null;
            for (Song s : MusicLibrary.getInstance().getAllSongs()) {
                if (s.getTitle().equalsIgnoreCase(songTitle) ||
                        s.getTitle().toLowerCase().contains(songTitle.toLowerCase())) {
                    originalSong = s;
                    break;
                }
            }

            if (originalSong == null) {
                return new PlaylistResult(false, "Song not found in library");
            }

            // FIXED: Make variable effectively final for lambda
            final Song foundSong = originalSong;

            // Check if song already in playlist
            boolean alreadyExists = playlist.getSongs().stream()
                    .anyMatch(s -> s.getTitle().equalsIgnoreCase(foundSong.getTitle()));

            if (alreadyExists) {
                return new PlaylistResult(true, "Song already in playlist");
            }

            // Create copy and add to playlist
            Song songCopy = new Song(
                    foundSong.getTitle(),
                    foundSong.getArtist(),
                    foundSong.getAlbum(),
                    foundSong.getGenre(),
                    foundSong.getDuration()
            );
            songCopy.setFilePath(foundSong.getFilePath());

            playlist.addSong(songCopy);
            UserPersistenceManager.updateUser(user);

            logger.accept("🎵 Song added to playlist: " + songTitle + " -> " + playlistName);
            return new PlaylistResult(true, "Song added to playlist");

        } catch (Exception e) {
            logger.accept("💥 Error adding song to playlist: " + e.getMessage());
            return new PlaylistResult(false, "Server error adding song");
        }
    }

    /**
     * Remove song from playlist
     */
    public PlaylistResult removeSongFromPlaylist(User user, String playlistName, String songTitle) {
        try {
            if (user == null) {
                return new PlaylistResult(false, "User not logged in");
            }

            Playlist playlist = user.getPlaylists().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(playlistName))
                    .findFirst()
                    .orElse(null);

            if (playlist == null) {
                return new PlaylistResult(false, "Playlist not found");
            }

            boolean removed = playlist.removeSong(songTitle);

            if (removed) {
                UserPersistenceManager.updateUser(user);
                logger.accept("🗑️ Song removed from playlist: " + songTitle + " from " + playlistName);
                return new PlaylistResult(true, "Song removed from playlist");
            } else {
                return new PlaylistResult(false, "Song not found in playlist");
            }

        } catch (Exception e) {
            logger.accept("💥 Error removing song from playlist: " + e.getMessage());
            return new PlaylistResult(false, "Server error removing song");
        }
    }

    /**
     * Delete playlist
     */
    public PlaylistResult deletePlaylist(User user, String playlistName) {
        try {
            if (user == null) {
                return new PlaylistResult(false, "User not logged in");
            }

            boolean removed = user.removePlaylist(playlistName);

            if (removed) {
                UserPersistenceManager.updateUser(user);
                logger.accept("🗑️ Playlist deleted: " + playlistName + " by " + user.getUsername());
                return new PlaylistResult(true, "Playlist deleted successfully");
            } else {
                return new PlaylistResult(false, "Playlist not found");
            }

        } catch (Exception e) {
            logger.accept("💥 Error deleting playlist: " + e.getMessage());
            return new PlaylistResult(false, "Server error deleting playlist");
        }
    }

    // Result classes
    public static class PlaylistResult {
        public final boolean success;
        public final String message;

        public PlaylistResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class PlaylistSongsResult {
        public final boolean success;
        public final String message;
        public final List<Song> songs;

        public PlaylistSongsResult(boolean success, String message, List<Song> songs) {
            this.success = success;
            this.message = message;
            this.songs = songs;
        }
    }
}