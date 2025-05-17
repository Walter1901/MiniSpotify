package users;

import com.google.gson.*;
import server.music.Playlist;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserInstanceCreator implements JsonDeserializer<User> {

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        String username = jsonObject.has("username") ? jsonObject.get("username").getAsString() : null;
        String passwordHash = jsonObject.has("passwordHash") ? jsonObject.get("passwordHash").getAsString() : null;
        String accountType = jsonObject.has("accountType") ? jsonObject.get("accountType").getAsString() : null;

        if (username == null || passwordHash == null || accountType == null) {
            throw new JsonParseException("Missing required fields: username, passwordHash, or accountType");
        }

        User user = new RegularUser(username, passwordHash, true);

        // Charger les playlists si présentes
        if (jsonObject.has("playlists")) {
            user.setPlaylists(context.deserialize(jsonObject.get("playlists"), List.class));
        }

        return user;
    }

}
