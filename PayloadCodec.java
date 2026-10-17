package com.stego.common;

import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;

/** Implemented by M2 (PayloadCodecImpl). Converts a Payload to bytes and back. */
public interface PayloadCodec {

    byte[] encode(Payload payload);

    /** @throws TamperedDataException bad magic header, bad version or truncated data */
    Payload decode(byte[] data) throws TamperedDataException;
}
