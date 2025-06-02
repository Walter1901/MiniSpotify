package client.commands;

import playback.PlaybackService;

/**
 * Concrete Command for play action.
 * Implements Command pattern to encapsulate play request.
 */
public class PlayCommand implements Command {
    private PlaybackService playbackService;

    /**
     * Constructor with dependency injection
     * @param playbackService Service to handle playback
     */
    public PlayCommand(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    /**
     * Execute play command
     */
    @Override
    public void execute() {
        playbackService.play();
    }
}