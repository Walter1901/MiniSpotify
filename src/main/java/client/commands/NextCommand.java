package client.commands;

import playback.PlaybackService;

public class NextCommand implements Command {
    private PlaybackService playbackService;

    public NextCommand(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @Override
    public void execute() {
        playbackService.next();
    }
}
