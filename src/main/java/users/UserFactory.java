package users;

import persistence.UserPersistenceManager;
import java.util.List;

public class UserFactory {

    public static User createUser(String username, String password, String accountType) {
        // Crée l'utilisateur
        User newUser;
        if (accountType.equalsIgnoreCase("free")) {
            newUser = new FreeUser(username, password);
        } else if (accountType.equalsIgnoreCase("premium")) {
            newUser = new PremiumUser(username, password);
        } else {
            throw new IllegalArgumentException("Invalid account type : " + accountType);
        }

        // Vérifier les doublons avant d'ajouter
        if (UserPersistenceManager.doesUserExist(username)) {;
        } else {
            // Ajouter l'utilisateur à la liste persistée
            UserPersistenceManager.addUser(newUser);
        }

        return newUser;
    }
}
