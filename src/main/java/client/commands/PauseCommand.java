package client.commands;

import playback.PlaybackService;

public class PauseCommand implements Command {
    private PlaybackService playbackService;

    public PauseCommand(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @Override
    public void execute() {
        playbackService.pause();
    }
}
