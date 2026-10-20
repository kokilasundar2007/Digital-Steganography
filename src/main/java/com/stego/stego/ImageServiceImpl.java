package com.stego.stego;

import com.stego.common.CryptoService;
import com.stego.common.ImageService;
import com.stego.common.PayloadCodec;
import com.stego.common.StegoEngine;
import com.stego.common.exception.CapacityExceededException;
import com.stego.common.model.Payload;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Image handling and the embed workflow.
 *
 * <p>Embed flow: validate and decode the uploaded image, encrypt the message (M3),
 * encode the encrypted {@link Payload} to bytes (PayloadCodec), hide the bytes
 * (StegoEngine), and encode the result as a PNG.
 *
 * <p>Accepted input formats: PNG, JPEG, BMP (decided by the file's content, never its name).
 * Output is always PNG, a lossless format, so the hidden bits survive.
 * Limits: {@value #MAX_IMAGE_BYTES} bytes per file and {@value #MAX_PIXELS} pixels per image.
 *
 * <p>Error messages are written for end users (M1's servlet shows them): they never contain
 * the password, the message, file paths or internal details.
 *
 * <p>Public constructor with exactly (CryptoService, PayloadCodec, StegoEngine):
 * {@code ServiceFactory} creates this class by reflection.
 */
public class ImageServiceImpl implements ImageService {

    /** Same as the servlet's upload limit (10 MB). */
    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    /** Largest decoded image: 4096 x 4096. Protects memory against "decompression bomb" files. */
    public static final long MAX_PIXELS = 16_777_216L;

    private static final Set<String> SUPPORTED_FORMATS =
            new HashSet<>(Arrays.asList("png", "jpeg", "bmp"));

    private static final String NOT_AN_IMAGE = "The uploaded file is not a valid image. Use PNG, JPEG or BMP.";

    private final CryptoService crypto;
    private final PayloadCodec codec;
    private final StegoEngine engine;

    public ImageServiceImpl(CryptoService crypto, PayloadCodec codec, StegoEngine engine) {
        this.crypto = Objects.requireNonNull(crypto, "crypto");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public byte[] embed(byte[] image, String message, String password)
            throws CapacityExceededException, GeneralSecurityException, IOException {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("The secret message must not be empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("A password is required.");
        }

        BufferedImage cover = readValidated(image);          // 1. cheap checks first
        Payload encrypted = crypto.encrypt(message, password); // 2. M3
        byte[] hidden = codec.encode(encrypted);               // 3. payload -> bytes
        BufferedImage stego = engine.embed(cover, hidden);     // 4. bytes -> pixels
        return toPng(stego);                                   // 5. lossless output
    }

    /**
     * Decodes uploaded bytes into a {@link BufferedImage} that {@link LsbStegoEngine} supports.
     * Also used by the extraction side (M3).
     *
     * @throws IllegalArgumentException with a user-safe message if the data is missing, too big,
     *         not a real PNG/JPEG/BMP, corrupt, or has too many pixels
     */
    public static BufferedImage readValidated(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("No image was provided.");
        }
        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("The image file is too large.");
        }
        // MemoryCache variant: keeps everything in memory, no temporary files.
        try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(imageBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in); // sniffs the real format
            if (!readers.hasNext()) {
                throw new IllegalArgumentException(NOT_AN_IMAGE);
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!SUPPORTED_FORMATS.contains(format)) {
                    throw new IllegalArgumentException(NOT_AN_IMAGE);
                }
                reader.setInput(in, true, true);

                // Dimensions come from the file header: check them BEFORE decoding any pixels.
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException("The image dimensions are too large.");
                }
                return normalize(reader.read(0));
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException(NOT_AN_IMAGE, e); // corrupt or truncated file
        }
    }

    /** Encodes an image as PNG bytes (lossless). */
    public static byte[] toPng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out)) {
                throw new IOException("No PNG writer is available.");
            }
            return out.toByteArray();
        }
    }

    /**
     * Gives the engine one of the image types it supports. Images that already have a supported
     * type are returned untouched (so a stego PNG keeps its exact pixel values); others
     * (grayscale, palette, 16-bit) are redrawn into an RGB or ARGB image.
     */
    private static BufferedImage normalize(BufferedImage image) {
        switch (image.getType()) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
                return image;
            default:
                int type = image.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
                BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), type);
                Graphics2D g = copy.createGraphics();
                try {
                    g.setComposite(AlphaComposite.Src); // copy pixels exactly, no blending
                    g.drawImage(image, 0, 0, null);
                } finally {
                    g.dispose(); // release graphics resources
                }
                return copy;
        }
    }
}