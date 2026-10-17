package com.stego.common;

import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;
import java.security.GeneralSecurityException;

public interface CryptoService {
    Payload encrypt(String plainText, String password) throws GeneralSecurityException;
    String decrypt(Payload payload, String password) throws TamperedDataException;
}
