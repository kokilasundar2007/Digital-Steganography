package com.stego.crypto;

import com.stego.common.CryptoService;
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

    private final KeyDerivation keyDerivation;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCryptoService(KeyDerivation keyDerivation) {
        this.keyDerivation = keyDerivation;
    }

    @Override
    public Payload encrypt(String plainText, String password)
            throws GeneralSecurityException {

        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey key = keyDerivation.deriveKey(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return new Payload(VERSION, salt, iv, cipherText);
    }

    @Override
    public String decrypt(Payload payload, String password)
            throws GeneralSecurityException {

        SecretKey key = keyDerivation.deriveKey(password.toCharArray(), payload.getSalt());

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(TAG_BITS, payload.getIv()));

        byte[] plainBytes = cipher.doFinal(payload.getCipherText());
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        AesGcmCryptoService svc = new AesGcmCryptoService(new KeyDerivation());

        Payload p = svc.encrypt("Hi", "s3cret");
        System.out.println("Version       : " + p.getVersion());
        System.out.println("Salt length   : " + p.getSalt().length);
        System.out.println("IV length     : " + p.getIv().length);
        System.out.println("Cipher length : " + p.getCipherText().length);
        System.out.println("Decrypted     : " + svc.decrypt(p, "s3cret"));

        try {
            svc.decrypt(p, "wrong");
        } catch (GeneralSecurityException e) {
            System.out.println("Wrong password -> " + e.getClass().getSimpleName());
        }
    }
}
