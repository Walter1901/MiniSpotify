package server.music;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String name;
    private PlaylistNode head;
    private PlaylistNode tail;
    private int size;

    public Playlist(String name) {
        this.name = name;
        this.size = 0;
    }

    public String getName() {
        return name;
    }

    // Ajoute une chanson en fin de playlist
    public void addSong(Song song) {
        PlaylistNode newNode = new PlaylistNode(song);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.setNext(newNode);
            newNode.setPrevious(tail);
            tail = newNode;
        }
        size++;
    }

    // Supprime une chanson par titre (insensible à la casse)
    public boolean removeSong(String title) {
        PlaylistNode current = head;
        while (current != null) {
            if (current.getSong().getTitle().equalsIgnoreCase(title)) {
                if (current.getPrevious() != null) {
                    current.getPrevious().setNext(current.getNext());
                } else {
                    head = current.getNext();
                }
                if (current.getNext() != null) {
                    current.getNext().setPrevious(current.getPrevious());
                } else {
                    tail = current.getPrevious();
                }
                size--;
                return true;
            }
            current = current.getNext();
        }
        return false;
    }

    // Affiche la playlist avec l'indice de chaque chanson
    public void printPlaylist() {
        PlaylistNode current = head;
        int index = 0;
        while (current != null) {
            System.out.println(index + ": " + current.getSong());
            current = current.getNext();
            index++;
        }
    }

    public PlaylistNode getHead() {
        return head;
    }

    public int size() {
        return size;
    }

    // Retourne le nœud à l'index donné
    public PlaylistNode getNodeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index : " + index);
        }
        PlaylistNode current = head;
        for (int i = 0; i < index; i++) {
            current = current.getNext();
        }
        return current;
    }

    // Retire et retourne le nœud à l'index donné
    public PlaylistNode removeNodeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index : " + index);
        }
        PlaylistNode nodeToRemove;
        if (index == 0) {
            nodeToRemove = head;
            head = head.getNext();
            if (head != null) {
                head.setPrevious(null);
            } else {
                tail = null;
            }
        } else {
            nodeToRemove = getNodeAt(index);
            if (nodeToRemove.getPrevious() != null) {
                nodeToRemove.getPrevious().setNext(nodeToRemove.getNext());
            }
            if (nodeToRemove.getNext() != null) {
                nodeToRemove.getNext().setPrevious(nodeToRemove.getPrevious());
            } else {
                tail = nodeToRemove.getPrevious();
            }
        }
        size--;
        nodeToRemove.setNext(null);
        nodeToRemove.setPrevious(null);
        return nodeToRemove;
    }

    // Insère un nœud à l'index souhaité
    public void insertNodeAt(PlaylistNode node, int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index : " + index);
        }
        if (index == 0) {
            node.setNext(head);
            if (head != null) {
                head.setPrevious(node);
            }
            head = node;
            if (tail == null) {
                tail = node;
            }
        } else if (index == size) {
            tail.setNext(node);
            node.setPrevious(tail);
            tail = node;
        } else {
            PlaylistNode current = getNodeAt(index);
            node.setPrevious(current.getPrevious());
            node.setNext(current);
            if (current.getPrevious() != null) {
                current.getPrevious().setNext(node);
            }
            current.setPrevious(node);
        }
        size++;
    }

    // Déplace une chanson d'une position à une autre
    public void moveSong(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= size || toIndex < 0 || toIndex > size) {
            throw new IndexOutOfBoundsException("Indices invalides");
        }
        if (fromIndex == toIndex) return;
        PlaylistNode node = removeNodeAt(fromIndex);
        insertNodeAt(node, toIndex);
    }

    // Retourne un nœud aléatoire (pour le mode shuffle)
    public PlaylistNode getRandomNode() {
        if (size == 0) return null;
        int randomIndex = (int)(Math.random() * size);
        return getNodeAt(randomIndex);
    }

    // Retourne la liste de toutes les chansons de la playlist
    public List<Song> getSongs() {
        List<Song> result = new ArrayList<>();
        PlaylistNode current = head;
        while (current != null) {
            result.add(current.getSong());
            current = current.getNext();
        }
        return result;
    }
}