package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for password hashing and verification.
 * Implements security best practices for password handling.
 *
 * Follows Single Responsibility Principle (SRP) - only handles
 * password hashing operations.
 */
public class PasswordHasher {

    /**
     * Hash a plain text password using SHA-256 algorithm
     * @param password Plain text password to hash
     * @return Hexadecimal representation of hashed password
     * @throws RuntimeException if hashing algorithm is not available
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hexadecimal string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing algorithm not found", e);
        }
    }

    /**
     * Verify a plain text password against a hashed password
     * @param inputPassword Plain text password to verify
     * @param hashedPassword Previously hashed password to compare against
     * @return true if passwords match, false otherwise
     */
    public static boolean checkPassword(String inputPassword, String hashedPassword) {
        if (inputPassword == null || hashedPassword == null) {
            return false;
        }

        String hashedInput = hashPassword(inputPassword);
        return hashedInput.equals(hashedPassword);
    }

    /**
     * Private constructor to prevent instantiation
     * This is a utility class with only static methods
     */
    private PasswordHasher() {
        // Utility class - no instances allowed
    }
}