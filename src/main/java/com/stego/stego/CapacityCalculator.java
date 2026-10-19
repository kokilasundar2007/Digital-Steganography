package com.stego.stego;

/**
 * Calculates how many bytes of payload an image can hold when it is used with
 * {@link LsbStegoEngine}.
 *
 * <p>Agreed LSB convention (the engine must use exactly these constants):
 * <ul>
 *   <li>Only the red, green and blue channels are used. Alpha is never touched.</li>
 *   <li>Each channel carries 1 bit (its least significant bit).</li>
 *   <li>The first {@value #HEADER_BYTES} hidden bytes are a header
 *       (4-byte magic marker + 4-byte payload length), followed by the payload.</li>
 * </ul>
 *
 * <p>Two different numbers exist:
 * <ul>
 *   <li><b>raw capacity</b>: every bit the image can theoretically store, in bytes;</li>
 *   <li><b>usable capacity</b>: raw capacity minus the header, i.e. the largest payload
 *       that {@code StegoEngine.embed} can accept.</li>
 * </ul>
 *
 * <p>Example, a 10 x 10 image: 100 pixels x 3 bits = 300 bits = 37 whole bytes raw
 * (37.5 rounded down). Usable = 37 - 8 = 29 bytes.
 *
 * <p>This is a utility class: it has no state, so everything is static and
 * it cannot be instantiated.
 */
public final class CapacityCalculator {

    /** Colour channels we write into: red, green, blue. */
    public static final int CHANNELS_PER_PIXEL = 3;

    /** Secret bits stored in each channel (only the least significant bit). */
    public static final int BITS_PER_CHANNEL = 1;

    /** Hidden header size: 4 bytes magic marker + 4 bytes payload length. */
    public static final int HEADER_BYTES = 8;

    private static final int BITS_PER_PIXEL = CHANNELS_PER_PIXEL * BITS_PER_CHANNEL;
    private static final int BITS_PER_BYTE = 8;

    private CapacityCalculator() {
        // not meant to be instantiated
    }

    /**
     * Theoretical capacity in whole bytes: {@code floor(width * height * 3 / 8)}.
     *
     * @throws IllegalArgumentException if width or height is not positive
     */
    public static long rawCapacityInBytes(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image width and height must be positive.");
        }
        // (long) first, so width * height cannot overflow an int.
        long pixels = (long) width * height;

        // 8 pixels carry exactly 24 bits = 3 bytes. Splitting the pixels into
        // groups of 8 plus a remainder gives the same answer as pixels * 3 / 8,
        // but without the multiplication that could overflow for absurd sizes.
        long wholeGroups = pixels / BITS_PER_BYTE;
        long leftoverPixels = pixels % BITS_PER_BYTE;
        return wholeGroups * BITS_PER_PIXEL + (leftoverPixels * BITS_PER_PIXEL) / BITS_PER_BYTE;
    }

    /**
     * Largest payload (in bytes) that can be embedded: raw capacity minus the header,
     * never below 0.
     *
     * @throws IllegalArgumentException if width or height is not positive
     */
    public static long usableCapacityInBytes(int width, int height) {
        long raw = rawCapacityInBytes(width, height);
        return Math.max(0L, raw - HEADER_BYTES);
    }

    /**
     * Returns true if a payload of {@code payloadLength} bytes fits in an image of this size.
     * A negative length never fits.
     *
     * @throws IllegalArgumentException if width or height is not positive
     */
    public static boolean canHold(int width, int height, long payloadLength) {
        return payloadLength >= 0 && payloadLength <= usableCapacityInBytes(width, height);
    }
}