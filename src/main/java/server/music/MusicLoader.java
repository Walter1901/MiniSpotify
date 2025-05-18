package server.music;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import persistence.UserPersistenceManager;
import server.config.ServerConfig;
import users.User;

/**
 * Chargeur de musique
 */
public class MusicLoader {
    private static volatile MusicLoader instance = null;
    private boolean songsLoaded = false;

    private MusicLoader() {}

    public static MusicLoader getInstance() {
        if (instance == null) {
            synchronized (MusicLoader.class) {
                if (instance == null) {
                    instance = new MusicLoader();
                }
            }
        }
        return instance;
    }

    public void loadAllSongs() {
        if (!songsLoaded) {
            // Essayer d'abord le chemin de développement, puis le chemin de production
            String devPath = "src/main/resources/mp3";
            String prodPath = "resources/mp3";

            File devDir = new File(devPath);
            File prodDir = new File(prodPath);

            File mp3Dir;
            if (devDir.exists() && devDir.isDirectory()) {
                mp3Dir = devDir;
                System.out.println("Using development MP3 path: " + devPath);
            } else if (prodDir.exists() && prodDir.isDirectory()) {
                mp3Dir = prodDir;
                System.out.println("Using production MP3 path: " + prodPath);
            } else {
                System.out.println("No MP3 directory found. Creating sample songs.");
                loadSampleSongs();
                songsLoaded = true;
                return;
            }

            // Lister tous les fichiers et afficher des informations détaillées pour le débogage
            System.out.println("Scanning directory: " + mp3Dir.getAbsolutePath());
            File[] allFiles = mp3Dir.listFiles();
            if (allFiles != null) {
                System.out.println("Found " + allFiles.length + " files in directory:");
                for (File file : allFiles) {
                    System.out.println(" - " + file.getName() + " (is file: " + file.isFile() + ")");
                }
            }

            // Filtrer pour les MP3
            File[] mp3Files = mp3Dir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
            if (mp3Files == null || mp3Files.length == 0) {
                System.out.println("No MP3 files found. Creating sample songs.");
                loadSampleSongs();
                songsLoaded = true;
                return;
            }

            System.out.println("Found " + mp3Files.length + " MP3 files");

            // Créer une correspondance entre les titres et les fichiers pour un matching plus facile
            Map<String, File> titleToFileMap = new HashMap<>();

            // D'abord, créer un mapping basé sur les noms de fichiers
            for (File file : mp3Files) {
                String fileName = file.getName().toLowerCase();
                String title = fileName.substring(0, fileName.length() - 4); // Enlever .mp3

                // Si le titre contient " - ", extraire le titre après le séparateur
                if (title.contains(" - ")) {
                    String[] parts = title.split(" - ", 2);
                    title = parts[1].trim().toLowerCase();
                }

                // Nettoyer le titre pour le matching
                title = title.replaceAll("[^a-z0-9]", ""); // Garder seulement les lettres et chiffres

                titleToFileMap.put(title, file);
                System.out.println("Mapped title '" + title + "' to file: " + file.getName());
            }

            // Définir les chansons de la bibliothèque avec les chemins complets
            createSongsWithPaths(titleToFileMap);

            songsLoaded = true;
        } else {
            System.out.println("Songs already loaded");
        }
    }

