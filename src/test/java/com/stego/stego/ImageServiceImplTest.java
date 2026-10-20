package com.stego.stego;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.stego.common.CryptoService;
import com.stego.common.exception.CapacityExceededException;
import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.Test;

/**
 * Unit tests for ImageServiceImpl. Codec and engine are the real M2 classes; encryption is a
 * FAKE (the "cipher text" is just the UTF-8 bytes) so these tests do not depend on M3.
 */
public class ImageServiceImplTest {

    /** Test double: NOT real encryption. */
    private static class FakeCrypto implements CryptoService {
        @Override
        public Payload encrypt(String plainText, String password) {
            return new Payload(1, new byte[16], new byte[12], plainText.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String decrypt(Payload payload, String password) throws TamperedDataException {
            return new String(payload.getCipherText(), StandardCharsets.UTF_8);
        }
    }

    private final PayloadCodecImpl codec = new PayloadCodecImpl();
    private final LsbStegoEngine engine = new LsbStegoEngine();
    private final FakeCrypto crypto = new FakeCrypto();
    private final ImageServiceImpl service = new ImageServiceImpl(crypto, codec, engine);

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

    private static byte[] encode(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue("writer for " + format, ImageIO.write(image, format, out));
        return out.toByteArray();
    }

    /** The full extract path: PNG bytes -> image -> engine -> codec -> decrypt. */
    private String extractText(byte[] stegoPng) throws Exception {
        BufferedImage image = ImageServiceImpl.readValidated(stegoPng);
        Payload payload = codec.decode(engine.extract(image));
        return crypto.decrypt(payload, "pw");
    }

    private void assertRejectedAsInvalid(byte[] upload) throws Exception {
        try {
            service.embed(upload, "secret", "password1");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // correct: user-safe message
            assertTrue(!expected.getMessage().contains("secret"));
            assertTrue(!expected.getMessage().contains("password1"));
        }
    }

    // ------------------------------------------------------------ happy paths

    @Test
    public void embed_returnsPngBytes() throws Exception {
        byte[] cover = encode(randomImage(60, 60, BufferedImage.TYPE_INT_RGB, 1), "png");
        byte[] out = service.embed(cover, "hello", "password1");
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertArrayEquals(signature, Arrays.copyOf(out, 8));
    }

    @Test
    public void embed_thenExtract_roundTripsUnicodeText() throws Exception {
        byte[] cover = encode(randomImage(80, 80, BufferedImage.TYPE_INT_RGB, 2), "png");
        String message = "Secret \u00e9\u4e16\u754c \ud83d\ude00 line1\nline2";
        assertEquals(message, extractText(service.embed(cover, message, "password1")));
    }

    @Test
    public void embed_preservesDimensions() throws Exception {
        byte[] cover = encode(randomImage(77, 41, BufferedImage.TYPE_INT_RGB, 3), "png");
        BufferedImage result = ImageServiceImpl.readValidated(service.embed(cover, "hi", "password1"));
        assertEquals(77, result.getWidth());
        assertEquals(41, result.getHeight());
    }

    @Test
    public void embed_acceptsJpegCover() throws Exception {
        byte[] cover = encode(randomImage(80, 80, BufferedImage.TYPE_INT_RGB, 4), "jpg");
        assertEquals("from jpeg", extractText(service.embed(cover, "from jpeg", "password1")));
    }

    @Test
    public void embed_acceptsBmpCover() throws Exception {
        byte[] cover = encode(randomImage(80, 80, BufferedImage.TYPE_INT_RGB, 5), "bmp");
        assertEquals("from bmp", extractText(service.embed(cover, "from bmp", "password1")));
    }

    @Test
    public void embed_acceptsGrayscaleCover() throws Exception {
        byte[] cover = encode(randomImage(80, 80, BufferedImage.TYPE_BYTE_GRAY, 6), "png");
        assertEquals("from gray", extractText(service.embed(cover, "from gray", "password1")));
    }

    @Test
    public void embed_preservesAlphaOfPngCover() throws Exception {
        BufferedImage source = randomImage(60, 60, BufferedImage.TYPE_INT_ARGB, 7);
        byte[] coverPng = encode(source, "png");
        int[] before = ImageServiceImpl.readValidated(coverPng).getRGB(0, 0, 60, 60, null, 0, 60);
        byte[] out = service.embed(coverPng, "alpha test", "password1");
        BufferedImage result = ImageServiceImpl.readValidated(out);
        int[] after = result.getRGB(0, 0, 60, 60, null, 0, 60);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i] >>> 24, after[i] >>> 24);
        }
        assertEquals("alpha test", extractText(out));
    }

    // ------------------------------------------------------- invalid uploads

    @Test(expected = IllegalArgumentException.class)
    public void embed_rejectsNullImage() throws Exception {
        service.embed(null, "secret", "password1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void embed_rejectsEmptyImage() throws Exception {
        service.embed(new byte[0], "secret", "password1");
    }

    @Test
    public void embed_rejectsNonImageBytes() throws Exception {
        assertRejectedAsInvalid("this is plain text, not an image".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void embed_rejectsCorruptPngWithValidSignature() throws Exception {
        byte[] fake = new byte[200];
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, fake, 0, 8);
        assertRejectedAsInvalid(fake);
    }

    @Test
    public void readValidated_rejectsTruncatedPng() throws Exception {
        byte[] full = encode(randomImage(60, 60, BufferedImage.TYPE_INT_RGB, 8), "png");
        assertRejectedAsInvalid(Arrays.copyOf(full, full.length / 2));
    }

    @Test
    public void embed_rejectsOversizedUpload() throws Exception {
        assertRejectedAsInvalid(new byte[(int) ImageServiceImpl.MAX_IMAGE_BYTES + 1]);
    }

    @Test
    public void embed_rejectsImageWithTooManyPixels() throws Exception {
        // 5000 x 5000 = 25 million pixels, but a blank 1-bit PNG is tiny, like a decompression bomb
        BufferedImage huge = new BufferedImage(5000, 5000, BufferedImage.TYPE_BYTE_BINARY);
        assertRejectedAsInvalid(encode(huge, "png"));
    }

    // ----------------------------------------------------- other bad requests

    @Test(expected = CapacityExceededException.class)
    public void embed_rejectsMessageThatDoesNotFit() throws Exception {
        byte[] tiny = encode(randomImage(10, 10, BufferedImage.TYPE_INT_RGB, 9), "png");
        service.embed(tiny, "this message is far too long for a ten by ten image", "password1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void embed_rejectsNullMessage() throws Exception {
        service.embed(encode(randomImage(40, 40, BufferedImage.TYPE_INT_RGB, 10), "png"), null, "password1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void embed_rejectsBlankPassword() throws Exception {
        service.embed(encode(randomImage(40, 40, BufferedImage.TYPE_INT_RGB, 11), "png"), "secret", "");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_rejectsNullDependency() {
        new ImageServiceImpl(null, codec, engine);
    }
}