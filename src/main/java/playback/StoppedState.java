package playback;

import server.music.DoublyLinkedPlaylist;

public class StoppedState implements PlaybackState {
    @Override
    public void play(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist == null || playlist.isEmpty()) {
            System.out.println("📂 Cannot start playback: playlist is empty or not defined.");
            return;
        }

        System.out.println("▶️ Starting playback: " +
                (playlist.getCurrentSong() != null ? playlist.getCurrentSong().getTitle() : "No song"));
        service.setState(new PlayingState());
    }

    @Override
    public void pause(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⚠️ Cannot pause: music is stopped.");
    }

    @Override
    public void next(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist != null && !playlist.isEmpty()) {
            System.out.println("Moving to next song while stopped.");
            service.getCurrentPlayMode().next(service, playlist);
        } else {
            System.out.println("⚠️ Cannot move to next song: playlist is empty or not defined.");
        }
    }

    @Override
    public void previous(PlaybackService service, DoublyLinkedPlaylist playlist) {
        if (playlist != null && !playlist.isEmpty()) {
            System.out.println("Moving to previous song while stopped.");
            service.getCurrentPlayMode().previous(service, playlist);
        } else {
            System.out.println("⚠️ Cannot move to previous song: playlist is empty or not defined.");
        }
    }

    @Override
    public void stop(PlaybackService service, DoublyLinkedPlaylist playlist) {
        System.out.println("⚠️ Music is already stopped.");
    }

    @Override
    public String getName() {
        return "Stopped";
    }
}