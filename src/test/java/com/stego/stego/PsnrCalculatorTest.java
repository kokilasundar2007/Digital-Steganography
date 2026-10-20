package com.stego.stego;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PsnrCalculatorTest {

    private static BufferedImage solid(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    @Test
    public void identicalImages_haveZeroMseAndInfinitePsnr() {
        BufferedImage a = solid(4, 4, 0x0A141E);
        BufferedImage b = solid(4, 4, 0x0A141E);
        assertEquals(0.0, PsnrCalculator.meanSquaredError(a, b), 0.0);
        assertTrue(Double.isInfinite(PsnrCalculator.psnr(a, b)));
        assertTrue(PsnrCalculator.psnr(a, b) > 0);
    }

    @Test
    public void oneChannelOfOnePixelChangedByOne() {
        // 2x2 image = 12 channel values; one differs by 1 -> MSE = 1/12
        BufferedImage a = solid(2, 2, 0x0A141E);
        BufferedImage b = solid(2, 2, 0x0A141E);
        b.setRGB(0, 0, 0x0B141E); // red 10 -> 11
        assertEquals(1.0 / 12.0, PsnrCalculator.meanSquaredError(a, b), 1e-12);
        assertEquals(58.92261606915535, PsnrCalculator.psnr(a, b), 1e-9);
    }

    @Test
    public void everyChannelChangedByOne_isAbout48dB() {
        BufferedImage a = solid(5, 5, 0x0A141E);
        BufferedImage b = solid(5, 5, 0x0B151F); // R, G and B all +1
        assertEquals(1.0, PsnrCalculator.meanSquaredError(a, b), 1e-12);
        assertEquals(48.1308036086791, PsnrCalculator.psnr(a, b), 1e-9);
    }

    @Test
    public void oneChannelChangedByMaximum() {
        // 1 pixel = 3 values; red 0 -> 255 gives MSE = 65025 / 3 = 21675, PSNR = 10*log10(3)
        BufferedImage a = solid(1, 1, 0x000000);
        BufferedImage b = solid(1, 1, 0xFF0000);
        assertEquals(21675.0, PsnrCalculator.meanSquaredError(a, b), 1e-9);
        assertEquals(4.771212547196624, PsnrCalculator.psnr(a, b), 1e-9);
    }

    @Test
    public void alphaDifferencesAreIgnored() {
        BufferedImage a = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        BufferedImage b = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                a.setRGB(x, y, 0xFF336699);
                b.setRGB(x, y, 0x10336699); // same colour, different alpha
            }
        }
        assertEquals(0.0, PsnrCalculator.meanSquaredError(a, b), 0.0);
    }

    @Test
    public void psnrIsSymmetric() {
        BufferedImage a = solid(3, 3, 0x102030);
        BufferedImage b = solid(3, 3, 0x182838);
        assertEquals(PsnrCalculator.psnr(a, b), PsnrCalculator.psnr(b, a), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void differentDimensions_areRejected() {
        PsnrCalculator.psnr(solid(4, 4, 0), solid(4, 5, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullImage_isRejected() {
        PsnrCalculator.psnr(solid(2, 2, 0), null);
    }

    @Test
    public void largestImage_blackVersusWhite_doesNotOverflow() {
        // 4096 x 4096 = 16.7 million pixels; the summed squared error is about 3.3e12,
        // far beyond the int range. Black vs white: MSE = 255^2 = 65025, PSNR = 0 dB.
        BufferedImage black = new BufferedImage(4096, 4096, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage white = new BufferedImage(4096, 4096, BufferedImage.TYPE_BYTE_GRAY);
        Arrays.fill(((DataBufferByte) white.getRaster().getDataBuffer()).getData(), (byte) 255);
        assertEquals(65025.0, PsnrCalculator.meanSquaredError(black, white), 1e-6);
        assertEquals(0.0, PsnrCalculator.psnr(black, white), 1e-9);
    }

    @Test
    public void lsbEmbeddingAtFullCapacity_giveAbout51dB() throws Exception {
        BufferedImage cover = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                cover.setRGB(x, y, random.nextInt());
            }
        }
        LsbStegoEngine engine = new LsbStegoEngine();
        byte[] data = new byte[(int) engine.capacityInBytes(cover)]; // fill every channel
        random.nextBytes(data);

        BufferedImage stego = engine.embed(cover, data);
        double mse = PsnrCalculator.meanSquaredError(cover, stego);
        double psnr = PsnrCalculator.psnr(cover, stego);

        assertTrue("MSE can never exceed 1 for 1-bit changes: " + mse, mse <= 1.0);
        assertTrue("PSNR is at least 48.13 dB: " + psnr, psnr >= 48.13);
        assertTrue("random bits flip about half the channels, so about 51 dB: " + psnr,
                psnr > 50.0 && psnr < 52.0);
    }
}