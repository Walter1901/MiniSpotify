package utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Secure password hasher using PBKDF2 with salt
 * Replaces the old SHA-256 implementation
 */
public class SecurePasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Hash password with PBKDF2 + salt
     */
    public static String hashPassword(String password) {
        try {
            // Generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            RANDOM.nextBytes(salt);

            // Hash password
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            // Return salt:hash
            return Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verify password (compatible with old SHA-256 hashes)
     */
    public static boolean checkPassword(String password, String storedHash) {
        try {
            if (password == null || storedHash == null) {
                return false;
            }

            // Handle legacy SHA-256 hashes (backward compatibility)
            if (!storedHash.contains(":")) {
                return checkLegacyPassword(password, storedHash);
            }

            // New PBKDF2 format
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2) return false;

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            return slowEquals(expectedHash, actualHash);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if hash needs migration from old format
     */
    public static boolean needsMigration(String storedHash) {
        return storedHash != null && !storedHash.contains(":");
    }

    // Private helper methods
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    @Deprecated
    private static boolean checkLegacyPassword(String password, String sha256Hash) {
        return PasswordHasher.checkPassword(password, sha256Hash);
    }
}