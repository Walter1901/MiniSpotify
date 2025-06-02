package server.music;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DoublyLinkedPlaylist {
    private PlaylistNode head;
    private PlaylistNode tail;
    private PlaylistNode current;

    public void addSong(Song song) {
        PlaylistNode node = new PlaylistNode(song);
        if (head == null) {
            head = tail = current = node;
        } else {
            tail.setNext(node);
            node.setPrevious(tail);
            tail = node;
        }
    }

    public void next() {
        if (current != null && current.getNext() != null) {
            current = current.getNext();
        }
    }

    public void previous() {
        if (current != null && current.getPrevious() != null) {
            current = current.getPrevious();
        }
    }

    public void shuffle() {
        List<PlaylistNode> nodes = new ArrayList<>();
        PlaylistNode temp = head;
        while (temp != null) {
            nodes.add(temp);
            temp = temp.getNext();
        }
        if (!nodes.isEmpty()) {
            current = nodes.get(new Random().nextInt(nodes.size()));
        }
    }

    public Song getCurrentSong() {
        return current != null ? current.getSong() : null;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public void reset() {
        current = head;
    }
}
