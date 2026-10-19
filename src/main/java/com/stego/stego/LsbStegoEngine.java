package com.stego.stego;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.stego.common.StegoEngine;
import com.stego.common.exception.CapacityExceededException;
import com.stego.common.exception.TamperedDataException;

/**
 * Hides a byte array in the least significant bits of an image's pixels, and reads it back.
 *
 * <h3>Convention (must never change without changing the version byte in the magic marker)</h3>
 * <ul>
 *   <li>Channels used: red, green, blue. Alpha is never modified.</li>
 *   <li>1 bit per channel: the least significant bit.</li>
 *   <li>Pixels are visited row by row, left to right; inside a pixel the order is R, G, B.</li>
 *   <li>Bytes are written most significant bit first.</li>
 *   <li>The hidden stream starts at the first channel of the first pixel and is:
 *       4 bytes magic {@code "STG1"} + 4 bytes payload length (big-endian) + payload.</li>
 * </ul>
 *
 * <h3>Supported {@link BufferedImage} types</h3>
 * {@code TYPE_INT_RGB}, {@code TYPE_INT_ARGB}, {@code TYPE_3BYTE_BGR}, {@code TYPE_4BYTE_ABGR}.
 * Palette (indexed) and grayscale images are refused, because converting colours would
 * change the low bits. {@code ImageServiceImpl} converts uploads to a supported type first.
 * When saving, only a lossless format (PNG) may be used: JPEG recompression destroys the bits.
 *
 * <p>The engine does no encryption: the bytes it receives are treated as opaque.
 * It is stateless, so one instance can be shared by many requests.
 *
 * <p>Public no-argument constructor: {@code ServiceFactory} creates this class by reflection.
 */
public class LsbStegoEngine implements StegoEngine {

    /** Marker at the start of the hidden stream: "STG1" (the 1 is the format version). */
    private static final byte[] MAGIC = {0x53, 0x54, 0x47, 0x31};

    private static final int HEADER_BYTES = CapacityCalculator.HEADER_BYTES; // 4 magic + 4 length
    private static final int CHANNELS = CapacityCalculator.CHANNELS_PER_PIXEL;

    /** Guard so that width * height always fits an int[] when we read the pixels. */
    private static final long MAX_PIXELS = Integer.MAX_VALUE - 8L;

    @Override
    public long capacityInBytes(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image must not be null.");
        }
        return CapacityCalculator.usableCapacityInBytes(image.getWidth(), image.getHeight());
    }

    @Override
    public BufferedImage embed(BufferedImage cover, byte[] data) throws CapacityExceededException {
        if (cover == null || data == null) {
            throw new IllegalArgumentException("Cover image and data must not be null.");
        }
        if (!isSupported(cover)) {
            throw new IllegalArgumentException("Unsupported image type for embedding.");
        }
        int width = cover.getWidth();
        int height = cover.getHeight();
        if ((long) width * height > MAX_PIXELS) {
            throw new IllegalArgumentException("Image is too large.");
        }

        long capacity = CapacityCalculator.usableCapacityInBytes(width, height);
        if (data.length > capacity) {
            throw new CapacityExceededException("Payload of " + data.length
                    + " bytes does not fit; this image can hold " + capacity + " bytes.");
        }

        // header = magic + length (ByteBuffer is big-endian by default)
        byte[] header = ByteBuffer.allocate(HEADER_BYTES)
                .put(MAGIC)
                .putInt(data.length)
                .array();

        // A fresh int[] copy of the pixels; the cover image itself is never changed.
        int[] pixels = cover.getRGB(0, 0, width, height, null, 0, width);

        long totalBits = ((long) HEADER_BYTES + data.length) * 8;
        for (long bit = 0; bit < totalBits; bit++) {
            int byteIndex = (int) (bit / 8);
            byte current = byteIndex < HEADER_BYTES ? header[byteIndex] : data[byteIndex - HEADER_BYTES];
            int value = (current >> (7 - (int) (bit % 8))) & 1;   // most significant bit first
            setLowBit(pixels, bit, value);
        }

        int outputType = cover.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage stego = new BufferedImage(width, height, outputType);
        stego.setRGB(0, 0, width, height, pixels, 0, width);
        return stego;
    }

    @Override
    public byte[] extract(BufferedImage stego) throws TamperedDataException {
        if (stego == null || !isSupported(stego)) {
            throw new TamperedDataException("No valid hidden data found.");
        }
        int width = stego.getWidth();
        int height = stego.getHeight();
        if ((long) width * height > MAX_PIXELS) {
            throw new TamperedDataException("No valid hidden data found.");
        }

        long rawBytes = CapacityCalculator.rawCapacityInBytes(width, height);
        if (rawBytes < HEADER_BYTES) {
            throw new TamperedDataException("No valid hidden data found.");
        }

        int[] pixels = stego.getRGB(0, 0, width, height, null, 0, width);

        // 1. read and check the header
        byte[] header = readBytes(pixels, 0, HEADER_BYTES);
        if (!Arrays.equals(Arrays.copyOfRange(header, 0, MAGIC.length), MAGIC)) {
            throw new TamperedDataException("No valid hidden data found.");
        }
        int length = ByteBuffer.wrap(header, MAGIC.length, 4).getInt();

        // 2. validate the length BEFORE allocating: it must be 0 or more and must fit in the image
        if (length < 0 || length > rawBytes - HEADER_BYTES) {
            throw new TamperedDataException("No valid hidden data found.");
        }

        // 3. read exactly 'length' payload bytes
        return readBytes(pixels, (long) HEADER_BYTES * 8, length);
    }

    // ---------------------------------------------------------------- helpers

    private static boolean isSupported(BufferedImage image) {
        switch (image.getType()) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Shift that selects a channel inside a packed ARGB int:
     * channel 0 = red (bits 16-23), 1 = green (8-15), 2 = blue (0-7). Alpha (24-31) is never used.
     */
    private static int shiftFor(long bitIndex) {
        return 16 - 8 * (int) (bitIndex % CHANNELS);
    }

    /** Overwrites the lowest bit of the channel that carries bit number {@code bitIndex}. */
    private static void setLowBit(int[] pixels, long bitIndex, int bit) {
        int pixelIndex = (int) (bitIndex / CHANNELS);
        int shift = shiftFor(bitIndex);
        pixels[pixelIndex] = (pixels[pixelIndex] & ~(1 << shift)) | (bit << shift);
    }

    /** Reads the lowest bit of the channel that carries bit number {@code bitIndex}. */
    private static int getLowBit(int[] pixels, long bitIndex) {
        int pixelIndex = (int) (bitIndex / CHANNELS);
        return (pixels[pixelIndex] >> shiftFor(bitIndex)) & 1;
    }

    /** Reads {@code count} bytes starting at bit {@code firstBit}; the caller has checked the range. */
    private static byte[] readBytes(int[] pixels, long firstBit, int count) {
        byte[] out = new byte[count];
        for (int i = 0; i < count; i++) {
            int value = 0;
            for (int k = 0; k < 8; k++) {
                value = (value << 1) | getLowBit(pixels, firstBit + (long) i * 8 + k);
            }
            out[i] = (byte) value;
        }
        return out;
    }
}