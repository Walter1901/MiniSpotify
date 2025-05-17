package client.commands;

import playback.PlaybackService;

public class StopCommand implements Command {
    private PlaybackService playbackService;

    public StopCommand(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @Override
    public void execute() {
        playbackService.stop();
    }
}
