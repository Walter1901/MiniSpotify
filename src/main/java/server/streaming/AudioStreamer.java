package server.streaming;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestionnaire de streaming audio côté serveur
 */
public class AudioStreamer {
    private static final int BUFFER_SIZE = 4096; // Taille du buffer pour le streaming
    private final Socket clientSocket;
    private final String audioFilePath;
    private FileInputStream audioFileStream;
    private BufferedInputStream bufferedStream;
    private OutputStream outputStream;

    private AtomicBoolean isStreaming = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private Thread streamingThread;
    private long currentPosition = 0;
    private long totalBytes = 0;

    public AudioStreamer(Socket clientSocket, String audioFilePath) {
        this.clientSocket = clientSocket;
        this.audioFilePath = audioFilePath;
    }

    /**
     * Démarre le streaming
     */
    public boolean startStreaming() {
        try {
            // Vérifier que le fichier existe
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                System.err.println("Fichier audio introuvable: " + audioFilePath);
                return false;
            }

            totalBytes = audioFile.length();
            audioFileStream = new FileInputStream(audioFile);
            bufferedStream = new BufferedInputStream(audioFileStream);
            outputStream = clientSocket.getOutputStream();

            // Skip à la position courante si on reprend
            if (currentPosition > 0) {
                bufferedStream.skip(currentPosition);
            }

            isStreaming.set(true);
            isPaused.set(false);

            // Créer et démarrer le thread de streaming
            streamingThread = new Thread(this::streamAudio);
            streamingThread.setDaemon(true);
            streamingThread.start();

            System.out.println("🎵 Streaming démarré: " + audioFilePath);
            return true;

        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du streaming: " + e.getMessage());
            return false;
        }
    }

    /**
     * Met en pause le streaming
     */
    public void pauseStreaming() {
        isPaused.set(true);
        System.out.println("⏸️ Streaming mis en pause à la position: " + currentPosition);
    }

    /**
     * Reprend le streaming
     */
    public void resumeStreaming() {
        isPaused.set(false);
        synchronized (this) {
            notify(); // Réveiller le thread de streaming
        }
        System.out.println("▶️ Streaming repris à la position: " + currentPosition);
    }

    /**
     * Arrête le streaming
     */
    public void stopStreaming() {
        isStreaming.set(false);
        isPaused.set(false);
        currentPosition = 0;

        if (streamingThread != null) {
            streamingThread.interrupt();
        }

        closeResources();
        System.out.println("⏹️ Streaming arrêté");
    }

    /**
     * Boucle principale de streaming
     */
    private void streamAudio() {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        try {
            while (isStreaming.get() && !Thread.currentThread().isInterrupted()) {
                // Si en pause, attendre
                if (isPaused.get()) {
                    synchronized (this) {
                        try {
                            wait(); // Attendre la reprise
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    continue;
                }

                // Lire et envoyer les données audio
                bytesRead = bufferedStream.read(buffer);
                if (bytesRead == -1) {
                    // Fin du fichier atteinte
                    break;
                }

                // Envoyer les données au client
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();

                currentPosition += bytesRead;

                // Petit délai pour contrôler le débit (optionnel)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        } catch (IOException e) {
            if (isStreaming.get()) {
                System.err.println("Erreur durant le streaming: " + e.getMessage());
            }
        } finally {
            closeResources();
        }
    }

    /**
     * Ferme les ressources
     */
    private void closeResources() {
        try {
            if (bufferedStream != null) bufferedStream.close();
            if (audioFileStream != null) audioFileStream.close();
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture des ressources: " + e.getMessage());
        }
    }

    /**
     * Retourne la position courante en pourcentage
     */
    public double getProgress() {
        if (totalBytes == 0) return 0.0;
        return (double) currentPosition / totalBytes * 100.0;
    }

    /**
     * Vérifie si le streaming est actif
     */
    public boolean isStreaming() {
        return isStreaming.get();
    }

    /**
     * Retourne la position courante
     */
    public long getCurrentPosition() {
        return currentPosition;
    }
}