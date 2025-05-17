package client.commands;

import playback.PlaybackService;

public class PrevCommand implements Command {
    private PlaybackService playbackService;

    public PrevCommand(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @Override
    public void execute() {
        playbackService.previous();
    }
}
