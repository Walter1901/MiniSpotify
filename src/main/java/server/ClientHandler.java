package server;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import persistence.UserPersistenceManager;
import server.command.ServerCommand;
import server.music.MusicLibrary;
import server.music.Playlist;
import server.music.CollaborativePlaylist;
import server.music.Song;
import server.protocol.ServerProtocol;
import users.FreeUser;
import users.PremiumUser;
import users.User;
import utils.PasswordHasher;

/**
 * Gestionnaire de client qui traite les requêtes d'un client connecté
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private User loggedInUser;
    private BufferedReader in;
    private PrintWriter out;

    // Map des commandes implémentant le Command Factory Pattern
    private Map<String, CommandFactory> commandFactories;

    // Variable pour le timer de session
    private java.util.Timer sessionTimer;
    // Durée d'inactivité maximale avant expiration de session (30 minutes en millisecondes)
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000;
    // Dernier temps d'activité de l'utilisateur
    private long lastActivityTime;

    /**
     * Constructeur
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        initializeCommandFactories();
    }

    /**
     * Initialise les fabriques de commandes
     */
    private void initializeCommandFactories() {
        commandFactories = new HashMap<>();

        // Commandes d'authentification
        commandFactories.put("LOGIN", this::createLoginCommand);
        commandFactories.put("CREATE", this::createUserCommand);
        commandFactories.put("LOGOUT", args -> out -> {
            loggedInUser = null;
            return true;
        });

        // Commandes de playlist
        commandFactories.put("CREATE_PLAYLIST", this::createPlaylistCommand);
        commandFactories.put("GET_PLAYLISTS", this::getPlaylistsCommand);
        commandFactories.put("CHECK_PLAYLIST", this::checkPlaylistCommand);
        commandFactories.put("ADD_SONG_TO_PLAYLIST", this::addSongToPlaylistCommand);
        commandFactories.put("REMOVE_SONG_FROM_PLAYLIST", this::removeSongFromPlaylistCommand);
        commandFactories.put("REORDER_PLAYLIST_SONG", this::reorderPlaylistSongCommand);
        commandFactories.put("CREATE_COLLAB_PLAYLIST", this::createCollabPlaylistCommand);

        // Commandes de bibliothèque musicale
        commandFactories.put("GET_PLAYLIST_SONGS", this::getPlaylistSongsCommand);
        commandFactories.put("GET_ALL_SONGS", args -> getAllSongsCommand());
        commandFactories.put("SEARCH_TITLE", this::searchTitleCommand);
        commandFactories.put("SEARCH_ARTIST", this::searchArtistCommand);

        // Commandes du lecteur
        commandFactories.put("LOAD_PLAYLIST", this::loadPlaylistCommand);
        commandFactories.put("SET_PLAYBACK_MODE", this::setPlaybackModeCommand);
        commandFactories.put("PLAYER_PLAY", args -> playerPlayCommand(args));
        commandFactories.put("PLAYER_PAUSE", args -> playerPauseCommand(args));
        commandFactories.put("PLAYER_STOP", args -> playerStopCommand(args));
        commandFactories.put("PLAYER_NEXT", args -> playerNextCommand(args));
        commandFactories.put("PLAYER_PREV", args -> playerPrevCommand(args));
        commandFactories.put("PLAYER_EXIT", args -> playerExitCommand(args));

        // Commandes sociales
        commandFactories.put("FOLLOW_USER", this::followUserCommand);
        commandFactories.put("UNFOLLOW_USER", this::unfollowUserCommand);
        commandFactories.put("GET_FOLLOWED_USERS", this::getFollowedUsersCommand);
        commandFactories.put("GET_SHARED_PLAYLISTS", this::getSharedPlaylistsCommand);
        commandFactories.put("GET_SHARED_PLAYLIST_SONGS", this::getSharedPlaylistSongsCommand);
        commandFactories.put("COPY_SHARED_PLAYLIST", this::copySharedPlaylistCommand);
        commandFactories.put("LOAD_SHARED_PLAYLIST", this::loadSharedPlaylistCommand);
        commandFactories.put("SET_PLAYLIST_SHARING", this::setPlaylistSharingCommand);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("🎵 Welcome to MiniSpotify server!");

            // Démarrer le timer pour surveiller la session
            startSessionTimer();

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("DEBUG: Received command -> " + line);

                // Rafraîchir le dernier temps d'activité
                lastActivityTime = System.currentTimeMillis();

                try {
                    processCommand(line);
                } catch (Exception e) {
                    System.err.println("Error processing command '" + line + "': " + e.getMessage());
                    e.printStackTrace();
                    // Envoyer un message d'erreur au client mais ne pas interrompre la connexion
                    out.println("ERROR: Server error processing your request");
                }
            }

            // Si on sort de la boucle normalement, c'est que le client s'est déconnecté proprement
            System.out.println("DEBUG: Client disconnected normally");
        } catch (IOException e) {
            System.err.println("Connection error in client handler: " + e.getMessage());
            // Ne pas afficher la stack trace pour les erreurs de connexion ordinaires
            if (!(e instanceof java.net.SocketException)) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in client handler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }

    /**
     * Traite une commande reçue du client
     */
    private void processCommand(String commandLine) {
        String[] parts = commandLine.split(" ", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        CommandFactory factory = commandFactories.get(command);
        if (factory != null) {
            ServerCommand cmd = factory.createCommand(args);
            cmd.execute(out);
        } else {
            out.println(ServerProtocol.RESP_ERROR + ": Unknown command");
        }
    }

    /**
     * Ferme les ressources
     */
    private void closeResources() {
        try {
            // Arrêter le timer s'il existe
            if (sessionTimer != null) {
                sessionTimer.cancel();
                sessionTimer = null;
            }

            // Fermer les flux et la socket
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();

            System.out.println("DEBUG: Resources closed and cleaned up");
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Synchronise l'état de l'utilisateur avec la persistence
     */
    private void syncUserState() {
        if (loggedInUser != null) {
            List<User> users = UserPersistenceManager.loadUsers();
            for (User u : users) {
                if (u.getUsername().equals(loggedInUser.getUsername())) {
                    loggedInUser = u;
                    System.out.println("DEBUG: Synchronized user state for: " + loggedInUser.getUsername());
                    System.out.println("DEBUG: User now has " + loggedInUser.getPlaylists().size() + " playlists");
                    break;
                }
            }
        }
    }

    // Interfaces et implémentations pour Command Factory

    /**
     * Interface fonctionnelle pour la fabrique de commandes
     */
    @FunctionalInterface
    private interface CommandFactory {
        ServerCommand createCommand(String args);
    }

    // Commandes d'authentification

    private ServerCommand createLoginCommand(String args) {
        return out -> {
            String[] parts = args.split(" ");
            if (parts.length != 2) {
                out.println(ServerProtocol.RESP_LOGIN_FAIL + " Invalid format. Expected: LOGIN username password");
                return false;
            }

            String username = parts[0];
            String password = parts[1];

            List<User> users = UserPersistenceManager.loadUsers();
            User matchingUser = users.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);

            if (matchingUser != null && PasswordHasher.checkPassword(password, matchingUser.getPassword())) {
                loggedInUser = matchingUser;
                out.println(ServerProtocol.RESP_LOGIN_SUCCESS);
                System.out.println("DEBUG: User logged in: " + username);
                System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists");
                return true;
            } else {
                out.println(ServerProtocol.RESP_LOGIN_FAIL + " Incorrect username or password");
                return false;
            }
        };
    }

    private ServerCommand createUserCommand(String args) {
        return out -> {
            String[] parts = args.split(" ");
            if (parts.length != 3) {
                out.println(ServerProtocol.RESP_CREATE_FAIL + " Invalid arguments. Expected: CREATE username password accountType");
                return false;
            }

            String username = parts[0];
            String password = parts[1];
            String accountType = parts[2];

            if (UserPersistenceManager.doesUserExist(username)) {
                out.println(ServerProtocol.RESP_CREATE_FAIL + " Username already exists");
                return false;
            }

            try {
                String hashedPassword = PasswordHasher.hashPassword(password);
                User newUser;

                if ("free".equalsIgnoreCase(accountType)) {
                    newUser = new FreeUser(username, hashedPassword);
                } else if ("premium".equalsIgnoreCase(accountType)) {
                    newUser = new PremiumUser(username, hashedPassword);
                } else {
                    out.println(ServerProtocol.RESP_CREATE_FAIL + " Invalid account type. Use 'free' or 'premium'");
                    return false;
                }

                UserPersistenceManager.addUser(newUser);
                out.println(ServerProtocol.RESP_CREATE_SUCCESS);
                return true;
            } catch (Exception e) {
                System.err.println("❌ Error during CREATE: " + e.getMessage());
                e.printStackTrace();
                out.println(ServerProtocol.RESP_CREATE_FAIL + " Server error: " + e.getMessage());
                return false;
            }
        };
    }

    /**
     * Crée une commande pour créer une playlist
     */
    private ServerCommand createPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("CREATE_PLAYLIST_FAIL No user logged in");
                return false;
            }

            String playlistName = args.trim();
            System.out.println("DEBUG: Creating playlist: '" + playlistName + "'");

            if (loggedInUser.canAddPlaylist()) {
                boolean exists = loggedInUser.getPlaylists().stream()
                        .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

                if (!exists) {
                    loggedInUser.addPlaylist(new Playlist(playlistName));

                    // Mettre à jour l'utilisateur dans le système de persistance
                    UserPersistenceManager.updateUser(loggedInUser);

                    System.out.println("DEBUG: Playlist created: '" + playlistName + "'");
                    System.out.println("DEBUG: User now has " + loggedInUser.getPlaylists().size() + " playlists");
                    out.println(ServerProtocol.RESP_PLAYLIST_CREATED);
                    return true;
                } else {
                    out.println(ServerProtocol.RESP_PLAYLIST_EXISTS);
                    return false;
                }
            } else {
                out.println("CREATE_PLAYLIST_FAIL Limit reached");
                return false;
            }
        };
    }

    /**
     * Crée une commande pour créer une playlist collaborative
     */
    private ServerCommand createCollabPlaylistCommand(String args) {
        return out -> {
            // Vérifier l'état de la session avant de traiter la commande
            checkUserSession();

            if (loggedInUser == null) {
                out.println("CREATE_COLLAB_PLAYLIST_FAIL No user logged in");
                return false;
            }

            String[] parts = args.split(" ", 2);
            if (parts.length < 1) {
                out.println("CREATE_COLLAB_PLAYLIST_FAIL Invalid format");
                return false;
            }

            String playlistName = parts[0];
            System.out.println("DEBUG: Creating collaborative playlist: '" + playlistName + "'");

            // Vérification importante: pour les playlists collaboratives, utiliser la même vérification
            // que les playlists normales pour la limite - c'est là que se trouve le problème
            if (loggedInUser.canAddPlaylist()) {
                boolean exists = loggedInUser.getPlaylists().stream()
                        .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

                if (!exists) {
                    // Créer une playlist collaborative
                    CollaborativePlaylist playlist = new CollaborativePlaylist(playlistName, loggedInUser);

                    // Collaborateurs (s'il y en a)
                    if (parts.length > 1) {
                        String[] collaboratorNames = parts[1].split(",");
                        for (String collaboratorName : collaboratorNames) {
                            collaboratorName = collaboratorName.trim();
                            if (!collaboratorName.isEmpty()) {
                                User collaborator = UserPersistenceManager.getUserByUsername(collaboratorName);
                                if (collaborator != null) {
                                    playlist.addCollaborator(collaborator);
                                    System.out.println("DEBUG: Added collaborator: " + collaboratorName);
                                }
                            }
                        }
                    }

                    loggedInUser.addPlaylist(playlist);

                    // Mettre à jour l'utilisateur dans le système de persistance
                    UserPersistenceManager.updateUser(loggedInUser);

                    System.out.println("DEBUG: Collaborative playlist created: '" + playlistName + "'");
                    out.println("COLLAB_PLAYLIST_CREATED");
                    return true;
                } else {
                    out.println("CREATE_COLLAB_PLAYLIST_FAIL Playlist already exists");
                    return false;
                }
            } else {
                out.println("CREATE_COLLAB_PLAYLIST_FAIL Limit reached");
                return false;
            }
        };
    }

    /**
     * Crée une commande pour récupérer les playlists
     */
    private ServerCommand getPlaylistsCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                out.println(ServerProtocol.RESP_END);
                return false;
            }

            System.out.println("DEBUG: Getting playlists for user: " + loggedInUser.getUsername());
            System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists");

            if (loggedInUser.getPlaylists().isEmpty()) {
                out.println(ServerProtocol.RESP_END);
                return true;
            }

            for (Playlist playlist : loggedInUser.getPlaylists()) {
                out.println(playlist.getName());
                System.out.println("DEBUG: Sending playlist: " + playlist.getName());
            }
            out.println(ServerProtocol.RESP_END);
            return true;
        };
    }

    private ServerCommand checkPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                return false;
            }

            String playlistName = args.trim();
            System.out.println("DEBUG: Checking for playlist: '" + playlistName + "'");

            // Debug output all playlists
            System.out.println("DEBUG: User has " + loggedInUser.getPlaylists().size() + " playlists:");
            for (Playlist p : loggedInUser.getPlaylists()) {
                System.out.println("DEBUG: - '" + p.getName() + "'");
            }

            // Check if the playlist exists
            boolean exists = loggedInUser.getPlaylists().stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(playlistName));

            if (exists) {
                out.println(ServerProtocol.RESP_PLAYLIST_FOUND);
                return true;
            } else {
                out.println(ServerProtocol.RESP_PLAYLIST_NOT_FOUND);
                return false;
            }
        };
    }

    private ServerCommand addSongToPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String[] parts = args.split(" ", 2);
            if (parts.length < 2) {
                out.println("ERROR: Invalid arguments. Expected: ADD_SONG_TO_PLAYLIST playlistName songTitle");
                return false;
            }

            String playlistName = parts[0].trim();
            String songTitle = parts[1].trim();

            // Trouver la playlist
            Playlist found = null;
            for (Playlist p : loggedInUser.getPlaylists()) {
                if (p.getName().equalsIgnoreCase(playlistName)) {
                    found = p;
                    break;
                }
            }

            if (found == null) {
                out.println("Playlist not found.");
                return false;
            }

            // Trouver la chanson avec tous ses attributs dans la bibliothèque
            Song originalSong = null;
            for (Song s : MusicLibrary.getInstance().getAllSongs()) {
                if (s.getTitle().equals(songTitle)) {
                    originalSong = s;
                    break;
                }
            }

            if (originalSong == null) {
                out.println("Song not found.");
                return false;
            }

            System.out.println("DEBUG: Adding song '" + originalSong.getTitle() +
                    "' with path: " + originalSong.getFilePath() +
                    " to playlist '" + playlistName + "'");

            // Créer une copie complète de la chanson pour l'ajouter à la playlist
            Song songCopy = new Song(
                    originalSong.getTitle(),
                    originalSong.getArtist(),
                    originalSong.getAlbum(),
                    originalSong.getGenre(),
                    originalSong.getDuration()
            );
            // Conserver le chemin du fichier!
            songCopy.setFilePath(originalSong.getFilePath());

            // Ajouter à la playlist
            found.addSong(songCopy);

            // Mettre à jour la persistance
            List<User> allUsers = UserPersistenceManager.loadUsers();
            List<User> updatedUsers = allUsers.stream()
                    .map(u -> u.getUsername().equalsIgnoreCase(loggedInUser.getUsername()) ? loggedInUser : u)
                    .collect(Collectors.toList());
            UserPersistenceManager.saveUsers(updatedUsers);

            out.println("SUCCESS: Song added to playlist.");
            return true;
        };
    }

    private ServerCommand removeSongFromPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                return false;
            }

            String[] parts = args.split(" ", 2);
            if (parts.length < 2) {
                out.println(ServerProtocol.RESP_ERROR + ": Invalid arguments. Expected: REMOVE_SONG_FROM_PLAYLIST playlistName songTitle");
                return false;
            }

            String playlistName = parts[0].trim();
            String songTitle = parts[1].trim();

            // Find playlist
            Playlist found = loggedInUser.getPlaylists().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(playlistName))
                    .findFirst()
                    .orElse(null);

            if (found == null) {
                out.println(ServerProtocol.RESP_ERROR + ": Playlist not found");
                return false;
            }

            // Remove song
            boolean removed = found.removeSong(songTitle);

            if (removed) {
                // Update persistence
                List<User> allUsers = UserPersistenceManager.loadUsers();
                List<User> updatedUsers = allUsers.stream()
                        .map(u -> u.getUsername().equalsIgnoreCase(loggedInUser.getUsername()) ? loggedInUser : u)
                        .collect(Collectors.toList());
                UserPersistenceManager.saveUsers(updatedUsers);

                out.println(ServerProtocol.RESP_SUCCESS + ": Song removed from playlist");
                return true;
            } else {
                out.println(ServerProtocol.RESP_ERROR + ": Song not found in playlist");
                return false;
            }
        };
    }

    private ServerCommand reorderPlaylistSongCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println(ServerProtocol.RESP_ERROR + ": Not logged in");
                return false;
            }

            String[] parts = args.split(" ");
            if (parts.length != 3) {
                out.println(ServerProtocol.RESP_ERROR + ": Invalid arguments. Expected: REORDER_PLAYLIST_SONG playlistName fromIndex toIndex");
                return false;
            }

            try {
                String playlistName = parts[0];
                int fromIndex = Integer.parseInt(parts[1]);
                int toIndex = Integer.parseInt(parts[2]);

                // Trouver la playlist
                Playlist foundPlaylist = null;
                for (Playlist p : loggedInUser.getPlaylists()) {
                    if (p.getName().equalsIgnoreCase(playlistName)) {
                        foundPlaylist = p;
                        break;
                    }
                }

                if (foundPlaylist == null) {
                    out.println(ServerProtocol.RESP_ERROR + ": Playlist not found");
                    return false;
                }

                // Vérifier les indices
                if (fromIndex < 0 || toIndex < 0 || fromIndex >= foundPlaylist.size() || toIndex >= foundPlaylist.size()) {
                    out.println(ServerProtocol.RESP_ERROR + ": Invalid indices");
                    return false;
                }

                // Déplacer la chanson
                foundPlaylist.moveSong(fromIndex, toIndex);

                // Mettre à jour la persistance
                UserPersistenceManager.updateUser(loggedInUser);

                out.println(ServerProtocol.RESP_SUCCESS + ": Song reordered");
                return true;
            } catch (NumberFormatException e) {
                out.println(ServerProtocol.RESP_ERROR + ": Invalid indices format");
                return false;
            } catch (Exception e) {
                out.println(ServerProtocol.RESP_ERROR + ": " + e.getMessage());
                return false;
            }
        };
    }

    // Commandes de bibliothèque musicale

    private ServerCommand getPlaylistSongsCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                out.println("END");
                return false;
            }

            String playlistName = args.trim();
            System.out.println("DEBUG: Getting songs for playlist: " + playlistName);

            // Trouver la playlist
            Playlist foundPlaylist = null;
            for (Playlist p : loggedInUser.getPlaylists()) {
                if (p.getName().equalsIgnoreCase(playlistName)) {
                    foundPlaylist = p;
                    break;
                }
            }

            if (foundPlaylist == null) {
                out.println("ERROR: Playlist not found");
                out.println("END");
                return false;
            }

            // Récupérer les chansons
            List<Song> songs = foundPlaylist.getSongs();

            if (songs.isEmpty()) {
                out.println("SUCCESS: Playlist is empty");
                out.println("END");
                return true;
            }

            out.println("SUCCESS: Found " + songs.size() + " songs");

            // Pour chaque chanson dans la playlist
            for (Song song : songs) {
                // Chercher la version complète de la chanson dans la bibliothèque
                String songTitle = song.getTitle();
                Song librarySong = null;

                for (Song s : MusicLibrary.getInstance().getAllSongs()) {
                    if (s.getTitle().equals(songTitle)) {
                        librarySong = s;
                        break;
                    }
                }

                // Si la chanson existe dans la bibliothèque, utiliser ses détails complets
                if (librarySong != null) {
                    // Formatage SIMPLE et CLAIR pour éviter toute confusion
                    out.println(librarySong.getTitle() + "|" +
                            librarySong.getArtist() + "|" +
                            librarySong.getAlbum() + "|" +
                            librarySong.getGenre() + "|" +
                            librarySong.getDuration() + "|" +
                            (librarySong.getFilePath() != null ? librarySong.getFilePath() : ""));

                    System.out.println("DEBUG: Sending song '" + librarySong.getTitle() +
                            "' with path: " + librarySong.getFilePath());
                } else {
                    // Si la chanson n'est pas dans la bibliothèque, utiliser la version de la playlist
                    out.println(song.getTitle() + "|" +
                            song.getArtist() + "|" +
                            song.getAlbum() + "|" +
                            song.getGenre() + "|" +
                            song.getDuration() + "|" +
                            (song.getFilePath() != null ? song.getFilePath() : ""));

                    System.out.println("DEBUG: Sending song '" + song.getTitle() +
                            "' with path: " + song.getFilePath() + " (not found in library)");
                }
            }

            out.println("END");
            return true;
        };
    }

    private ServerCommand setPlaybackModeCommand(String args) {
        return out -> {
            String modeChoice = args.trim();

            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            boolean validMode = true;
            switch (modeChoice) {
                case "1": // Sequential
                    System.out.println("DEBUG: Setting playback mode to Sequential");
                    break;
                case "2": // Shuffle
                    System.out.println("DEBUG: Setting playback mode to Shuffle");
                    break;
                case "3": // Repeat
                    System.out.println("DEBUG: Setting playback mode to Repeat");
                    break;
                default:
                    validMode = false;
                    System.out.println("DEBUG: Unknown playback mode: " + modeChoice);
            }

            if (validMode) {
                out.println("SUCCESS: Playback mode set");
                return true;
            } else {
                out.println("ERROR: Invalid playback mode");
                return false;
            }
        };
    }

    /**
     * Gère la commande de lecture (play)
     */
    private ServerCommand playerPlayCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            System.out.println("DEBUG: Player play command received");
            out.println("▶️ Playing music...");
            return true;
        };
    }

    /**
     * Gère la commande de pause
     */
    private ServerCommand playerPauseCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            System.out.println("DEBUG: Player pause command received");
            out.println("⏸️ Music paused");
            return true;
        };
    }

    /**
     * Gère la commande d'arrêt
     */
    private ServerCommand playerStopCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            System.out.println("DEBUG: Player stop command received");
            out.println("⏹️ Music stopped");
            return true;
        };
    }

    /**
     * Gère la commande pour passer à la chanson suivante
     */
    private ServerCommand playerNextCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            System.out.println("DEBUG: Player next command received");
            out.println("⏭️ Next song");
            return true;
        };
    }

    /**
     * Gère la commande pour revenir à la chanson précédente
     */
    private ServerCommand playerPrevCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            System.out.println("DEBUG: Player previous command received");
            out.println("⏮️ Previous song");
            return true;
        };
    }

    /**
     * Gère la commande pour quitter le lecteur
     */
    private ServerCommand playerExitCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            System.out.println("DEBUG: Player exit command received");
            out.println("Exiting player mode");
            return true;
        };
    }

    private ServerCommand loadPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String playlistName = args.trim();
            System.out.println("DEBUG: Loading playlist: " + playlistName);

            // Trouver la playlist dans les playlists de l'utilisateur
            Playlist foundPlaylist = null;
            for (Playlist p : loggedInUser.getPlaylists()) {
                if (p.getName().equalsIgnoreCase(playlistName)) {
                    foundPlaylist = p;
                    break;
                }
            }

            if (foundPlaylist == null) {
                out.println("ERROR: Playlist not found");
                return false;
            }

            System.out.println("DEBUG: Playlist found: " + foundPlaylist.getName()
                    + " with " + foundPlaylist.size() + " songs");
            out.println("SUCCESS: Playlist loaded successfully");
            return true;
        };
    }

    /**
     * Crée une commande pour récupérer toutes les chansons
     */
    private ServerCommand getAllSongsCommand() {
        return out -> {
            List<Song> songs = MusicLibrary.getInstance().getAllSongs();

            if (songs.isEmpty()) {
                out.println("No songs in the library.");
            } else {
                for (Song song : songs) {
                    out.println(song.toString());
                }
            }
            out.println("END");
            return true;
        };
    }

    private ServerCommand searchTitleCommand(String args) {
        return out -> {
            String title = args.trim();
            List<Song> results = MusicLibrary.getInstance().searchByTitle(title);

            if (results.isEmpty()) {
                out.println("No songs found with title: " + title);
            } else {
                for (Song song : results) {
                    out.println(song.toString());
                }
            }
            out.println(ServerProtocol.RESP_END);
            return true;
        };
    }

    private ServerCommand searchArtistCommand(String args) {
        return out -> {
            String artist = args.trim();
            List<Song> results = MusicLibrary.getInstance().filterByArtist(artist);

            if (results.isEmpty()) {
                out.println("No songs found by artist: " + artist);
            } else {
                for (Song song : results) {
                    out.println(song.toString());
                }
            }
            out.println(ServerProtocol.RESP_END);
            return true;
        };
    }

    // Commandes sociales

    private ServerCommand followUserCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String usernameToFollow = args.trim();

            // Vérifier que l'utilisateur ne se suit pas lui-même
            if (loggedInUser.getUsername().equals(usernameToFollow)) {
                out.println("ERROR: You cannot follow yourself");
                return false;
            }

            // Vérifier si l'utilisateur existe
            User userToFollow = UserPersistenceManager.getUserByUsername(usernameToFollow);
            if (userToFollow == null) {
                out.println("ERROR: User not found");
                return false;
            }

            // Vérifier si l'utilisateur est déjà suivi
            if (loggedInUser.isFollowing(usernameToFollow)) {
                out.println("INFO: You are already following this user");
                return true;
            }

            // Suivre l'utilisateur
            loggedInUser.follow(userToFollow);

            // Mettre à jour la persistance
            UserPersistenceManager.updateUser(loggedInUser);

            out.println("SUCCESS: You are now following " + usernameToFollow);
            return true;
        };
    }

    private ServerCommand unfollowUserCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String usernameToUnfollow = args.trim();

            // Vérifier si l'utilisateur existe
            User userToUnfollow = UserPersistenceManager.getUserByUsername(usernameToUnfollow);
            if (userToUnfollow == null) {
                out.println("ERROR: User not found");
                return false;
            }

            // Vérifier si l'utilisateur est suivi
            if (!loggedInUser.isFollowing(usernameToUnfollow)) {
                out.println("INFO: You are not following this user");
                return true;
            }

            // Ne plus suivre l'utilisateur
            loggedInUser.unfollow(userToUnfollow);

            // Mettre à jour la persistance
            UserPersistenceManager.updateUser(loggedInUser);

            out.println("SUCCESS: You are no longer following " + usernameToUnfollow);
            return true;
        };
    }

    private ServerCommand getFollowedUsersCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                out.println("END");
                return false;
            }

            List<User> followedUsers = loggedInUser.getFollowedUsers();

            if (followedUsers.isEmpty()) {
                out.println("END");
                return true;
            }

            // Envoyer la liste des utilisateurs suivis
            for (User user : followedUsers) {
                out.println(user.getUsername());
            }

            out.println("END");
            return true;
        };
    }

    private ServerCommand getSharedPlaylistsCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                out.println("END");
                return false;
            }

            List<User> followedUsers = loggedInUser.getFollowedUsers();
            boolean foundPlaylists = false;

            // Pour chaque utilisateur suivi
            for (User followedUser : followedUsers) {
                // Vérifier s'il partage ses playlists
                if (followedUser.arePlaylistsSharedPublicly()) {
                    // Envoyer les playlists de cet utilisateur
                    for (Playlist playlist : followedUser.getPlaylists()) {
                        out.println(playlist.getName() + "|" + followedUser.getUsername());
                        foundPlaylists = true;
                    }
                }
            }

            if (!foundPlaylists) {
                // Pas de playlists partagées trouvées
            }

            out.println("END");
            return true;
        };
    }

    private ServerCommand getSharedPlaylistSongsCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                out.println("END");
                return false;
            }

            String[] parts = args.split(" ", 2);
            if (parts.length != 2) {
                out.println("ERROR: Invalid arguments");
                out.println("END");
                return false;
            }

            String ownerUsername = parts[0];
            String playlistName = parts[1];

            // Vérifier si l'utilisateur existe
            User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
            if (owner == null) {
                out.println("ERROR: User not found");
                out.println("END");
                return false;
            }

            // Vérifier si l'utilisateur est suivi
            if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                out.println("ERROR: You cannot access this playlist");
                out.println("END");
                return false;
            }

            // Trouver la playlist
            Playlist playlist = owner.getPlaylistByName(playlistName);
            if (playlist == null) {
                out.println("ERROR: Playlist not found");
                out.println("END");
                return false;
            }

            out.println("SUCCESS: Found playlist");

            // Envoyer les chansons
            for (Song song : playlist.getSongs()) {
                out.println(song.getTitle() + "|" +
                        song.getArtist() + "|" +
                        song.getAlbum() + "|" +
                        song.getGenre() + "|" +
                        song.getDuration() + "|" +
                        (song.getFilePath() != null ? song.getFilePath() : ""));
            }

            out.println("END");
            return true;
        };
    }

    private ServerCommand copySharedPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String[] parts = args.split(" ", 3);
            if (parts.length != 3) {
                out.println("ERROR: Invalid arguments");
                return false;
            }

            String ownerUsername = parts[0];
            String sourcePlaylistName = parts[1];
            String newPlaylistName = parts[2];

            // Vérifier si l'utilisateur existe
            User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
            if (owner == null) {
                out.println("ERROR: User not found");
                return false;
            }

            // Vérifier si l'utilisateur est suivi
            if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                out.println("ERROR: You cannot access this playlist");
                return false;
            }

            // Trouver la playlist source
            Playlist sourcePlaylist = owner.getPlaylistByName(sourcePlaylistName);
            if (sourcePlaylist == null) {
                out.println("ERROR: Playlist not found");
                return false;
            }

            // Vérifier si une playlist avec le nouveau nom existe déjà
            if (loggedInUser.getPlaylistByName(newPlaylistName) != null) {
                out.println("ERROR: A playlist with this name already exists");
                return false;
            }

            // Vérifier si l'utilisateur peut ajouter plus de playlists
            if (!loggedInUser.canAddPlaylist()) {
                out.println("ERROR: You have reached your playlist limit");
                return false;
            }

            // Créer une nouvelle playlist avec les mêmes chansons
            Playlist newPlaylist = new Playlist(newPlaylistName);
            for (Song song : sourcePlaylist.getSongs()) {
                newPlaylist.addSong(song);
            }

            // Ajouter la playlist à l'utilisateur
            loggedInUser.addPlaylist(newPlaylist);

            // Mettre à jour la persistance
            UserPersistenceManager.updateUser(loggedInUser);

            out.println("SUCCESS: Playlist copied successfully");
            return true;
        };
    }

    private ServerCommand loadSharedPlaylistCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            String[] parts = args.split(" ", 2);
            if (parts.length != 2) {
                out.println("ERROR: Invalid arguments");
                return false;
            }

            String ownerUsername = parts[0];
            String playlistName = parts[1];

            // Vérifier si l'utilisateur existe
            User owner = UserPersistenceManager.getUserByUsername(ownerUsername);
            if (owner == null) {
                out.println("ERROR: User not found");
                return false;
            }

            // Vérifier si l'utilisateur est suivi
            if (!loggedInUser.isFollowing(owner) && !owner.arePlaylistsSharedPublicly()) {
                out.println("ERROR: You cannot access this playlist");
                return false;
            }

            // Trouver la playlist
            Playlist playlist = owner.getPlaylistByName(playlistName);
            if (playlist == null) {
                out.println("ERROR: Playlist not found");
                return false;
            }

            out.println("SUCCESS: Playlist loaded");
            return true;
        };
    }

    private ServerCommand setPlaylistSharingCommand(String args) {
        return out -> {
            if (loggedInUser == null) {
                out.println("ERROR: Not logged in");
                return false;
            }

            boolean sharePublicly = Boolean.parseBoolean(args);

            loggedInUser.setSharePlaylistsPublicly(sharePublicly);

            // Mettre à jour la persistance
            UserPersistenceManager.updateUser(loggedInUser);

            out.println("SUCCESS: Playlist sharing preferences updated");
            return true;
        };
    }
    /**
     * Démarre le timer qui surveille la session utilisateur
     */
    public void startSessionTimer() {
        // Initialiser le temps d'activité
        lastActivityTime = System.currentTimeMillis();

        // Créer et configurer le timer
        sessionTimer = new java.util.Timer();
        sessionTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    // Vérifier si la connexion est toujours active
                    if (socket != null && (socket.isClosed() || !socket.isConnected())) {
                        System.out.println("DEBUG: Socket closed or disconnected - cleaning up");
                        closeResources();
                        cancel(); // Arrêter le timer
                        return;
                    }

                    // Vérifier si la session a expiré par inactivité
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastActivityTime > SESSION_TIMEOUT) {
                        System.out.println("DEBUG: Session expired due to inactivity");
                        loggedInUser = null; // Déconnecter l'utilisateur
                    }

                    // Vérifier l'état de la session utilisateur
                    checkUserSession();
                } catch (Exception e) {
                    System.err.println("Error in session timer: " + e.getMessage());
                }
            }
        }, 10000, 60000); // Vérifier toutes les 60 secondes, première vérification après 10 secondes
    }

    /**
     * Vérifie l'état de la session utilisateur
     */
    private void checkUserSession() {
        if (loggedInUser == null) {
            // Aucun utilisateur connecté, rien à faire
            return;
        }

        // Recharger l'utilisateur depuis la persistance pour s'assurer qu'il est à jour
        User updatedUser = UserPersistenceManager.getUserByUsername(loggedInUser.getUsername());
        if (updatedUser != null) {
            loggedInUser = updatedUser;
        } else {
            System.out.println("DEBUG: User no longer exists in persistence - clearing session");
            loggedInUser = null;
        }
    }
}