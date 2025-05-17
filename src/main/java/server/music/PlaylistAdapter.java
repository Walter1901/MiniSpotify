package server.music;

import com.google.gson.*;
import java.lang.reflect.Type;

public class PlaylistAdapter implements JsonSerializer<Playlist>, JsonDeserializer<Playlist> {

    @Override
    public JsonElement serialize(Playlist playlist, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        // Sérialiser le nom
        json.addProperty("name", playlist.getName());
        // Sérialiser la liste des chansons obtenue via getSongs()
        JsonElement songs = context.serialize(playlist.getSongs());
        json.add("songs", songs);
        return json;
    }

    @Override
    public Playlist deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String name = obj.get("name").getAsString();
        Playlist playlist = new Playlist(name);
        // Désérialiser le tableau des chansons
        JsonArray songsArray = obj.getAsJsonArray("songs");
        for (JsonElement elem : songsArray) {
            Song song = context.deserialize(elem, Song.class);
            playlist.addSong(song);
        }
        return playlist;
    }
}
