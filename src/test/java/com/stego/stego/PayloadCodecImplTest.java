package com.stego.stego;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;

public class PayloadCodecImplTest {

    private final PayloadCodecImpl codec = new PayloadCodecImpl();

    /** Builds a byte array whose content is easy to recognise: start, start+1, ... */
    private static byte[] sequence(int length, int start) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) (start + i);
        }
        return out;
    }

    private static void assertSamePayload(Payload expected, Payload actual) {
        assertEquals(expected.getVersion(), actual.getVersion());
        assertArrayEquals(expected.getSalt(), actual.getSalt());
        assertArrayEquals(expected.getIv(), actual.getIv());
        assertArrayEquals(expected.getCipherText(), actual.getCipherText());
    }

    private void assertRejected(byte[] data) {
        try {
            codec.decode(data);
            fail("Expected TamperedDataException for " + (data == null ? "null" : data.length + " bytes"));
        } catch (TamperedDataException expected) {
            // correct
        }
    }

    @Test
    public void roundTrip_typicalPayload() throws Exception {
        Payload original = new Payload(1, sequence(16, 1), sequence(12, 100), sequence(40, 7));
        assertSamePayload(original, codec.decode(codec.encode(original)));
    }

    @Test
    public void roundTrip_allByteValues() throws Exception {
        // 256 bytes covering 0x00..0xFF, including negative Java byte values
        Payload original = new Payload(255, sequence(16, 0), sequence(12, 200), sequence(256, 0));
        assertSamePayload(original, codec.decode(codec.encode(original)));
    }

    @Test
    public void roundTrip_emptyFields() throws Exception {
        Payload original = new Payload(0, new byte[0], new byte[0], new byte[0]);
        byte[] encoded = codec.encode(original);
        assertEquals(7, encoded.length);
        assertSamePayload(original, codec.decode(encoded));
    }

    @Test
    public void roundTrip_maximumSaltAndIvLength() throws Exception {
        Payload original = new Payload(2, sequence(255, 3), sequence(255, 9), sequence(5, 1));
        assertSamePayload(original, codec.decode(codec.encode(original)));
    }

    @Test
    public void encode_producesDocumentedBytes() {
        Payload p = new Payload(1,
                new byte[] {(byte) 0xAA, (byte) 0xBB},
                new byte[] {(byte) 0xCC},
                new byte[] {1, 2, 3});
        byte[] expected = {
                0x01,                         // version
                0x02, (byte) 0xAA, (byte) 0xBB, // saltLength, salt
                0x01, (byte) 0xCC,            // ivLength, iv
                0x00, 0x00, 0x00, 0x03,       // cipherTextLength (big-endian)
                0x01, 0x02, 0x03              // cipherText
        };
        assertArrayEquals(expected, codec.encode(p));
    }

    @Test(expected = IllegalArgumentException.class)
    public void encode_rejectsSaltLongerThan255() {
        codec.encode(new Payload(1, new byte[256], new byte[12], new byte[1]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void encode_rejectsVersionAbove255() {
        codec.encode(new Payload(256, new byte[16], new byte[12], new byte[1]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void encode_rejectsNegativeVersion() {
        codec.encode(new Payload(-1, new byte[16], new byte[12], new byte[1]));
    }

    @Test
    public void decode_rejectsNullAndEmpty() {
        assertRejected(null);
        assertRejected(new byte[0]);
    }

    @Test
    public void decode_rejectsEveryTruncation() {
        byte[] full = codec.encode(new Payload(1, sequence(16, 1), sequence(12, 50), sequence(30, 9)));
        for (int length = 0; length < full.length; length++) {
            assertRejected(Arrays.copyOf(full, length));
        }
    }

    @Test
    public void decode_rejectsTrailingBytes() {
        byte[] full = codec.encode(new Payload(1, sequence(16, 1), sequence(12, 50), sequence(30, 9)));
        assertRejected(Arrays.copyOf(full, full.length + 1));
    }

    @Test
    public void decode_rejectsHugeDeclaredLengthWithoutAllocating() {
        // version=1, saltLength=0, ivLength=0, cipherTextLength=0x7FFFFFFF, then only 3 bytes of data
        byte[] forged = {1, 0, 0, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 9, 9, 9};
        assertRejected(forged);
    }

    @Test
    public void decode_rejectsNegativeLength() {
        // cipherTextLength = 0xFFFFFFFF = -1 as a signed int
        byte[] forged = {1, 0, 0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertRejected(forged);
    }

    @Test
    public void decode_rejectsFieldLengthBeyondData() {
        // saltLength says 200 but only 2 bytes follow
        assertRejected(new byte[] {1, (byte) 200, 1, 2});
    }
}