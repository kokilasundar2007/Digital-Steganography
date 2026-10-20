package com.stego.stego;

import java.awt.image.BufferedImage;

/**
 * Measures how much an image changed, using Peak Signal-to-Noise Ratio (PSNR).
 *
 * <pre>
 *   MSE  = (1 / (width * height * 3)) * sum of (original - modified)^2   over every R, G and B value
 *   PSNR = 10 * log10( 255^2 / MSE )    in decibels (dB)
 * </pre>
 *
 * <p>Only the red, green and blue channels are compared, the same channels the LSB engine uses;
 * alpha is ignored. If the two images are identical, MSE is 0 and PSNR is
 * {@link Double#POSITIVE_INFINITY} (callers that display it must handle that value).
 *
 * <p>Higher PSNR means smaller pixel differences. Changing only the lowest bit of a channel
 * alters its value by at most 1, so MSE is at most 1 and PSNR is at least about 48.13 dB;
 * hiding random bits in every channel gives MSE of about 0.5, which is about 51.14 dB.
 *
 * <p><b>Limitation:</b> PSNR measures pixel difference, not detectability. A high PSNR does not
 * mean the hidden data is invisible to statistical steganalysis, and it says nothing about how
 * secure the hidden message is. It is a quality measure, not a security measure.
 *
 * <p>Compare the images as the engine saw them (for example both decoded with
 * {@code ImageServiceImpl.readValidated}), so that colour conversion does not add differences.
 */
public final class PsnrCalculator {

    /** Largest value of one 8-bit colour channel. */
    public static final double MAX_CHANNEL_VALUE = 255.0;

    private PsnrCalculator() {
        // utility class
    }

    /**
     * Mean squared error over the R, G and B channels.
     *
     * @throws IllegalArgumentException if an image is null or the dimensions differ
     */
    public static double meanSquaredError(BufferedImage original, BufferedImage modified) {
        if (original == null || modified == null) {
            throw new IllegalArgumentException("Both images are required.");
        }
        int width = original.getWidth();
        int height = original.getHeight();
        if (modified.getWidth() != width || modified.getHeight() != height) {
            throw new IllegalArgumentException("Images must have the same dimensions.");
        }

        // Row by row: avoids holding two full pixel arrays for a big image.
        int[] rowA = new int[width];
        int[] rowB = new int[width];
        double totalSquaredError = 0.0;

        for (int y = 0; y < height; y++) {
            original.getRGB(0, y, width, 1, rowA, 0, width);
            modified.getRGB(0, y, width, 1, rowB, 0, width);

            long rowSum = 0;
            for (int x = 0; x < width; x++) {
                int a = rowA[x];
                int b = rowB[x];
                int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
                int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
                int db = (a & 0xFF) - (b & 0xFF);
                rowSum += (long) (dr * dr) + dg * dg + db * db; // each term is at most 65025
            }
            totalSquaredError += rowSum;
        }

        // double arithmetic: width * height * 3 cannot overflow
        double sampleCount = (double) width * height * 3;
        return totalSquaredError / sampleCount;
    }

    /**
     * PSNR in decibels, or {@link Double#POSITIVE_INFINITY} when the images are identical.
     *
     * @throws IllegalArgumentException if an image is null or the dimensions differ
     */
    public static double psnr(BufferedImage original, BufferedImage modified) {
        double mse = meanSquaredError(original, modified);
        if (mse == 0.0) {
            return Double.POSITIVE_INFINITY; // no error at all
        }
        return 10.0 * Math.log10((MAX_CHANNEL_VALUE * MAX_CHANNEL_VALUE) / mse);
    }
}