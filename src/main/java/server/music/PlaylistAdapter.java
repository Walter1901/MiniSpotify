package server.music;

import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * JSON adapter for playlists that properly handles different playlist types - CORRECTED VERSION
 */
public class PlaylistAdapter implements JsonSerializer<Playlist>, JsonDeserializer<Playlist> {

    @Override
    public JsonElement serialize(Playlist playlist, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();

        // Serialize common fields
        json.addProperty("name", playlist.getName());

        // CORRECTION: Vérification plus explicite du type
        System.out.println("DEBUG: Serializing playlist '" + playlist.getName() + "' of type: " + playlist.getClass().getSimpleName());

        if (playlist instanceof CollaborativePlaylist) {
            json.addProperty("type", "collaborative");
            System.out.println("DEBUG: Playlist marked as collaborative");

            // Add collaborative playlist specific fields
            CollaborativePlaylist collabPlaylist = (CollaborativePlaylist) playlist;
            String ownerUsername = collabPlaylist.getOwnerUsername();
            if (ownerUsername != null) {
                json.addProperty("owner", ownerUsername);
                System.out.println("DEBUG: Added owner: " + ownerUsername);
            }

            // Add collaborators
            JsonArray collaboratorsArray = new JsonArray();
            for (String username : collabPlaylist.getCollaboratorUsernames()) {
                collaboratorsArray.add(username);
                System.out.println("DEBUG: Added collaborator: " + username);
            }
            json.add("collaborators", collaboratorsArray);
        } else {
            json.addProperty("type", "standard");
            System.out.println("DEBUG: Playlist marked as standard");
        }

        // Serialize the song list
        JsonArray songsArray = new JsonArray();
        for (Song song : playlist.getSongs()) {
            JsonObject songObj = new JsonObject();
            songObj.addProperty("title", song.getTitle());
            songObj.addProperty("artist", song.getArtist());
            songObj.addProperty("album", song.getAlbum());
            songObj.addProperty("genre", song.getGenre());
            songObj.addProperty("duration", song.getDuration());

            // Also save file path to ensure correct playback
            if (song.getFilePath() != null && !song.getFilePath().isEmpty()) {
                songObj.addProperty("filePath", song.getFilePath());
            }

            songsArray.add(songObj);
        }
        json.add("songs", songsArray);

        System.out.println("DEBUG: Serialization complete for playlist: " + playlist.getName());
        return json;
    }

    @Override
    public Playlist deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String name = obj.get("name").getAsString();

        // Determine the type of playlist to create
        String type = obj.has("type") ? obj.get("type").getAsString() : "standard";
        Playlist playlist;

        System.out.println("DEBUG: Deserializing playlist '" + name + "' of type: " + type);

        if ("collaborative".equals(type)) {
            // Create a collaborative playlist
            String ownerUsername = obj.has("owner") ? obj.get("owner").getAsString() : null;
            System.out.println("DEBUG: Creating CollaborativePlaylist with owner: " + ownerUsername);

            // Create the collaborative playlist with owner's username
            CollaborativePlaylist collabPlaylist = new CollaborativePlaylist(name, ownerUsername);

            // Add collaborators if present
            if (obj.has("collaborators") && obj.get("collaborators").isJsonArray()) {
                JsonArray collaboratorsArray = obj.get("collaborators").getAsJsonArray();
                for (JsonElement collaboratorElement : collaboratorsArray) {
                    String collaboratorUsername = collaboratorElement.getAsString();
                    // Directly add to the collaborators list (since we're deserializing)
                    collabPlaylist.getCollaboratorUsernames().add(collaboratorUsername);
                    System.out.println("DEBUG: Added collaborator to deserialized playlist: " + collaboratorUsername);
                }
            }

            playlist = collabPlaylist;
            System.out.println("DEBUG: Created CollaborativePlaylist instance");
        } else {
            // Create a standard playlist
            playlist = new Playlist(name);
            System.out.println("DEBUG: Created standard Playlist instance");
        }

        // Deserialize songs
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

                // Restore file path if available
                if (songObj.has("filePath")) {
                    song.setFilePath(songObj.get("filePath").getAsString());
                }

                playlist.addSong(song);
            }
        }

        System.out.println("DEBUG: Deserialization complete for playlist: " + name + " (final type: " + playlist.getClass().getSimpleName() + ")");
        return playlist;
    }
}