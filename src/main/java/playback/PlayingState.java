package playback;

public class PlayingState implements PlayerState {

    @Override
    public void play(PlaybackService service) {
        System.out.println("La musique est déjà en lecture.");
    }

    @Override
    public void pause(PlaybackService service) {
        System.out.println("Pause en cours...");
        service.setState(new PausedState());
        // Pour JLayer, la mise en pause nécessite une gestion spécifique (non implémentée ici)
    }

    @Override
    public void stop(PlaybackService service) {
        System.out.println("Arrêt de la lecture...");
        service.setState(new StoppedState());
    }
}
