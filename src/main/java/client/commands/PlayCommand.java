package client.commands;

import playback.PlaybackService;

public class PlayCommand implements Command {
    private PlaybackService playbackService;

    public PlayCommand(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @Override
    public void execute() {
        playbackService.play();
    }
}
