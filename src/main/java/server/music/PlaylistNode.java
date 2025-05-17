package server.music;

public class PlaylistNode {
    private Song song;
    private PlaylistNode next;
    private PlaylistNode previous;

    public PlaylistNode(Song song) {
        this.song = song;
    }

    public Song getSong() {
        return song;
    }

    public PlaylistNode getNext() {
        return next;
    }

    public void setNext(PlaylistNode next) {
        this.next = next;
    }

    public PlaylistNode getPrevious() {
        return previous;
    }

    public void setPrevious(PlaylistNode previous) {
        this.previous = previous;
    }
}