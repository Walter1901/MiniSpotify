package playback;

import javazoom.jl.player.Player;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lecteur audio avec support du streaming depuis le serveur
 */
public class StreamingAudioPlayer {
    private Player player;
    private Thread playerThread;
    private Socket streamSocket;
    private InputStream streamInput;
    private PipedInputStream pipedInput;
    private PipedOutputStream pipedOutput;

    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private AtomicBoolean isStreaming = new AtomicBoolean(false);

    private String currentSong;
    private long currentPosition = 0;

    /**
     * Démarre la lecture en streaming depuis le serveur
     */
    public boolean playStream(String serverAddress, int serverPort, String songPath) {
        try {
            // Arrêter toute lecture en cours
            stop();

            currentSong = songPath;

            // Créer une connexion dédiée au streaming
            streamSocket = new Socket(serverAddress, serverPort + 1); // Port streaming = port principal + 1
            streamInput = streamSocket.getInputStream();

            // Créer des pipes pour faire le lien entre le stream réseau et le player
            pipedOutput = new PipedOutputStream();
            pipedInput = new PipedInputStream(pipedOutput, 8192); // Buffer de 8KB

            // Envoyer la demande de streaming au serveur
            PrintWriter out = new PrintWriter(streamSocket.getOutputStream(), true);
            out.println("STREAM_AUDIO " + songPath);

            isStreaming.set(true);
            isPlaying.set(true);
            isPaused.set(false);

            // Thread pour recevoir le stream et l'écrire dans le pipe
            Thread streamingThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while (isStreaming.get() && (bytesRead = streamInput.read(buffer)) != -1) {
                        if (!isPaused.get()) {
                            pipedOutput.write(buffer, 0, bytesRead);
                            pipedOutput.flush();
                            currentPosition += bytesRead;
                        }
                    }
                } catch (IOException e) {
                    if (isStreaming.get()) {
                        System.err.println("Erreur de streaming: " + e.getMessage());
                    }
                } finally {
                    try {
                        pipedOutput.close();
                    } catch (IOException e) {
                        // Ignorer
                    }
                }
            });

            // Thread pour la lecture audio
            playerThread = new Thread(() -> {
                try {
                    player = new Player(new BufferedInputStream(pipedInput));
                    System.out.println("🎵 Lecture streaming démarrée: " + songPath);
                    player.play();
                    System.out.println("🎵 Lecture streaming terminée");
                } catch (Exception e) {
                    if (isPlaying.get()) {
                        System.err.println("Erreur de lecture: " + e.getMessage());
                    }
                } finally {
                    isPlaying.set(false);
                }
            });

            streamingThread.setDaemon(true);
            playerThread.setDaemon(true);

            streamingThread.start();
            playerThread.start();

            return true;

        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du streaming: " + e.getMessage());
            return false;
        }
    }

    /**
     * Met en pause la lecture (sans fermer le player)
     */
    public void pause() {
        if (isPlaying.get() && !isPaused.get()) {
            isPaused.set(true);

            // Envoyer la commande pause au serveur
            if (streamSocket != null && !streamSocket.isClosed()) {
                try {
                    PrintWriter out = new PrintWriter(streamSocket.getOutputStream(), true);
                    out.println("PAUSE_STREAM");
                } catch (IOException e) {
                    System.err.println("Erreur lors de l'envoi de la commande pause: " + e.getMessage());
                }
            }

            System.out.println("⏸️ Lecture mise en pause");
        }
    }

    /**
     * Reprend la lecture
     */
    public void resume() {
        if (isPlaying.get() && isPaused.get()) {
            isPaused.set(false);

            // Envoyer la commande resume au serveur
            if (streamSocket != null && !streamSocket.isClosed()) {
                try {
                    PrintWriter out = new PrintWriter(streamSocket.getOutputStream(), true);
                    out.println("RESUME_STREAM");
                } catch (IOException e) {
                    System.err.println("Erreur lors de l'envoi de la commande resume: " + e.getMessage());
                }
            }

            System.out.println("▶️ Lecture reprise");
        }
    }

    /**
     * Arrête complètement la lecture
     */
    public void stop() {
        isStreaming.set(false);
        isPlaying.set(false);
        isPaused.set(false);
        currentPosition = 0;

        // Arrêter le player
        if (player != null) {
            player.close();
            player = null;
        }

        // Interrompre le thread de lecture
        if (playerThread != null) {
            playerThread.interrupt();
            playerThread = null;
        }

        // Fermer les ressources de streaming
        closeStreamingResources();

        System.out.println("⏹️ Lecture arrêtée");
    }

    /**
     * Ferme les ressources de streaming
     */
    private void closeStreamingResources() {
        try {
            if (pipedOutput != null) {
                pipedOutput.close();
                pipedOutput = null;
            }
            if (pipedInput != null) {
                pipedInput.close();
                pipedInput = null;
            }
            if (streamInput != null) {
                streamInput.close();
                streamInput = null;
            }
            if (streamSocket != null && !streamSocket.isClosed()) {
                PrintWriter out = new PrintWriter(streamSocket.getOutputStream(), true);
                out.println("STOP_STREAM");
                streamSocket.close();
                streamSocket = null;
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture des ressources: " + e.getMessage());
        }
    }

    /**
     * Vérifie si la lecture est en cours
     */
    public boolean isPlaying() {
        return isPlaying.get();
    }

    /**
     * Vérifie si la lecture est en pause
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * Retourne la chanson courante
     */
    public String getCurrentSong() {
        return currentSong;
    }

    /**
     * Retourne la position courante
     */
    public long getCurrentPosition() {
        return currentPosition;
    }
}