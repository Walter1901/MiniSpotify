package users;

public class PremiumUser extends User {
    public PremiumUser(String username, String passwordHash) {
        super(username, passwordHash);
    }
    @Override
    public String getAccountType() {
        return "premium";
    }

    @Override
    public boolean canAddPlaylist() {
        return true;
    }

    @Override
    public boolean canUseShuffle() {
        return true;
    }

}