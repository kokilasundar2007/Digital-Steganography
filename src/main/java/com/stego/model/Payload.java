package com.stego.common.model;

public class Payload {
    private final int version;
    private final byte[] salt;
    private final byte[] iv;
    private final byte[] cipherText;

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
