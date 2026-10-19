package com.stego.crypto;

import com.stego.common.CryptoService;

import com.stego.common.model.Payload;
import com.stego.common.exception.TamperedDataException;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * AES-256-GCM encryption. (M3)
 * Blob layout: [version 1B][salt 16B][iv 12B][ciphertext ... + 16B GCM tag]
 */
public class AesGcmCryptoService implements CryptoService {

    private static final byte VERSION = 1;
    private static final int IV_LENGTH = 12;      // recommended size for GCM
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = TAG_BITS / 8;
    private static final int HEADER_LENGTH = 1 + KeyDerivation.SALT_LENGTH + IV_LENGTH;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecureRandom random = new SecureRandom();

    @Override
    public Payload encrypt(String  plaintext, String password) throws Exception {
        if (plaintext == null) throw new IllegalArgumentException("Message is required");
        requirePassword(password);

        byte[]plaintext=plainText.getBytes(StrandardCharsets.UTF_8);
        byte[] salt = KeyDerivation.generateSalt();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);                       // never reuse an IV with the same key

        SecretKey key = KeyDerivation.deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(new byte[]{VERSION});      // version is authenticated, not secret
        byte[] ciphertext = cipher.doFinal(plaintext);  // includes the 16-byte tag

        return ByteBuffer.allocate(HEADER_LENGTH + ciphertext.length)
                .put(VERSION).put(salt).put(iv).put(ciphertext)
                .array();
    }

    @Override
    public String decrypt(Payload payload, String password) throws TamperedDataException {
        requirePassword(password);
        if (blob == null || blob.length < HEADER_LENGTH + TAG_BYTES) {
            throw new TamperedDataException("Hidden data is corrupted or incomplete.");
        }

        byte[] blob=payload.getData();
        ByteBuffer buf = ByteBuffer.wrap(blob);
        byte version = buf.get();
        if (version != VERSION) {
            throw new TamperedDataException("Unsupported data version.");
        }
        byte[] salt = new byte[KeyDerivation.SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        buf.get(salt);
        buf.get(iv);
        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);

        try {
            SecretKey key = KeyDerivation.deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(new byte[]{version});
            return cipher.doFinal(ciphertext);      // verifies the tag first
        } catch (AEADBadTagException e) {
            throw new TamperedDataException("Wrong password or the image has been modified.");
        } catch (GeneralSecurityException e) {
            throw new TamperedDataException("Decryption failed.");
        }
    }

    private static void requirePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
    }
}
