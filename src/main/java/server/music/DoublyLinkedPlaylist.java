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

    public void removeCurrentSong() {
        if (current == null) return;

        PlaylistNode prev = current.getPrevious();
        PlaylistNode next = current.getNext();

        if (prev != null) {
            prev.setNext(next);
        } else {
            head = next;
        }

        if (next != null) {
            next.setPrevious(prev);
        } else {
            tail = prev;
        }

        current = next != null ? next : prev;
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
        if (nodes.isEmpty()) return;
        current = nodes.get(new Random().nextInt(nodes.size()));
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

    public void printAllSongs() {
        PlaylistNode temp = head;
        while (temp != null) {
            System.out.println(temp.getSong());
            temp = temp.getNext();
        }
    }
}
