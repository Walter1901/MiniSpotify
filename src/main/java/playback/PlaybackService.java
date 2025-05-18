package playback;

import server.music.DoublyLinkedPlaylist;
import server.music.Song;
import users.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaybackService {
    private PlaybackState currentState;
    private PlaybackMode currentPlayMode; // Déclaration manquante
    private DoublyLinkedPlaylist playlist;
    private User user;
    private AudioPlayer audioPlayer;
    private List<PlaybackObserver> observers = new ArrayList<>(); // Déclaration manquante

    public PlaybackService(DoublyLinkedPlaylist playlist, User user) {
        this.playlist = playlist;
        this.user = user;
        this.currentState = new StoppedState();
        this.audioPlayer = new AudioPlayer();
        this.currentPlayMode = new SequentialPlayState(); // Initialisation du mode par défaut
    }

    // Ajouter cette méthode pour définir le mode de lecture
    public void setPlaybackMode(PlaybackMode mode) {
        this.currentPlayMode = mode;
        notifyObservers();
    }

    /**
     * Démarre ou reprend la lecture
     */
    public void play() {
        if (playlist == null || playlist.isEmpty()) {
            System.out.println("📂 Playlist vide.");
            return;
        }

        Song currentSong = playlist.getCurrentSong();
        if (currentSong != null) {
            // Vérifier explicitement le chemin
            String filePath = currentSong.getFilePath();
            System.out.println("DEBUG: Trying to play file: " + filePath);

            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    audioPlayer.play(filePath);
                    System.out.println("🎵 Lecture de: " + currentSong.getTitle() + " par " + currentSong.getArtist());
                } else {
                    System.out.println("⚠️ Fichier introuvable: " + filePath);
                    System.out.println("⚠️ Chemin absolu: " + file.getAbsolutePath());
                }
            } else {
                System.out.println("⚠️ Fichier audio manquant pour cette chanson: " + currentSong.getTitle());
            }

            currentState.play(this, playlist);
        }
    }

    public void pause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
        }
        currentState.pause(this, playlist);
    }

    public void stop() {
        audioPlayer.stop();
        currentState.stop(this, playlist);
    }

    public void next() {
        audioPlayer.stop();
        currentState.next(this, playlist);
        play(); // Lecture automatique après avoir changé de chanson
    }

    public void previous() {
        audioPlayer.stop();
        currentState.previous(this, playlist);
        play(); // Lecture automatique après avoir changé de chanson
    }

    /**
     * Change l'état courant du lecteur
     */
    public void setState(PlaybackState state) {
        this.currentState = state;
        notifyObservers();
    }

    /**
     * Définit la playlist à jouer
     */
    public void setPlaylist(DoublyLinkedPlaylist playlist) {
        this.playlist = playlist;
        notifyObservers();
    }

    /**
     * Définit l'utilisateur courant
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Récupère l'état courant
     */
    public PlaybackState getCurrentState() {
        return currentState;
    }

    /**
     * Récupère le mode de lecture courant
     */
    public PlaybackMode getCurrentPlayMode() {
        return currentPlayMode;
    }

    /**
     * Récupère la chanson actuellement en lecture
     */
    public Song getCurrentSong() {
        return playlist != null ? playlist.getCurrentSong() : null;
    }

    // Méthodes pour le pattern Observer

    /**
     * Enregistre un observateur
     */
    public void registerObserver(PlaybackObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Supprime un observateur
     */
    public void removeObserver(PlaybackObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifie tous les observateurs des changements
     */
    private void notifyObservers() {
        for (PlaybackObserver observer : observers) {
            observer.onPlayStateChanged(currentState.getName());

            Song song = getCurrentSong();
            if (song != null) {
                observer.onSongChanged(song);
            }
        }
    }
}