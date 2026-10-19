package com.stego.crypto;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/** Turns a human password into a 256-bit AES key using PBKDF2-HMAC-SHA256. (M3) */
public final class KeyDerivation {

    public static final int SALT_LENGTH = 16;      // bytes
    public static final int ITERATIONS = 120_000;  // slows down brute force
    public static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private KeyDerivation() { }

    /** Fresh random salt, new for every message. */
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /** Same password + same salt always gives the same key. */
    public static SecretKey deriveKey(String password, byte[] salt) throws GeneralSecurityException {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            spec.clearPassword();          // wipe password copies from memory
            Arrays.fill(chars, '\0');
        }
    }
}
