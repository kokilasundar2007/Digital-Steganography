package com.stego.stego;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import com.stego.common.PayloadCodec;
import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;

/**
 * Converts a {@link Payload} (version, salt, iv, cipherText) to bytes and back.
 *
 * <p>These bytes are what {@code LsbStegoEngine} hides in the image. The codec never
 * encrypts or decrypts anything; it only lays the fields out in a fixed order.
 *
 * <h3>Byte layout (all multi-byte numbers are big-endian)</h3>
 * <pre>
 * offset        size  field
 * 0             1     version             unsigned byte, 0..255
 * 1             1     saltLength = S      unsigned byte, 0..255
 * 2             S     salt
 * 2+S           1     ivLength = I        unsigned byte, 0..255
 * 3+S           I     iv
 * 3+S+I         4     cipherTextLength = C   signed int, must be 0 or more
 * 7+S+I         C     cipherText
 * </pre>
 * The total length is exactly 7 + S + I + C bytes. Anything shorter or longer is rejected.
 *
 * <p>Because every variable-length field has an explicit length, this codec does not
 * depend on the salt or IV sizes chosen by the crypto module.
 *
 * <p>Public no-argument constructor: {@code ServiceFactory} creates this class by reflection.
 */
public class PayloadCodecImpl implements PayloadCodec {

    /** Largest value that fits in the one-byte length fields (salt and iv). */
    public static final int MAX_SMALL_FIELD_LENGTH = 255;

    /** version(1) + saltLength(1) + ivLength(1) + cipherTextLength(4). */
    private static final int FIXED_OVERHEAD = 1 + 1 + 1 + 4;

    /** One generic message: it must not reveal which check failed. */
    private static final String INVALID = "Payload data is invalid or has been modified.";

    @Override
    public byte[] encode(Payload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload must not be null.");
        }
        int version = payload.getVersion();
        byte[] salt = payload.getSalt();
        byte[] iv = payload.getIv();
        byte[] cipherText = payload.getCipherText();

        if (version < 0 || version > 255) {
            throw new IllegalArgumentException("Payload version must be between 0 and 255.");
        }
        if (salt.length > MAX_SMALL_FIELD_LENGTH || iv.length > MAX_SMALL_FIELD_LENGTH) {
            throw new IllegalArgumentException("Salt and IV must each be at most 255 bytes.");
        }
        // long arithmetic: an int sum could overflow for a huge cipherText
        long total = (long) FIXED_OVERHEAD + salt.length + iv.length + cipherText.length;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Payload is too large to encode.");
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) total); // big-endian by default
        buffer.put((byte) version);
        buffer.put((byte) salt.length);
        buffer.put(salt);
        buffer.put((byte) iv.length);
        buffer.put(iv);
        buffer.putInt(cipherText.length);
        buffer.put(cipherText);
        return buffer.array();
    }

    @Override
    public Payload decode(byte[] data) throws TamperedDataException {
        if (data == null) {
            throw new TamperedDataException(INVALID);
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);

            int version = buffer.get() & 0xFF;          // & 0xFF turns a signed byte into 0..255
            int saltLength = buffer.get() & 0xFF;
            byte[] salt = readBytes(buffer, saltLength);
            int ivLength = buffer.get() & 0xFF;
            byte[] iv = readBytes(buffer, ivLength);

            int cipherTextLength = buffer.getInt();
            // The declared length must match what is really left. Checking BEFORE
            // allocating means a forged length can never cause a huge allocation.
            if (cipherTextLength < 0 || cipherTextLength != buffer.remaining()) {
                throw new TamperedDataException(INVALID);
            }
            byte[] cipherText = readBytes(buffer, cipherTextLength);

            return new Payload(version, salt, iv, cipherText);
        } catch (BufferUnderflowException e) {
            // ran out of bytes while reading a fixed-size field
            throw new TamperedDataException(INVALID);
        }
    }

    /** Reads exactly {@code length} bytes, or fails before allocating anything. */
    private static byte[] readBytes(ByteBuffer buffer, int length) throws TamperedDataException {
        if (length < 0 || length > buffer.remaining()) {
            throw new TamperedDataException(INVALID);
        }
        byte[] out = new byte[length];
        buffer.get(out);
        return out;
    }
}