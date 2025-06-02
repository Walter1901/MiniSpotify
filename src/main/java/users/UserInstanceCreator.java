package users;

import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * Custom deserializer for User objects.
 * Creates the appropriate subclass (FreeUser or PremiumUser) based on account type.
 * <p>
 * This class implements the JsonDeserializer interface from Gson to properly
 * handle polymorphic deserialization of User objects.
 * </p>
 */
public class UserInstanceCreator implements JsonDeserializer<User> {

    /**
     * Deserializes a JSON element into a User object.
     *
     * @param json The JSON element being deserialized
     * @param typeOfT The type of the Object to deserialize to
     * @param context Context for deserialization
     * @return A User object of the appropriate subclass
     * @throws JsonParseException if the JSON is invalid or missing required fields
     */
    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // Extract required fields
        String username = jsonObject.has("username") ? jsonObject.get("username").getAsString() : null;
        String passwordHash = jsonObject.has("passwordHash") ? jsonObject.get("passwordHash").getAsString() : null;
        String accountType = jsonObject.has("accountType") ? jsonObject.get("accountType").getAsString() : null;

        // Validate required fields
        if (username == null || passwordHash == null || accountType == null) {
            throw new JsonParseException("Missing required fields: username, passwordHash, or accountType");
        }

        // Create the appropriate user type based on accountType
        User user;
        if ("premium".equalsIgnoreCase(accountType)) {
            user = new PremiumUser(username, passwordHash);
        } else {
            // Default to FreeUser
            user = new FreeUser(username, passwordHash);
        }

        return user;
    }
}