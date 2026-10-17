package com.stego.common;

import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;

public interface PayloadCodec {
    byte[] encode(Payload payload);
    Payload decode(byte[] data) throws TamperedDataException;
}
