package server.music;

import com.google.gson.*;
import java.lang.reflect.Type;

public class PlaylistAdapter implements JsonSerializer<Playlist>, JsonDeserializer<Playlist> {

    @Override
    public JsonElement serialize(Playlist playlist, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        // Sérialiser le nom
        json.addProperty("name", playlist.getName());

        // Sérialiser la liste des chansons
        JsonArray songsArray = new JsonArray();
        for (Song song : playlist.getSongs()) {
            JsonObject songObj = new JsonObject();
            songObj.addProperty("title", song.getTitle());
            songObj.addProperty("artist", song.getArtist());
            songObj.addProperty("album", song.getAlbum());
            songObj.addProperty("genre", song.getGenre());
            songObj.addProperty("duration", song.getDuration());
            songsArray.add(songObj);
        }
        json.add("songs", songsArray);

        return json;
    }

    @Override
    public Playlist deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String name = obj.get("name").getAsString();
        Playlist playlist = new Playlist(name);

        // Désérialiser les chansons
        if (obj.has("songs") && obj.get("songs").isJsonArray()) {
            JsonArray songsArray = obj.getAsJsonArray("songs");
            for (JsonElement songElement : songsArray) {
                JsonObject songObj = songElement.getAsJsonObject();
                String title = songObj.get("title").getAsString();
                String artist = songObj.has("artist") ? songObj.get("artist").getAsString() : "Unknown";
                String album = songObj.has("album") ? songObj.get("album").getAsString() : "Unknown";
                String genre = songObj.has("genre") ? songObj.get("genre").getAsString() : "Unknown";
                int duration = songObj.has("duration") ? songObj.get("duration").getAsInt() : 0;

                Song song = new Song(title, artist, album, genre, duration);
                playlist.addSong(song);
            }
        }

        return playlist;
    }
}