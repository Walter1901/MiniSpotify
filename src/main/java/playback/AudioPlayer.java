package playback;

import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;

public class AudioPlayer {
    private Player player;
    private Thread playerThread;
    private String currentFile;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long pauseLocation;
    private long songTotalLength;
    private FileInputStream fileInputStream;

    public void play(String filePath) {
        try {
            // Arrêter la lecture en cours si nécessaire
            stop();

            // Vérifier que le chemin n'est pas un chemin formaté par erreur
            if (filePath != null && filePath.contains("|")) {
                System.out.println("⚠️ Format de chemin incorrect détecté: " + filePath);
                System.out.println("⚠️ Impossible de lire le fichier avec ce format");
                isPlaying = false;
                return;
            }

            // Vérifier si le fichier existe
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("⚠️ Fichier introuvable: " + filePath);
                System.out.println("⚠️ Chemin absolu: " + file.getAbsolutePath());
                isPlaying = false;
                return;
            }

            currentFile = filePath;
            fileInputStream = new FileInputStream(filePath);
            songTotalLength = fileInputStream.available();

            // Créer un nouveau joueur
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            player = new Player(bufferedInputStream);

            // Démarrer la lecture dans un thread séparé
            playerThread = new Thread(() -> {
                try {
                    System.out.println("🎵 Début de la lecture audio: " + filePath);
                    isPlaying = true;
                    isPaused = false;
                    player.play();
                    System.out.println("🎵 Fin de la lecture");
                    isPlaying = false;
                } catch (Exception e) {
                    System.out.println("⚠️ Erreur de lecture: " + e.getMessage());
                    isPlaying = false;
                }
            });

            playerThread.start();
        } catch (Exception e) {
            System.out.println("⚠️ Erreur d'initialisation du lecteur: " + e.getMessage());
            isPlaying = false;
        }
    }

    public void pause() {
        if (player != null && isPlaying && !isPaused) {
            try {
                // JLayer ne supporte pas directement pause/resume
                // On stoppe le joueur et on mémorise la position
                pauseLocation = fileInputStream.available();
                player.close();
                fileInputStream.close();
                isPaused = true;
                isPlaying = false;

                if (playerThread != null) {
                    playerThread.interrupt();
                }

                System.out.println("⏸️ Lecture en pause à " + (songTotalLength - pauseLocation) + "/" + songTotalLength);
            } catch (Exception e) {
                System.out.println("⚠️ Erreur lors de la pause: " + e.getMessage());
            }
        }
    }

    public void resume() {
        if (isPaused && currentFile != null) {
            try {
                // On crée un nouveau joueur et on se positionne
                fileInputStream = new FileInputStream(currentFile);
                long skipBytes = songTotalLength - pauseLocation;
                fileInputStream.skip(skipBytes);

                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                player = new Player(bufferedInputStream);

                // Démarrer la lecture dans un thread séparé
                playerThread = new Thread(() -> {
                    try {
                        System.out.println("▶️ Reprise de la lecture à " + skipBytes + "/" + songTotalLength);
                        isPlaying = true;
                        isPaused = false;
                        player.play();
                        System.out.println("🎵 Fin de la lecture");
                        isPlaying = false;
                    } catch (Exception e) {
                        System.out.println("⚠️ Erreur lors de la reprise: " + e.getMessage());
                        isPlaying = false;
                    }
                });

                playerThread.start();
            } catch (Exception e) {
                System.out.println("⚠️ Erreur lors de la reprise: " + e.getMessage());
            }
        }
    }

    public void stop() {
        if (player != null) {
            player.close();
            isPlaying = false;
            isPaused = false;

            if (playerThread != null) {
                playerThread.interrupt();
            }

            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                System.out.println("⚠️ Erreur lors de l'arrêt: " + e.getMessage());
            }

            System.out.println("⏹️ Lecture arrêtée");
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPaused() {
        return isPaused;
    }

}