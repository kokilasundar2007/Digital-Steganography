package com.stego.stego;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.stego.common.exception.CapacityExceededException;
import com.stego.common.exception.TamperedDataException;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.Test;

public class LsbStegoEngineTest {

    private final LsbStegoEngine engine = new LsbStegoEngine();

    /** A repeatable "photo": every pixel gets a pseudo-random value (same seed = same image). */
    private static BufferedImage randomImage(int width, int height, int type, long seed) {
        BufferedImage image = new BufferedImage(width, height, type);
        Random random = new Random(seed);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, random.nextInt());
            }
        }
        return image;
    }

    private static byte[] sequence(int length) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) (i * 7 + 3);
        }
        return out;
    }

    private static int[] pixelsOf(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    /** Test-only helper: forces one hidden bit using the documented convention (R,G,B order). */
    private static void forceBit(BufferedImage image, long bitIndex, int bit) {
        int pixelIndex = (int) (bitIndex / 3);
        int x = pixelIndex % image.getWidth();
        int y = pixelIndex / image.getWidth();
        int shift = 16 - 8 * (int) (bitIndex % 3);
        int pixel = image.getRGB(x, y);
        image.setRGB(x, y, (pixel & ~(1 << shift)) | (bit << shift));
    }

    // ------------------------------------------------------------ round trips

    @Test
    public void roundTrip_smallText() throws Exception {
        BufferedImage cover = randomImage(20, 20, BufferedImage.TYPE_INT_RGB, 1);
        byte[] data = "Hello, stego! \u00e9\u4e16\u754c".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(data, engine.extract(engine.embed(cover, data)));
    }

    @Test
    public void roundTrip_allByteValues() throws Exception {
        BufferedImage cover = randomImage(50, 50, BufferedImage.TYPE_INT_RGB, 2);
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }
        assertArrayEquals(data, engine.extract(engine.embed(cover, data)));
    }

    @Test
    public void roundTrip_emptyPayload() throws Exception {
        BufferedImage cover = randomImage(10, 10, BufferedImage.TYPE_INT_RGB, 3);
        assertEquals(0, engine.extract(engine.embed(cover, new byte[0])).length);
    }

    @Test
    public void roundTrip_exactlyAtCapacity() throws Exception {
        BufferedImage cover = randomImage(10, 10, BufferedImage.TYPE_INT_RGB, 4);
        assertEquals(29L, engine.capacityInBytes(cover));
        byte[] data = sequence(29);
        assertArrayEquals(data, engine.extract(engine.embed(cover, data)));
    }

    @Test
    public void roundTrip_allSupportedImageTypes() throws Exception {
        int[] types = {BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB,
                BufferedImage.TYPE_3BYTE_BGR, BufferedImage.TYPE_4BYTE_ABGR};
        for (int type : types) {
            BufferedImage cover = randomImage(30, 30, type, 5);
            byte[] data = sequence(100);
            assertArrayEquals("image type " + type, data, engine.extract(engine.embed(cover, data)));
        }
    }

    // --------------------------------------------------------- capacity limits

    @Test(expected = CapacityExceededException.class)
    public void embed_rejectsPayloadOverCapacity() throws Exception {
        BufferedImage cover = randomImage(10, 10, BufferedImage.TYPE_INT_RGB, 6);
        engine.embed(cover, sequence(30)); // capacity is 29
    }

    @Test
    public void capacityInBytes_matchesCalculator() {
        BufferedImage image = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB);
        assertEquals(299992L, engine.capacityInBytes(image));
    }

    // ------------------------------------------------- image is changed correctly

    @Test
    public void embed_doesNotModifyCover() throws Exception {
        BufferedImage cover = randomImage(20, 20, BufferedImage.TYPE_INT_ARGB, 7);
        int[] before = pixelsOf(cover);
        engine.embed(cover, sequence(40));
        assertArrayEquals(before, pixelsOf(cover));
    }

    @Test
    public void embed_preservesDimensions() throws Exception {
        BufferedImage cover = randomImage(37, 23, BufferedImage.TYPE_INT_RGB, 8);
        BufferedImage stego = engine.embed(cover, sequence(50));
        assertEquals(37, stego.getWidth());
        assertEquals(23, stego.getHeight());
    }

    @Test
    public void embed_changesOnlyLowestBitOfRgbChannels() throws Exception {
        BufferedImage cover = randomImage(40, 40, BufferedImage.TYPE_INT_ARGB, 9);
        byte[] data = sequence(100);
        int[] before = pixelsOf(cover);
        int[] after = pixelsOf(engine.embed(cover, data));

        long changedChannels = 0;
        for (int i = 0; i < before.length; i++) {
            // alpha and the upper 7 bits of R, G and B must be identical
            assertEquals(before[i] & 0xFFFEFEFE, after[i] & 0xFFFEFEFE);
            changedChannels += Integer.bitCount((before[i] ^ after[i]) & 0x00010101);
        }
        assertTrue(changedChannels <= (8 + data.length) * 8L);
    }

    @Test
    public void embed_preservesAlpha() throws Exception {
        BufferedImage cover = randomImage(30, 30, BufferedImage.TYPE_INT_ARGB, 10);
        int[] before = pixelsOf(cover);
        int[] after = pixelsOf(engine.embed(cover, sequence(60)));
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i] >>> 24, after[i] >>> 24);
        }
    }

    // --------------------------------------------------------- bad / hostile input

    @Test
    public void extract_rejectsImageWithoutHiddenData() {
        BufferedImage plain = randomImage(40, 40, BufferedImage.TYPE_INT_RGB, 11);
        try {
            engine.extract(plain);
            fail("expected TamperedDataException");
        } catch (TamperedDataException expected) {
            // correct
        }
    }

    @Test
    public void extract_rejectsCorruptedMagic() throws Exception {
        BufferedImage stego = engine.embed(randomImage(20, 20, BufferedImage.TYPE_INT_RGB, 12), sequence(20));
        // first magic bit is the top bit of 'S' (0); flip it to 1
        forceBit(stego, 0, 1);
        try {
            engine.extract(stego);
            fail("expected TamperedDataException");
        } catch (TamperedDataException expected) {
            // correct
        }
    }

    @Test
    public void extract_rejectsForgedHugeLength() throws Exception {
        BufferedImage stego = engine.embed(randomImage(20, 20, BufferedImage.TYPE_INT_RGB, 13), sequence(20));
        // overwrite the 4 length bytes (bits 32..63) with 0x7FFFFFFF
        for (int i = 0; i < 32; i++) {
            forceBit(stego, 32 + i, i == 0 ? 0 : 1);
        }
        try {
            engine.extract(stego);
            fail("expected TamperedDataException");
        } catch (TamperedDataException expected) {
            // correct
        }
    }

    @Test
    public void extract_rejectsTruncatedImage() throws Exception {
        BufferedImage cover = randomImage(40, 40, BufferedImage.TYPE_INT_RGB, 14);
        byte[] data = sequence((int) (engine.capacityInBytes(cover) * 6 / 10)); // about 60% full
        BufferedImage stego = engine.embed(cover, data);
        BufferedImage topHalf = stego.getSubimage(0, 0, 40, 20); // cut off the lower half
        try {
            engine.extract(topHalf);
            fail("expected TamperedDataException");
        } catch (TamperedDataException expected) {
            // correct
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void embed_rejectsUnsupportedImageType() throws Exception {
        engine.embed(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY), sequence(5));
    }

    @Test
    public void extract_rejectsUnsupportedImageType() {
        try {
            engine.extract(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
            fail("expected TamperedDataException");
        } catch (TamperedDataException expected) {
            // correct
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void embed_rejectsNullData() throws Exception {
        engine.embed(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), null);
    }
}