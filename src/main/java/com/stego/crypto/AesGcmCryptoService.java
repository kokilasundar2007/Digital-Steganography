package com.stego.crypto;

import com.stego.common.CryptoService;
import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class AesGcmCryptoService implements CryptoService {

    public static final int VERSION = 1;
    public static final int SALT_LEN = 16;
    public static final int IV_LEN = 12;
    public static final int TAG_BITS = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecureRandom random = new SecureRandom();

    @Override
    public Payload encrypt(String plainText, String password)
            throws GeneralSecurityException {

        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey key = KeyDerivation.deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return new Payload(VERSION, salt, iv, cipherText);
    }

    @Override
    public String decrypt(Payload payload, String password)
            throws TamperedDataException {

        try {
            SecretKey key = KeyDerivation.deriveKey(password, payload.getSalt());

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, payload.getIv()));

            byte[] plainBytes = cipher.doFinal(payload.getCipherText());
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException e) {
            throw new TamperedDataException("Wrong password or data was tampered with");
        }
    }
}
