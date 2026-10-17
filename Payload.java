package com.stego.common.model;

/**
 * The encrypted message plus everything needed to decrypt it later.
 * Produced by CryptoService, turned into bytes by PayloadCodec.
 */
public class Payload {
    private final int version;
    private final byte[] salt;        // for key derivation from the password
    private final byte[] iv;          // AES-GCM nonce
    private final byte[] cipherText;  // encrypted message + GCM tag

    public Payload(int version, byte[] salt, byte[] iv, byte[] cipherText) {
        this.version = version;
        this.salt = salt.clone();
        this.iv = iv.clone();
        this.cipherText = cipherText.clone();
    }

    public int getVersion() { return version; }
    public byte[] getSalt() { return salt.clone(); }
    public byte[] getIv() { return iv.clone(); }
    public byte[] getCipherText() { return cipherText.clone(); }
}