    private void createSongsWithPaths(Map<String, File> titleToFileMap) {
        // Liste des chansons que nous voulons créer
        String[][] songData = {
                {"Mussulo", "Dj Aka-m e Dj Malvado Feat Dody", "Afro House", "Electronic"},
                {"Ciel", "GIMS", "Rap", "Hip-Hop"},
                {"NINAO", "GIMS", "Rap", "Hip-Hop"},
                {"Mood", "Keblack", "Rap", "Hip-Hop"},
                {"Melrose Place", "Keblack Ft. Guy2Bezbar", "Rap", "Hip-Hop"}
        };

        // Créer chaque chanson avec le bon chemin de fichier
        for (String[] data : songData) {
            String title = data[0];
            String artist = data[1];
            String album = data[2];
            String genre = data[3];

            Song song = new Song(title, artist, album, genre, 0);

            // Chercher un fichier correspondant au titre (insensible à la casse)
            String cleanTitle = title.toLowerCase().replaceAll("[^a-z0-9]", "");
            File matchedFile = titleToFileMap.get(cleanTitle);

            if (matchedFile != null) {
                song.setFilePath(matchedFile.getAbsolutePath());
                System.out.println("Set path for song '" + title + "': " + matchedFile.getAbsolutePath());
            } else {
                System.out.println("No matching file found for song: " + title + " (clean: " + cleanTitle + ")");

                // Essayer une recherche plus large dans les noms de fichiers
                for (Map.Entry<String, File> entry : titleToFileMap.entrySet()) {
                    if (entry.getKey().contains(cleanTitle) || cleanTitle.contains(entry.getKey())) {
                        song.setFilePath(entry.getValue().getAbsolutePath());
                        System.out.println("Found partial match for '" + title + "': " + entry.getValue().getName());
                        break;
                    }
                }
            }

            MusicLibrary.getInstance().addSong(song);
            System.out.println("Added song: " + title + " with path: " + song.getFilePath());
        }
    }

