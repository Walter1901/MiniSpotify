package server.music;

import java.io.File;

public class MusicLoader {
    public static void loadAllSongs() {
        File dir = new File("resources/mp3");
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files != null) {
            for (File file : files) {
                String title = file.getName().replace(".mp3", "");
                String artist = "Unknown";
                String genre = "Unknown";
                String album = "Unknown";
                int duration = 0;
                Song song = new Song(title, artist, genre, album, duration);
                MusicLibrary.getInstance().addSong(song);
            }
        }
    }
}