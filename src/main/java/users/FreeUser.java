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
        return playlists.size() < 1;
    }

    @Override
    public boolean canUseShuffle() {
        return false;
    }
}