    public void updateSongPaths() {
        System.out.println("Updating song file paths...");

        // Obtenir toutes les chansons de la bibliothèque
        List<Song> allSongs = MusicLibrary.getInstance().getAllSongs();

        // Chemin correct vers le dossier MP3
        File mp3Dir = new File("src/main/resources/mp3");
        if (!mp3Dir.exists() || !mp3Dir.isDirectory()) {
            System.out.println("MP3 directory not found: " + mp3Dir.getAbsolutePath());
            return;
        }

        // Lister tous les fichiers MP3
        File[] mp3Files = mp3Dir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        if (mp3Files == null || mp3Files.length == 0) {
            System.out.println("No MP3 files found in: " + mp3Dir.getAbsolutePath());
            return;
        }

        // Pour chaque chanson, essayer de trouver un fichier MP3 correspondant
        for (Song song : allSongs) {
            // Échapper les caractères spéciaux pour la comparaison
            String titlePattern = song.getTitle().toLowerCase().replaceAll("[^a-z0-9]", ".*");
            String artistPattern = song.getArtist().toLowerCase().replaceAll("[^a-z0-9]", ".*");

            // Chercher un fichier MP3 correspondant
            for (File mp3File : mp3Files) {
                String fileName = mp3File.getName().toLowerCase();
                // Si le nom du fichier contient à la fois le titre et l'artiste
                if (fileName.matches(".*" + titlePattern + ".*") &&
                        fileName.matches(".*" + artistPattern + ".*")) {
                    // Définir le chemin du fichier
                    String oldPath = song.getFilePath();
                    song.setFilePath(mp3File.getAbsolutePath());
                    System.out.println("Updated path for '" + song.getTitle() +
                            "' from '" + oldPath + "' to '" + mp3File.getAbsolutePath() + "'");
                    break;
                }
            }
        }

        System.out.println("Song paths updated");
    }
    public void repairExistingPlaylists() {
        System.out.println("Repairing existing playlists...");

        // Charger tous les utilisateurs
        List<User> allUsers = UserPersistenceManager.loadUsers();
        boolean changesFound = false;

        // Pour chaque utilisateur
        for (User user : allUsers) {
            // Pour chaque playlist de l'utilisateur
            for (Playlist playlist : user.getPlaylists()) {
                System.out.println("Checking playlist: " + playlist.getName() + " for user: " + user.getUsername());

                // Pour chaque chanson dans la playlist
                List<Song> songs = playlist.getSongs();
                for (int i = 0; i < songs.size(); i++) {
                    Song playlistSong = songs.get(i);

                    System.out.println("  - Song: " + playlistSong.getTitle() + " has path: " + playlistSong.getFilePath());

                    // Si la chanson n'a pas de chemin ou a un chemin invalide
                    if (playlistSong.getFilePath() == null || playlistSong.getFilePath().isEmpty()) {
                        // Chercher la chanson correspondante dans la bibliothèque
                        for (Song librarySong : MusicLibrary.getInstance().getAllSongs()) {
                            if (librarySong.getTitle().equals(playlistSong.getTitle())) {
                                // Mettre à jour le chemin
                                String oldPath = playlistSong.getFilePath();
                                playlistSong.setFilePath(librarySong.getFilePath());

                                System.out.println("    - Updated path from: " + oldPath + " to: " + librarySong.getFilePath());
                                changesFound = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Sauvegarder les changements si nécessaire
        if (changesFound) {
            UserPersistenceManager.saveUsers(allUsers);
            System.out.println("Playlist repairs saved.");
        } else {
            System.out.println("No repairs needed for existing playlists.");
        }
    }

    private void createSampleSongs() {
        System.out.println("Creating sample songs with file paths...");

        // Obtenir le chemin absolu du dossier resources/mp3
        File resourcesDir = new File("resources/mp3");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }

        // Créer des échantillons de chansons avec chemins spécifiques
        String basePath = resourcesDir.getAbsolutePath() + File.separator;

        Song song1 = new Song("Mussulo", "Dj Aka-m e Dj Malvado Feat Dody", "Album 1", "Electronic", 416);
        song1.setFilePath(basePath + "mussulo.mp3");

        Song song2 = new Song("Ciel", "GIMS", "Album 2", "Hip-Hop", 306);
        song2.setFilePath(basePath + "ciel.mp3");

        Song song3 = new Song("NINAO", "GIMS", "Album 2", "Hip-Hop", 247);
        song3.setFilePath(basePath + "ninao.mp3");

        Song song4 = new Song("Mood", "Keblack", "Album 3", "Hip-Hop", 253);
        song4.setFilePath(basePath + "mood.mp3");

        Song song5 = new Song("Melrose Place", "Keblack Ft. Guy2Bezbar", "Album 3", "Hip-Hop", 234);
        song5.setFilePath(basePath + "melrose_place.mp3");

        // Ajouter à la bibliothèque
        MusicLibrary library = MusicLibrary.getInstance();
        library.addSong(song1);
        library.addSong(song2);
        library.addSong(song3);
        library.addSong(song4);
        library.addSong(song5);

        System.out.println("Added 5 sample songs with file paths:");
        for (Song song : library.getAllSongs()) {
            System.out.println(" - " + song.getTitle() + " (Path: " + song.getFilePath() + ")");
        }
    }

    private void loadSampleSongs() {
        System.out.println("Loading sample songs...");

        // Créer des songs avec des chemins fictifs
        Song song1 = new Song("Mussulo", "Dj Aka-m e Dj Malvado Feat Dody", "Album 1", "Electronic", 416);
        Song song2 = new Song("Ciel", "GIMS", "Album 2", "Hip-Hop", 306);
        Song song3 = new Song("NINAO", "GIMS", "Album 2", "Hip-Hop", 247);
        Song song4 = new Song("Mood", "Keblack", "Album 3", "Hip-Hop", 253);
        Song song5 = new Song("Melrose Place", "Keblack Ft. Guy2Bezbar", "Album 3", "Hip-Hop", 234);

        // Indiquer que ces chansons n'ont pas de fichier réel
        song1.setFilePath("SAMPLE");
        song2.setFilePath("SAMPLE");
        song3.setFilePath("SAMPLE");
        song4.setFilePath("SAMPLE");
        song5.setFilePath("SAMPLE");

        // Ajouter à la bibliothèque
        MusicLibrary.getInstance().addSong(song1);
        MusicLibrary.getInstance().addSong(song2);
        MusicLibrary.getInstance().addSong(song3);
        MusicLibrary.getInstance().addSong(song4);
        MusicLibrary.getInstance().addSong(song5);

        songsLoaded = true;
        System.out.println("Loaded 5 sample songs");
    }
}