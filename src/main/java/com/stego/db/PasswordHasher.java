package com.stego.db;
 
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
 
public final class PasswordHasher {
 
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
 
    /** Public so that ServiceFactory can create one; AuthServiceImpl uses the instance methods below. */
    public PasswordHasher() {
    }
 
    /** Instance form of {@link #hashPassword(String)}. */
    public String hash(String password) {
        return hashPassword(password);
    }
 
    /** Instance form of {@link #verifyPassword(String, String)}. */
    public boolean verify(String password, String storedHash) {
        return verifyPassword(password, storedHash);
    }
 
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                "Password cannot be empty"
            );
        }
 
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
 
        byte[] hash = derive(password, salt);
 
        return ITERATIONS + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }
 
    public static boolean verifyPassword(
            String password, String storedHash) {
 
        if (password == null || storedHash == null) {
            return false;
        }
 
        try {
            String[] parts = storedHash.split(":");
 
            if (parts.length != 3) {
                return false;
            }
 
            int iterations = Integer.parseInt(parts[0]);
 
            if (iterations < 100_000 || iterations > 1_000_000) {
                return false;
            }
 
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
 
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                expected.length * 8
            );
 
            try {
                byte[] actual = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
 
                return java.security.MessageDigest
                    .isEqual(expected, actual);
            } finally {
                spec.clearPassword();
            }
 
        } catch (IllegalArgumentException
                | java.security.GeneralSecurityException e) {
            return false;
        }
    }
 
    private static byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        );
 
        try {
            return SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(
                "Password hashing failed", e
            );
        } finally {
            spec.clearPassword();
        }
    }
}
 