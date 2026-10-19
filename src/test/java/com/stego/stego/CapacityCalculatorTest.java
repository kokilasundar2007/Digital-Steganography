package com.stego.stego;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CapacityCalculatorTest {

    @Test
    public void rawCapacity_10x10_is37Bytes() {
        // 100 pixels * 3 bits = 300 bits = 37.5 bytes -> 37
        assertEquals(37L, CapacityCalculator.rawCapacityInBytes(10, 10));
    }

    @Test
    public void usableCapacity_10x10_is29Bytes() {
        assertEquals(29L, CapacityCalculator.usableCapacityInBytes(10, 10));
    }

    @Test
    public void usableCapacity_1000x800_is299992Bytes() {
        assertEquals(300000L, CapacityCalculator.rawCapacityInBytes(1000, 800));
        assertEquals(299992L, CapacityCalculator.usableCapacityInBytes(1000, 800));
    }

    @Test
    public void tinyImages_haveZeroUsableCapacity_notNegative() {
        assertEquals(0L, CapacityCalculator.rawCapacityInBytes(1, 1));
        assertEquals(0L, CapacityCalculator.usableCapacityInBytes(1, 1));
        assertEquals(3L, CapacityCalculator.rawCapacityInBytes(3, 3));
        assertEquals(0L, CapacityCalculator.usableCapacityInBytes(3, 3));
    }

    @Test
    public void hugeDimensions_doNotOverflow() {
        int w = Integer.MAX_VALUE;
        int h = Integer.MAX_VALUE;
        long expected = BigInteger.valueOf(w)
                .multiply(BigInteger.valueOf(h))
                .multiply(BigInteger.valueOf(3))
                .divide(BigInteger.valueOf(8))
                .longValueExact();
        assertEquals(expected, CapacityCalculator.rawCapacityInBytes(w, h));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroWidth_isRejected() {
        CapacityCalculator.rawCapacityInBytes(0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeHeight_isRejected() {
        CapacityCalculator.usableCapacityInBytes(10, -5);
    }

    @Test
    public void canHold_isExactAtTheBoundary() {
        assertTrue(CapacityCalculator.canHold(10, 10, 0));
        assertTrue(CapacityCalculator.canHold(10, 10, 29));
        assertFalse(CapacityCalculator.canHold(10, 10, 30));
    }

    @Test
    public void canHold_rejectsNegativeLength() {
        assertFalse(CapacityCalculator.canHold(10, 10, -1));
    }
}