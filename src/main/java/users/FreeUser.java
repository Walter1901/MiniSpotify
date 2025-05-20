package users;

public class FreeUser extends User {
    public FreeUser(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public String getAccountType() {
        return "free";
    }

    @Override
    public boolean canAddPlaylist() {
        return playlists.size() < 2; // Augmenter la limite à 2 pour permettre 1 normale et 1 collaborative
    }

    @Override
    public boolean canUseShuffle() {
        return false;
    }
}