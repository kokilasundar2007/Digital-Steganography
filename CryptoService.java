package com.stego.common;

import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;
import java.security.GeneralSecurityException;

/** Implemented by M3 (AesGcmCryptoService). Internal: used by M2 and M3. */
public interface CryptoService {

    /** Derives a key from the password and encrypts the text with AES-GCM. */
    Payload encrypt(String plainText, String password) throws GeneralSecurityException;

    /** @throws TamperedDataException wrong password or modified data (GCM tag check fails) */
    String decrypt(Payload payload, String password) throws TamperedDataException;
}
