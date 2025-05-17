package users;

import utils.PasswordHasher;

public class RegularUser extends User {

    public RegularUser(String username, String password) {
        super(username, PasswordHasher.hashPassword(password));
    }

    // Constructeur utilisé lors de la désérialisation (hash déjà fait)
    public RegularUser(String username, String passwordHash, boolean isHashed) {
        super(username, passwordHash);
    }

    @Override
    public boolean canAddPlaylist() {
        // Exemple : RegularUser peut avoir jusqu’à 5 playlists
        return playlists.size() < 5;
    }

    @Override
    public boolean canUseShuffle() {
        // Exemple : RegularUser peut utiliser le shuffle
        return true;
    }

    @Override
    public String toString() {
        return "RegularUser{" +
                "username='" + username + '\'' +
                ", playlists=" + playlists +
                '}';
    }
    @Override
    public String getAccountType() {
        return "Regular";
    }
}